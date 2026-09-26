package com.fitness.tracker.service;

import com.fitness.tracker.dto.SocialDtos.FriendView;
import com.fitness.tracker.dto.SocialDtos.FriendsOverview;
import com.fitness.tracker.dto.SocialDtos.InviteResult;
import com.fitness.tracker.dto.SocialDtos.Person;
import com.fitness.tracker.dto.SocialDtos.RequestView;
import com.fitness.tracker.dto.SocialDtos.SearchResult;
import com.fitness.tracker.dto.SocialDtos.Suggestion;
import com.fitness.tracker.entity.Friendship;
import com.fitness.tracker.entity.Profile;
import com.fitness.tracker.entity.User;
import com.fitness.tracker.exception.BadRequestException;
import com.fitness.tracker.exception.ConflictException;
import com.fitness.tracker.exception.NotFoundException;
import com.fitness.tracker.repository.FriendshipRepository;
import com.fitness.tracker.repository.ProfileRepository;
import com.fitness.tracker.repository.UserRepository;
import com.fitness.tracker.security.CurrentUserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/** Friend requests, friendships, finding people and inviting them by email. */
@Service
public class FriendService {

    private static final int SEARCH_LIMIT = 10;
    private static final int SUGGESTION_LIMIT = 6;
    // Invitations go to any address, so each person can send only a few a day.
    private static final int INVITES_PER_DAY = 10;

    private final FriendshipRepository friendshipRepository;
    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final CurrentUserService currentUserService;
    private final NotificationService notificationService;
    private final String frontendUrl;
    private final Map<Long, Deque<LocalDateTime>> recentInvites = new java.util.concurrent.ConcurrentHashMap<>();

    public FriendService(FriendshipRepository friendshipRepository, UserRepository userRepository,
                         ProfileRepository profileRepository, CurrentUserService currentUserService,
                         NotificationService notificationService,
                         @Value("${app.frontend-url}") String frontendUrl) {
        this.friendshipRepository = friendshipRepository;
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.currentUserService = currentUserService;
        this.notificationService = notificationService;
        this.frontendUrl = frontendUrl.endsWith("/") ? frontendUrl.substring(0, frontendUrl.length() - 1) : frontendUrl;
    }

    @Transactional(readOnly = true)
    public FriendsOverview overview() {
        User me = currentUserService.get();
        List<Friendship> all = friendshipRepository.findAllInvolving(me.getId());
        Map<Long, String> pictures = picturesFor(all.stream().map(f -> f.other(me.getId()).getId()).toList());

        List<FriendView> friends = new ArrayList<>();
        List<RequestView> incoming = new ArrayList<>();
        List<RequestView> outgoing = new ArrayList<>();
        for (Friendship f : all) {
            Person other = person(f.other(me.getId()), pictures);
            if (f.getStatus() == Friendship.Status.ACCEPTED) {
                friends.add(new FriendView(other, f.getRespondedAt()));
            } else if (f.getAddressee().getId().equals(me.getId())) {
                incoming.add(new RequestView(f.getId(), other, f.getCreatedAt()));
            } else {
                outgoing.add(new RequestView(f.getId(), other, f.getCreatedAt()));
            }
        }
        friends.sort(Comparator.comparing(v -> v.person().name(), String.CASE_INSENSITIVE_ORDER));
        return new FriendsOverview(friends, incoming, outgoing);
    }

    @Transactional(readOnly = true)
    public List<SearchResult> search(String query) {
        User me = currentUserService.get();
        // LIKE wildcards typed by the user would match everyone.
        String q = query == null ? "" : query.trim().replace("%", "").replace("_", "");
        if (q.length() < 3) {
            throw new BadRequestException("Type at least 3 letters to search.");
        }
        List<User> found = userRepository.search(me.getId(), q, PageRequest.of(0, SEARCH_LIMIT));
        Map<Long, Friendship> links = linksByOther(me);
        Map<Long, String> pictures = picturesFor(found.stream().map(User::getId).toList());
        return found.stream().map(u -> new SearchResult(person(u, pictures), relation(me, links.get(u.getId())))).toList();
    }

    /** Friends of your friends you're not connected to yet, most mutual friends first. */
    @Transactional(readOnly = true)
    public List<Suggestion> suggestions() {
        User me = currentUserService.get();
        Map<Long, Friendship> links = linksByOther(me);
        Set<Long> friendIds = links.entrySet().stream()
                .filter(e -> e.getValue().getStatus() == Friendship.Status.ACCEPTED)
                .map(Map.Entry::getKey).collect(Collectors.toSet());

        Map<Long, Integer> mutual = new HashMap<>();
        Map<Long, User> users = new HashMap<>();
        for (Long friendId : friendIds) {
            for (Friendship f : friendshipRepository.findAllInvolving(friendId)) {
                if (f.getStatus() != Friendship.Status.ACCEPTED) continue;
                User candidate = f.other(friendId);
                Long id = candidate.getId();
                if (id.equals(me.getId()) || links.containsKey(id)) continue;
                mutual.merge(id, 1, Integer::sum);
                users.put(id, candidate);
            }
        }
        List<Long> top = mutual.entrySet().stream()
                .sorted(Map.Entry.<Long, Integer>comparingByValue().reversed())
                .limit(SUGGESTION_LIMIT).map(Map.Entry::getKey).toList();
        Map<Long, String> pictures = picturesFor(top);
        return top.stream().map(id -> new Suggestion(person(users.get(id), pictures), mutual.get(id))).toList();
    }

    /** Sends a request, or accepts theirs if they already asked you. */
    @Transactional
    public void request(Long userId) {
        User me = currentUserService.get();
        if (me.getId().equals(userId)) {
            throw new BadRequestException("You can't add yourself.");
        }
        User other = userRepository.findById(userId).orElseThrow(() -> new NotFoundException("Person not found"));
        Optional<Friendship> existing = friendshipRepository.findBetween(me.getId(), other.getId());
        if (existing.isPresent()) {
            Friendship f = existing.get();
            if (f.getStatus() == Friendship.Status.ACCEPTED) {
                throw new ConflictException("You're already friends.");
            }
            if (f.getRequester().getId().equals(me.getId())) {
                throw new ConflictException("You've already sent a request.");
            }
            accept(f);
            return;
        }
        Friendship f = new Friendship();
        f.setRequester(me);
        f.setAddressee(other);
        f.setStatus(Friendship.Status.PENDING);
        f.setCreatedAt(LocalDateTime.now());
        friendshipRepository.save(f);
    }

    @Transactional
    public void acceptRequest(Long requestId) {
        User me = currentUserService.get();
        Friendship f = friendshipRepository.findById(requestId)
                .filter(r -> r.getAddressee().getId().equals(me.getId()) && r.getStatus() == Friendship.Status.PENDING)
                .orElseThrow(() -> new NotFoundException("Request not found"));
        accept(f);
    }

    /** Declines a request sent to you, or cancels one you sent. */
    @Transactional
    public void dismissRequest(Long requestId) {
        User me = currentUserService.get();
        Friendship f = friendshipRepository.findById(requestId)
                .filter(r -> r.getStatus() == Friendship.Status.PENDING)
                .filter(r -> r.getAddressee().getId().equals(me.getId()) || r.getRequester().getId().equals(me.getId()))
                .orElseThrow(() -> new NotFoundException("Request not found"));
        friendshipRepository.delete(f);
    }

    @Transactional
    public void unfriend(Long userId) {
        User me = currentUserService.get();
        Friendship f = friendshipRepository.findBetween(me.getId(), userId)
                .filter(r -> r.getStatus() == Friendship.Status.ACCEPTED)
                .orElseThrow(() -> new NotFoundException("You aren't friends with this person"));
        friendshipRepository.delete(f);
    }

    /** Emails an invitation to join; if they already have an account, sends them a friend request instead. */
    @Transactional
    public InviteResult invite(String email) {
        User me = currentUserService.get();
        String address = email.trim();
        if (address.equalsIgnoreCase(me.getEmail())) {
            throw new BadRequestException("That's your own email address.");
        }
        Optional<User> existing = userRepository.findByEmail(address);
        if (existing.isPresent()) {
            Optional<Friendship> link = friendshipRepository.findBetween(me.getId(), existing.get().getId());
            if (link.isEmpty()) {
                request(existing.get().getId());
                return new InviteResult("They're already on FitTracker, so we sent them a friend request.");
            }
            return new InviteResult(link.get().getStatus() == Friendship.Status.ACCEPTED
                    ? "You're already friends." : "There's already a friend request between you.");
        }
        Deque<LocalDateTime> sent = recentInvites.computeIfAbsent(me.getId(), id -> new ArrayDeque<>());
        synchronized (sent) {
            LocalDateTime dayAgo = LocalDateTime.now().minusDays(1);
            while (!sent.isEmpty() && sent.peekFirst().isBefore(dayAgo)) sent.pollFirst();
            if (sent.size() >= INVITES_PER_DAY) {
                throw new BadRequestException("You've sent " + INVITES_PER_DAY + " invitations today. Try again tomorrow.");
            }
            sent.addLast(LocalDateTime.now());
        }
        notificationService.sendFriendInvite(me.getName(), address, frontendUrl + "/register");
        return new InviteResult("Invitation sent to " + address + ".");
    }

    public long pendingCount(User me) {
        return friendshipRepository.countByAddresseeIdAndStatus(me.getId(), Friendship.Status.PENDING);
    }

    public boolean areFriends(Long a, Long b) {
        return friendshipRepository.findBetween(a, b).map(f -> f.getStatus() == Friendship.Status.ACCEPTED).orElse(false);
    }

    /** Name and photo for each person, looked up in one query. */
    public Map<Long, String> picturesFor(Collection<Long> userIds) {
        if (userIds.isEmpty()) return Map.of();
        Map<Long, String> pictures = new HashMap<>();
        for (Profile p : profileRepository.findByUserIdIn(new HashSet<>(userIds))) {
            if (p.getProfilePic() != null) pictures.put(p.getUser().getId(), p.getProfilePic());
        }
        return pictures;
    }

    public static Person person(User u, Map<Long, String> pictures) {
        return new Person(u.getId(), u.getName(), pictures.get(u.getId()));
    }

    private void accept(Friendship f) {
        f.setStatus(Friendship.Status.ACCEPTED);
        f.setRespondedAt(LocalDateTime.now());
        friendshipRepository.save(f);
    }

    private Map<Long, Friendship> linksByOther(User me) {
        Map<Long, Friendship> links = new HashMap<>();
        for (Friendship f : friendshipRepository.findAllInvolving(me.getId())) {
            links.put(f.other(me.getId()).getId(), f);
        }
        return links;
    }

    private static String relation(User me, Friendship f) {
        if (f == null) return "NONE";
        if (f.getStatus() == Friendship.Status.ACCEPTED) return "FRIEND";
        return f.getRequester().getId().equals(me.getId()) ? "REQUESTED" : "INCOMING";
    }
}
