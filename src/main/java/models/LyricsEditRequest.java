package models;

public record LyricsEditRequest(String email, String artistNickname, String songTitle, String albumName,
                                String suggestedLyrics, String timestamp, String status) {
}