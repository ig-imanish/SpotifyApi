package com.spotify.api.controllers;

import org.apache.hc.core5.http.ParseException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.spotify.api.service.SpotifyAuthService;
import se.michaelthelin.spotify.exceptions.SpotifyWebApiException;

import java.io.IOException;
import java.net.URI;

@RestController
public class SpotifyAuthController {

    private final SpotifyAuthService spotifyAuthService;

    public SpotifyAuthController(SpotifyAuthService spotifyAuthService) {
        this.spotifyAuthService = spotifyAuthService;
    }

    @GetMapping("/login")
    public ResponseEntity<Void> login() {
        try {
            if (spotifyAuthService.hasValidRefreshToken()) {
                return ResponseEntity.ok().build(); // No need to login if refresh token exists
            }
            URI uri = spotifyAuthService.getAuthorizationUri();
            return ResponseEntity.status(HttpStatus.FOUND).location(uri).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/callback")
    public ResponseEntity<String> callback(@RequestParam("code") String code) throws ParseException {
        try {
            spotifyAuthService.exchangeCodeForTokens(code);
            return ResponseEntity.ok("Authentication successful. Tokens stored. You can now use Spotify API endpoints.");
        } catch (IOException | SpotifyWebApiException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error during token exchange: " + e.getMessage());
        }
    }
}