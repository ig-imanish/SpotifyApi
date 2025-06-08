package com.spotify.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.PostConstruct;

import org.apache.hc.core5.http.ParseException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.exceptions.SpotifyWebApiException;
import se.michaelthelin.spotify.requests.authorization.authorization_code.AuthorizationCodeUriRequest;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;

@Service
public class SpotifyAuthService {

    private final SpotifyApi spotifyApi;
    private final String clientId;
    private final String clientSecret;
    private final String redirectUri;
    private final String tokenFilePath = "spotify_tokens.json";
    private final ObjectMapper objectMapper = new ObjectMapper();

    public SpotifyAuthService(
            @Value("${spotify.client.id}") String clientId,
            @Value("${spotify.client.secret}") String clientSecret,
            @Value("${spotify.redirect.uri}") String redirectUri) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.redirectUri = redirectUri;

        this.spotifyApi = new SpotifyApi.Builder()
                .setClientId(clientId)
                .setClientSecret(clientSecret)
                .setRedirectUri(URI.create(redirectUri))
                .build();
    }

    @PostConstruct
    public void init() {
        validateConfiguration();
        loadStoredTokens();
        if (hasValidRefreshToken()) {
            try {
                refreshAccessToken();
            } catch (Exception e) {
                System.err.println("Failed to refresh token on startup: " + e.getMessage());
            }
        }
    }

    private void validateConfiguration() {
        if (clientId == null || clientId.isEmpty() ||
            clientSecret == null || clientSecret.isEmpty() ||
            redirectUri == null || redirectUri.isEmpty()) {
            throw new IllegalStateException("Spotify configuration properties are missing or invalid");
        }
    }

    public boolean hasValidRefreshToken() {
        return spotifyApi.getRefreshToken() != null && !spotifyApi.getRefreshToken().isEmpty();
    }

    public URI getAuthorizationUri() {
        AuthorizationCodeUriRequest authorizationCodeUriRequest = spotifyApi.authorizationCodeUri()
                .scope("user-read-currently-playing,user-read-playback-state")
                .show_dialog(true)
                .build();
        return authorizationCodeUriRequest.execute();
    }

    public void exchangeCodeForTokens(String code) throws IOException, SpotifyWebApiException, ParseException {
        var credentials = spotifyApi.authorizationCode(code).build().execute();
        spotifyApi.setAccessToken(credentials.getAccessToken());
        spotifyApi.setRefreshToken(credentials.getRefreshToken());
        saveTokensToFile(credentials.getRefreshToken());
    }

    public void refreshAccessToken() throws IOException, SpotifyWebApiException, ParseException {
        var credentials = spotifyApi.authorizationCodeRefresh().build().execute();
        spotifyApi.setAccessToken(credentials.getAccessToken());
        System.out.println("Access token refreshed successfully");
    }

    private void saveTokensToFile(String refreshToken) throws IOException {
        TokenData tokenData = new TokenData(refreshToken);
        objectMapper.writeValue(new File(tokenFilePath), tokenData);
        System.out.println("Refresh token saved to " + tokenFilePath);
    }

    private void loadStoredTokens() {
        try {
            if (Files.exists(Paths.get(tokenFilePath))) {
                TokenData tokenData = objectMapper.readValue(new File(tokenFilePath), TokenData.class);
                spotifyApi.setRefreshToken(tokenData.getRefreshToken());
                System.out.println("Loaded refresh token from " + tokenFilePath);
            }
        } catch (IOException e) {
            System.err.println("Failed to load refresh token: " + e.getMessage());
        }
    }

    public SpotifyApi getSpotifyApi() {
        return spotifyApi;
    }

    private static class TokenData {
        private String refreshToken;

        public TokenData() {}

        public TokenData(String refreshToken) {
            this.refreshToken = refreshToken;
        }

        public String getRefreshToken() {
            return refreshToken;
        }

        public void setRefreshToken(String refreshToken) {
            this.refreshToken = refreshToken;
        }
    }
}