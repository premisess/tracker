package com.fitness.tracker.controller;

import com.fitness.tracker.dto.ProfileDTO;
import com.fitness.tracker.dto.ProfileResponse;
import com.fitness.tracker.entity.Profile;
import com.fitness.tracker.repository.ProfileRepository;
import com.fitness.tracker.repository.UserRepository;
import com.fitness.tracker.security.SecurityUtil;
import com.fitness.tracker.service.ProfileService;
import com.fitness.tracker.entity.User;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private final ProfileService profileService;
    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;

    public ProfileController(ProfileService profileService, UserRepository userRepository, ProfileRepository profileRepository) {
        this.profileService = profileService;
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
    }

    @GetMapping
    public ResponseEntity<ProfileResponse> getProfile() {
        return ResponseEntity.ok(profileService.getMyProfileResponse());
    }

    @PutMapping
    public ResponseEntity<ProfileResponse> updateProfile(@RequestBody ProfileDTO dto) {
        return ResponseEntity.ok(profileService.updateMyProfileResponse(dto));
    }

    @PostMapping("/upload-picture")
    public ResponseEntity<String> uploadPicture(@RequestParam("file") MultipartFile file) {
        try {
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return ResponseEntity.badRequest().body("Only image uploads are allowed");
            }

            String uploadDir = "uploads/";
            File dir = new File(uploadDir);
            if (!dir.exists()) dir.mkdirs();

            // Strip any path segments from the original name so it can't escape the uploads dir.
            String originalName = Paths.get(file.getOriginalFilename() != null ? file.getOriginalFilename() : "image")
                    .getFileName().toString();
            String fileName = System.currentTimeMillis() + "_" + originalName;
            String filePath = uploadDir + fileName;
            file.transferTo(new File(filePath));

            String email = SecurityUtil.getCurrentUserEmail();
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            Profile profile = profileRepository.findByUserId(user.getId())
                    .orElseGet(() -> {
                        Profile p = new Profile();
                        p.setUser(user);
                        return p;
                    });

            profile.setProfilePic(fileName);
            profileRepository.save(profile);

            return ResponseEntity.ok(fileName);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Upload failed");
        }
    }

    @GetMapping("/picture/{fileName}")
    public ResponseEntity<Resource> getPicture(@PathVariable String fileName) {
        try {
            Path uploadsDir = Paths.get("uploads/").toAbsolutePath().normalize();
            Path filePath = uploadsDir.resolve(fileName).normalize();
            if (!filePath.startsWith(uploadsDir)) {
                return ResponseEntity.notFound().build();
            }
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists()) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok()
                    .header("Content-Type", "image/jpeg")
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}