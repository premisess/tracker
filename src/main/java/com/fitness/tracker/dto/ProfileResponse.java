package com.fitness.tracker.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ProfileResponse {
    private Long id;
    private Integer age;
    private String gender;
    private Double weight;
    private Double height;
    private String profilePic;
    private String userName;
    private String userEmail;
}
