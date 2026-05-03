package online.dbarenholz.omiyage.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import online.dbarenholz.omiyage.dto.LoginRequest;
import online.dbarenholz.omiyage.dto.SignupResponse;
import online.dbarenholz.omiyage.dto.UpdateDisplayNameRequest;
import online.dbarenholz.omiyage.dto.UserResponse;
import online.dbarenholz.omiyage.entity.User;
import online.dbarenholz.omiyage.repository.UserRepository;
import online.dbarenholz.omiyage.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;

    @PostMapping("/auth/signup")
    public ResponseEntity<SignupResponse> signup() {
        SignupResponse response = authService.signup();
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/auth/login")
    public ResponseEntity<UserResponse> login(@RequestBody LoginRequest request,
                                              HttpServletRequest httpRequest) {
        UserResponse response = authService.login(request.accountNumber(), httpRequest);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/auth/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        authService.logout(request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/auth/me")
    public ResponseEntity<UserResponse> me(Authentication auth) {
        User user = (User) auth.getPrincipal();
        return ResponseEntity.ok(authService.toUserResponse(user));
    }

    @GetMapping("/auth/csrf")
    public ResponseEntity<Map<String, String>> csrf(CsrfToken csrfToken) {
        return ResponseEntity.ok(Map.of("token", csrfToken.getToken()));
    }

    @PatchMapping("/users/me")
    public ResponseEntity<UserResponse> updateMe(@RequestBody UpdateDisplayNameRequest request,
                                                 Authentication auth) {
        if (request.displayName() == null || request.displayName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Display name is required");
        }

        User user = (User) auth.getPrincipal();
        user.setDisplayName(request.displayName().trim());
        User saved = userRepository.save(user);
        return ResponseEntity.ok(authService.toUserResponse(saved));
    }
}
