package com.spotify.api.controllers;

import java.io.IOException;

import org.apache.hc.core5.http.ParseException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.spotify.api.service.SpotifyService;
import se.michaelthelin.spotify.exceptions.SpotifyWebApiException;
import se.michaelthelin.spotify.model_objects.miscellaneous.CurrentlyPlayingContext;

@RestController
public class SpotifyController {

    private final SpotifyService spotifyService;

    public SpotifyController(SpotifyService spotifyService) {
        this.spotifyService = spotifyService;
    }

    @GetMapping("/currently-playing")
    public ResponseEntity<?> getCurrentlyPlaying() throws ParseException {
        try {
            CurrentlyPlayingContext currentlyPlaying = spotifyService.getCurrentlyPlaying();
            if (currentlyPlaying == null || currentlyPlaying.getItem() == null) {
                return ResponseEntity.ok("No track is currently playing.");
            }
            return ResponseEntity.ok(currentlyPlaying);
        } catch (IOException | SpotifyWebApiException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error fetching currently playing track: " + e.getMessage());
        }
    }
}