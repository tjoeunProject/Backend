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
}
