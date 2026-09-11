package com.fitness.tracker.dto;

import lombok.Data;

@Data
public class ProfileDTO {
    private Integer age;
    private String gender;
    private Double weight;
    private Double height;
    private String profilePic;
}