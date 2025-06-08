package com.spotify.api.service;

import org.apache.hc.core5.http.ParseException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.exceptions.SpotifyWebApiException;
import se.michaelthelin.spotify.model_objects.miscellaneous.CurrentlyPlayingContext;

import java.io.IOException;

@Service
public class SpotifyService {

    private final SpotifyApi spotifyApi;
    private final SpotifyAuthService spotifyAuthService;

    public SpotifyService(SpotifyAuthService spotifyAuthService) {
        this.spotifyAuthService = spotifyAuthService;
        this.spotifyApi = spotifyAuthService.getSpotifyApi();
    }

    @Scheduled(fixedDelay = 44 * 60 * 1000)
    public void refreshAccessToken() throws ParseException {
        try {
            if (spotifyApi.getRefreshToken() == null) {
                System.out.println("No refresh token available, skipping refresh.");
                return;
            }
            spotifyAuthService.refreshAccessToken();
        } catch (IOException | SpotifyWebApiException e) {
            System.err.println("Failed to refresh Spotify token: " + e.getMessage());
        }
    }

    public CurrentlyPlayingContext getCurrentlyPlaying() throws IOException, SpotifyWebApiException, ParseException {
        return spotifyApi.getInformationAboutUsersCurrentPlayback().build().execute();
    }
}