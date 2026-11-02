// 내 정보 조회
package com.example.project.member.dto;

import com.example.project.security.user.Role;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class UserInfoResponse {

    private Integer id;
    private String nickname;
    private String email;
    private Integer age;
    private String gender;
    private Role role;
    public static UserInfoResponse from(com.example.project.member.domain.TravelUser user) {
        return new UserInfoResponse(user.getId(), user.getNickname(), user.getEmail(),
                user.getAge(), user.getGender(), user.getRole());
    }
}

