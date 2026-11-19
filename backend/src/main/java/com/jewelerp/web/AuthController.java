package com.jewelerp.web;

import com.jewelerp.dto.Requests;
import com.jewelerp.model.AppUser;
import com.jewelerp.service.AuthService;
import com.jewelerp.service.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService auth;
    private final CurrentUser currentUser;

    @PostMapping("/login")
    public AuthService.LoginResult login(@Valid @RequestBody Requests.Login req) {
        return auth.login(req.email(), req.password());
    }

    @GetMapping("/me")
    public AppUser me() {
        return currentUser.get();
    }
}
