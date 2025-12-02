package com.example.project.member.dto;

import java.time.LocalDate;
import java.util.List;

import com.example.project.route.dto.PlaceSummaryDto;
import com.example.project.security.user.Role;

import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class TravelUserDto {
	 	@Column(length = 50)
	    private String nickname;

	    @Column(length = 1)
	    private String gender; // 'M', 'F'

	    private Integer age;

	    @Column(length = 100, unique = true)
	    private String email;


	    @Column(length = 300)
	    private String password;
	
}
