package com.example.project.member.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateProfileRequest {
    private String nickname;
    private String gender;
    private Integer age;
}
