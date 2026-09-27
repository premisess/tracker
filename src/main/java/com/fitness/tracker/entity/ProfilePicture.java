package com.fitness.tracker.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/** A user's profile photo, stored in the database so it survives the app restarting. */
@Entity
@Table(name = "profile_pictures")
@Getter
@Setter
public class ProfilePicture {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "file_name", nullable = false, unique = true)
    private String fileName;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Lob
    @JdbcTypeCode(SqlTypes.LONG32VARBINARY)
    @Column(nullable = false, columnDefinition = "longblob")
    private byte[] data;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
