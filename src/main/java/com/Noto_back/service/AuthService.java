package com.Noto_back.service;


import com.Noto_back.dto.AuthResponse;
import com.Noto_back.dto.LoginRequest;
import com.Noto_back.dto.RefreshRequest;
import com.Noto_back.dto.RegisterRequest;
import com.Noto_back.exceptions.ResourceNotFoundException;
import com.Noto_back.model.RefreshToken;
import com.Noto_back.model.User;
import com.Noto_back.model.Role;
import com.Noto_back.repository.RefreshTokenRepository;
import com.Noto_back.repository.UserRepository;
import com.Noto_back.security.UserPrincipal;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@Transactional
@RequiredArgsConstructor
public class AuthService {


    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    public AuthResponse register(RegisterRequest rq){
        if(userRepository.existsByEmail(rq.email())){
            throw new IllegalArgumentException("Cet email est déjà utilisé");
        }
        if (userRepository.existsByUsername(rq.username())){
            throw new IllegalArgumentException("Ce nom d'utilisateur est déjà pris");
        }
        User user = User.builder()
                .username(rq.username())
                .email(rq.email())
                .password(passwordEncoder.encode(rq.password()))
                .role(Role.USER)
                .build();

        return genererTokens(new UserPrincipal(user));
    }

    public AuthResponse login(LoginRequest lr){
        try{
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(lr.email(),lr.password()));

        } catch (Exception e){
            throw new BadCredentialsException("Email ou mot de passe incorrect");
        }
        User user = userRepository.findByEmail(lr.email())
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable : "+ lr.email()));
        return genererTokens(new UserPrincipal(user));
    }

    public AuthResponse refresh(RefreshRequest rr){
        String refreshTokenStr = rr.refreshToken();
        RefreshToken storedToken = refreshTokenRepository.findByToken(refreshTokenStr)
                .orElseThrow(() -> new BadCredentialsException("Refresh token invalide"));

        if (storedToken.isRevoked() || storedToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new BadCredentialsException("Refresh token expiré ou révoqué, veuillez vous reconnecter");
        }

        if (!jwtService.isTokenStructurallyValid(refreshTokenStr)) {
            throw new BadCredentialsException("Refresh token invalide");
        }

        User user = storedToken.getUser();

        // Rotation : on révoque l'ancien refresh token et on en émet un nouveau
        storedToken.setRevoked(true);
        refreshTokenRepository.save(storedToken);

        return genererTokens(new UserPrincipal(user));
    }

    public void logout(String refreshTokenStr){
        refreshTokenRepository.findByToken(refreshTokenStr)
                .ifPresent(refreshToken -> {
                    refreshToken.setRevoked(true);
                    refreshTokenRepository.save(refreshToken);
                });
    }


    private AuthResponse genererTokens(UserPrincipal principal) {
        String accessToken = jwtService.generateAccessToken(principal);
        String refreshToken = jwtService.generateRefreshToken(principal);

        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));

        RefreshToken entity = RefreshToken.builder()
                .token(refreshToken)
                .user(user)
                .expiryDate(LocalDateTime.now().plusSeconds(refreshTokenExpiration / 1000))
                .revoked(false)
                .build();

        refreshTokenRepository.save(entity);

        return new AuthResponse(accessToken, refreshToken);
    }

}
