package com.fitness.tracker.controller;

import jakarta.validation.Valid;
import com.fitness.tracker.exception.BadRequestException;
import com.fitness.tracker.exception.UnauthorizedException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import com.fitness.tracker.dto.ProfileDTO;
import com.fitness.tracker.dto.ProfileResponse;
import com.fitness.tracker.entity.Profile;
import com.fitness.tracker.entity.ProfilePicture;
import com.fitness.tracker.repository.ProfilePictureRepository;
import com.fitness.tracker.repository.ProfileRepository;
import com.fitness.tracker.repository.UserRepository;
import com.fitness.tracker.security.SecurityUtil;
import com.fitness.tracker.service.ProfileService;
import com.fitness.tracker.entity.User;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.core.io.UrlResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.UUID;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private final ProfileService profileService;
    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final ProfilePictureRepository profilePictureRepository;

    public ProfileController(ProfileService profileService, UserRepository userRepository, ProfileRepository profileRepository,
                             ProfilePictureRepository profilePictureRepository) {
        this.profileService = profileService;
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.profilePictureRepository = profilePictureRepository;
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
        // Photo formats only: an SVG can carry scripts, and photos are served from the site's own address.
        if (contentType == null || !java.util.Set.of("image/jpeg", "image/png", "image/gif", "image/webp").contains(contentType)) {
            throw new BadRequestException("Please upload a JPG, PNG, GIF or WebP image");
        }

        byte[] bytes;
        try (InputStream in = file.getInputStream()) {
            bytes = in.readAllBytes();
        } catch (IOException e) {
            throw new BadRequestException("Upload failed. Please try a different image.");
        }
        // A new random name each time, so browsers never show a cached old photo.
        String fileName = UUID.randomUUID() + extensionFor(contentType);

        String email = SecurityUtil.getCurrentUserEmail();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("Your session has expired. Please sign in again."));

        Profile profile = profileRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    Profile p = new Profile();
                    p.setUser(user);
                    return p;
                });

        // Stored in the database rather than on disk, so it survives the app restarting.
        ProfilePicture picture = profilePictureRepository.findById(user.getId()).orElseGet(ProfilePicture::new);
        picture.setUserId(user.getId());
        picture.setFileName(fileName);
        picture.setContentType(contentType);
        picture.setData(bytes);
        picture.setUpdatedAt(LocalDateTime.now());
        profilePictureRepository.save(picture);

        profile.setProfilePic(fileName);
        profileRepository.save(profile);

        return ResponseEntity.ok(fileName);
    }

    @GetMapping("/picture/{fileName}")
    public ResponseEntity<Resource> getPicture(@PathVariable String fileName) {
        var stored = profilePictureRepository.findByFileName(fileName);
        if (stored.isPresent()) {
            ProfilePicture picture = stored.get();
            return ResponseEntity.ok()
                    .header("Content-Type", picture.getContentType())
                    // Each upload gets a new name, so a name's content never changes.
                    .cacheControl(CacheControl.maxAge(java.time.Duration.ofDays(30)).cachePrivate())
                    .body(new ByteArrayResource(picture.getData()));
        }
        // Photos uploaded before they were kept in the database.
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

    private static String extensionFor(String contentType) {
        return switch (contentType) {
            case "image/png" -> ".png";
            case "image/gif" -> ".gif";
            case "image/webp" -> ".webp";
            default -> ".jpg";
        };
    }
}
