// 수정 금지
package com.example.project.member.service;

import java.security.Principal;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.project.member.domain.TravelUser;
import com.example.project.member.repository.TravelUserRepository;
import com.example.project.security.user.ChangePasswordRequest;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TravelUserService {

    private final PasswordEncoder passwordEncoder;
    private final TravelUserRepository repository;
    
    
    
    public void changePassword(ChangePasswordRequest request, Principal connectedUser) {

        var user = (TravelUser) ((UsernamePasswordAuthenticationToken) connectedUser).getPrincipal();

        // check if the current password is correct
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new IllegalStateException("Wrong password");
        }
        // check if the two new passwords are the same
        if (!request.getNewPassword().equals(request.getConfirmationPassword())) {
            throw new IllegalStateException("Password are not the same");
        }

        // update the password
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));

        // save the new password
        repository.save(user);
    }
}
