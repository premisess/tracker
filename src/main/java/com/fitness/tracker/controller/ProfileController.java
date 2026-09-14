package com.fitness.tracker.controller;

import jakarta.validation.Valid;
import com.fitness.tracker.exception.BadRequestException;
import com.fitness.tracker.exception.UnauthorizedException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
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
    public ResponseEntity<ProfileResponse> updateProfile(@Valid @RequestBody ProfileDTO dto) {
        return ResponseEntity.ok(profileService.updateMyProfileResponse(dto));
    }

    @PostMapping("/upload-picture")
    public ResponseEntity<String> uploadPicture(@RequestParam("file") MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BadRequestException("Only image uploads are allowed");
        }

        // Strip any path segments from the original name so it can't escape the uploads dir.
        String originalName = Paths.get(file.getOriginalFilename() != null ? file.getOriginalFilename() : "image")
                .getFileName().toString();
        String fileName = System.currentTimeMillis() + "_" + originalName;

        try {
            // Must be absolute: MultipartFile resolves relative paths against the servlet
            // container's temp directory, so the old "uploads/" + name never landed where
            // getPicture() looks for it.
            Path uploadsDir = Paths.get("uploads").toAbsolutePath().normalize();
            Files.createDirectories(uploadsDir);
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, uploadsDir.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new BadRequestException("Upload failed. Please try a different image.");
        }

        String email = SecurityUtil.getCurrentUserEmail();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("Your session has expired. Please sign in again."));

        Profile profile = profileRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    Profile p = new Profile();
                    p.setUser(user);
                    return p;
                });

        profile.setProfilePic(fileName);
        profileRepository.save(profile);

        return ResponseEntity.ok(fileName);
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
            String type = Files.probeContentType(filePath);
            return ResponseEntity.ok()
                    .header("Content-Type", type != null ? type : "application/octet-stream")
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}