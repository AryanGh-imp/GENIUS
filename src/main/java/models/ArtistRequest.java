package models;


public record ArtistRequest(String email, String nickname, String password, String status, String timestamp) {

    @Override
    public String toString() {
        return String.format("Artist: %s (%s)", nickname, status);
    }
}