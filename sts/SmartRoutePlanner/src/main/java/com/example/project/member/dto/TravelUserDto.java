package com.example.project.member.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TravelUserDto {

    private String nickname;
    private String gender;
    // private Integer age;
    private String email;
    private String password;
}
