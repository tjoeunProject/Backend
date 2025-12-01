package com.example.project.security.auth;

import com.example.project.security.user.Role;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RegisterRequest {

	// 기존 firstname, lastname 삭제 -> nickname으로 통합
	  private String nickname;
	  private String email;
	  private String password;
	  
	  // 새로 추가된 필드들 (DB 스키마 반영)
	  private String gender; // M or F
	  private Integer age;
	  
	  // 선택사항: 회원가입 시 Role을 지정할지 여부 (기본값 USER로 설정하면 제외 가능)
	  private Role role;
}
