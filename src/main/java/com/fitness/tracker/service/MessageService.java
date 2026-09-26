package com.fitness.tracker.service;

import com.fitness.tracker.dto.SocialDtos.Alerts;
import com.fitness.tracker.dto.SocialDtos.Conversation;
import com.fitness.tracker.dto.SocialDtos.MessageView;
import com.fitness.tracker.dto.SocialDtos.SendMessageRequest;
import com.fitness.tracker.entity.Message;
import com.fitness.tracker.entity.User;
import com.fitness.tracker.exception.ForbiddenException;
import com.fitness.tracker.exception.NotFoundException;
import com.fitness.tracker.repository.MessageRepository;
import com.fitness.tracker.repository.UserRepository;
import com.fitness.tracker.security.CurrentUserService;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/** Chat between friends. Only friends can message each other. */
@Service
public class MessageService {

    private static final int THREAD_LIMIT = 100;
    // Enough recent messages to find the latest one with each friend.
    private static final int INBOX_SCAN = 500;

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;
    private final FriendService friendService;

    public MessageService(MessageRepository messageRepository, UserRepository userRepository,
                          CurrentUserService currentUserService, FriendService friendService) {
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.currentUserService = currentUserService;
        this.friendService = friendService;
    }

    /** Latest message with each person, newest conversation first. */
    @Transactional(readOnly = true)
    public List<Conversation> conversations() {
        User me = currentUserService.get();
        Map<Long, Message> latest = new LinkedHashMap<>();
        Map<Long, User> people = new HashMap<>();
        for (Message m : messageRepository.findAllInvolving(me.getId(), PageRequest.of(0, INBOX_SCAN))) {
            User other = m.getSender().getId().equals(me.getId()) ? m.getRecipient() : m.getSender();
            latest.putIfAbsent(other.getId(), m);
            people.putIfAbsent(other.getId(), other);
        }
        Map<Long, Long> unread = new HashMap<>();
        for (Object[] row : messageRepository.unreadBySender(me.getId())) {
            unread.put((Long) row[0], (Long) row[1]);
        }
        Map<Long, String> pictures = friendService.picturesFor(latest.keySet());
        return latest.entrySet().stream()
                .map(e -> new Conversation(FriendService.person(people.get(e.getKey()), pictures),
                        view(e.getValue(), me.getId()), unread.getOrDefault(e.getKey(), 0L)))
                .toList();
    }

    /** The last messages with one friend, oldest first. Opening it marks their messages as read. */
    @Transactional
    public List<MessageView> thread(Long otherId) {
        User me = currentUserService.get();
        User other = friend(me, otherId);
        messageRepository.markRead(me.getId(), other.getId(), LocalDateTime.now());
        List<Message> newestFirst = messageRepository.findThread(me.getId(), other.getId(), PageRequest.of(0, THREAD_LIMIT));
        List<MessageView> views = new ArrayList<>(newestFirst.stream().map(m -> view(m, me.getId())).toList());
        Collections.reverse(views);
        return views;
    }

    @Transactional
    public MessageView send(Long otherId, SendMessageRequest request) {
        User me = currentUserService.get();
        User other = friend(me, otherId);
        Message m = new Message();
        m.setSender(me);
        m.setRecipient(other);
        m.setKind(request.getKind());
        m.setBody(request.getBody().trim());
        m.setCreatedAt(LocalDateTime.now());
        return view(messageRepository.save(m), me.getId());
    }

    /** Counts for the sidebar badge. */
    @Transactional(readOnly = true)
    public Alerts alerts() {
        User me = currentUserService.get();
        return new Alerts(messageRepository.countByRecipientIdAndReadAtIsNull(me.getId()), friendService.pendingCount(me));
    }

    private User friend(User me, Long otherId) {
        User other = userRepository.findById(otherId).orElseThrow(() -> new NotFoundException("Person not found"));
        if (!friendService.areFriends(me.getId(), other.getId())) {
            throw new ForbiddenException("You can only message your friends.");
        }
        return other;
    }

    private static MessageView view(Message m, Long me) {
        return new MessageView(m.getId(), m.getSender().getId().equals(me), m.getKind(), m.getBody(),
                m.getCreatedAt(), m.getReadAt() != null);
    }
}
