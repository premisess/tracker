package com.fitness.tracker.service;

import com.fitness.tracker.exception.UnauthorizedException;
import com.fitness.tracker.dto.ProfileDTO;
import com.fitness.tracker.dto.ProfileResponse;
import com.fitness.tracker.entity.Profile;
import com.fitness.tracker.entity.User;
import com.fitness.tracker.repository.ProfileRepository;
import com.fitness.tracker.repository.UserRepository;
import com.fitness.tracker.security.SecurityUtil;
import org.springframework.stereotype.Service;

@Service
public class ProfileService {

    private final ProfileRepository profileRepository;
    private final UserRepository userRepository;

    public ProfileService(ProfileRepository profileRepository, UserRepository userRepository) {
        this.profileRepository = profileRepository;
        this.userRepository = userRepository;
    }

    public Profile getMyProfile() {
        User user = getCurrentUser();
        return profileRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    Profile newProfile = new Profile();
                    newProfile.setUser(user);
                    return profileRepository.save(newProfile);
                });
    }

    public Profile updateMyProfile(ProfileDTO dto) {
        Profile profile = getMyProfile();
        profile.setAge(dto.getAge());
        profile.setGender(dto.getGender());
        profile.setWeight(dto.getWeight());
        profile.setHeight(dto.getHeight());
        profile.setProfilePic(dto.getProfilePic());
        return profileRepository.save(profile);
    }

    private User getCurrentUser() {
        String email = SecurityUtil.getCurrentUserEmail();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("Your session has expired. Please sign in again."));
    }

    public ProfileResponse getMyProfileResponse() {
        Profile profile = getMyProfile();
        return new ProfileResponse(
                profile.getId(),
                profile.getAge(),
                profile.getGender(),
                profile.getWeight(),
                profile.getHeight(),
                profile.getProfilePic(),
                profile.getUser().getName(),
                profile.getUser().getEmail()
        );
    }

    public ProfileResponse updateMyProfileResponse(ProfileDTO dto) {
        Profile profile = updateMyProfile(dto);
        return new ProfileResponse(
                profile.getId(),
                profile.getAge(),
                profile.getGender(),
                profile.getWeight(),
                profile.getHeight(),
                profile.getProfilePic(),
                profile.getUser().getName(),
                profile.getUser().getEmail()
        );
    }

}