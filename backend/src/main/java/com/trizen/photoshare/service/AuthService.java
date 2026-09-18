package com.trizen.photoshare.service;

import com.trizen.photoshare.dto.AuthDtos.AuthResponse;
import com.trizen.photoshare.dto.AuthDtos.LoginRequest;
import com.trizen.photoshare.dto.AuthDtos.RegisterRequest;
import com.trizen.photoshare.dto.AuthDtos.UserDto;
import com.trizen.photoshare.entity.Role;
import com.trizen.photoshare.entity.User;
import com.trizen.photoshare.exception.ConflictException;
import com.trizen.photoshare.repository.UserRepository;
import com.trizen.photoshare.security.AppUserPrincipal;
import com.trizen.photoshare.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    /** Self service signup creates a studio lead. Team members are created by an admin. */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new ConflictException("An account already exists for that email");
        }
        User user = new User(request.name().trim(),
                request.email().trim().toLowerCase(),
                passwordEncoder.encode(request.password()),
                Role.ADMIN);
        userRepository.save(user);
        return tokenFor(new AppUserPrincipal(user));
    }

    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.email().trim().toLowerCase(), request.password()));
        return tokenFor((AppUserPrincipal) authentication.getPrincipal());
    }

    public UserDto currentUser(AppUserPrincipal principal) {
        return new UserDto(principal.getId(), principal.getName(),
                principal.getEmail(), principal.getRole().name());
    }

    private AuthResponse tokenFor(AppUserPrincipal principal) {
        return new AuthResponse(jwtService.issueAccessToken(principal),
                jwtService.accessTokenExpiry(),
                currentUser(principal));
    }
}
