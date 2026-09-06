package com.smartgrid.authservice.service;

import com.smartgrid.authservice.domain.User;
import com.smartgrid.authservice.dto.RegisterRequest;
import com.smartgrid.authservice.dto.TokenPairResponse;
import com.smartgrid.authservice.repository.UserRepository;
import com.smartgrid.commons.exception.ConflictException;
import com.smartgrid.commons.exception.ResourceNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final RefreshTokenStore refreshTokenStore;
    private final LoginAttemptService loginAttemptService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            TokenService tokenService,
            RefreshTokenStore refreshTokenStore,
            LoginAttemptService loginAttemptService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.refreshTokenStore = refreshTokenStore;
        this.loginAttemptService = loginAttemptService;
    }

    @Transactional
    public User register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new ConflictException("Username already taken: " + request.username());
        }
        User user = new User(request.username(), passwordEncoder.encode(request.password()), request.role());
        return userRepository.save(user);
    }

    public TokenPairResponse login(String username, String rawPassword) {
        loginAttemptService.lockoutRemaining(username)
                .ifPresent(remaining -> {
                    throw new AccountLockedException(remaining);
                });

        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null || !passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            loginAttemptService.recordFailure(username);
            throw new InvalidCredentialsException();
        }

        loginAttemptService.recordSuccess(username);
        return issueTokenPair(user);
    }

    public TokenPairResponse refresh(String refreshToken) {
        String username = refreshTokenStore.resolveUsername(refreshToken)
                .orElseThrow(InvalidRefreshTokenException::new);
        refreshTokenStore.revoke(refreshToken);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", username));
        return issueTokenPair(user);
    }

    public void logout(String refreshToken) {
        refreshTokenStore.revoke(refreshToken);
    }

    private TokenPairResponse issueTokenPair(User user) {
        String accessToken = tokenService.issueAccessToken(user);
        String refreshToken = refreshTokenStore.issue(user.getUsername());
        return new TokenPairResponse(accessToken, refreshToken, tokenService.accessTokenTtl().getSeconds());
    }
}
