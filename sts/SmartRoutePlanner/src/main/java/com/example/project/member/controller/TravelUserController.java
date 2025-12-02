package com.example.project.member.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.project.member.dto.TravelUserDto;
import com.example.project.member.service.TravelUserService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class TravelUserController {
    private final TravelUserService tuService;

    @PostMapping
    public String signUp(@RequestBody TravelUserDto dto) {
    	return null;
    }
    	
    
	
	
	
}
