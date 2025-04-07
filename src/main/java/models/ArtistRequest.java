package models;

/**
 * Represents an artist request with details such as email, nickname, password, status, and timestamp.
 */
public class ArtistRequest {
    private final String email;
    private final String nickname;
    private final String password;
    private final String status;
    private final String timestamp;

    /**
     * Constructs a new ArtistRequest with the specified details.
     *
     * @param email     The email of the artist.
     * @param nickname  The nickname of the artist.
     * @param password  The password of the artist.
     * @param status    The status of the request (e.g., Pending, Approved, Rejected).
     * @param timestamp The timestamp of the request.
     */
    public ArtistRequest(String email, String nickname, String password, String status, String timestamp) {
        this.email = email;
        this.nickname = nickname;
        this.password = password;
        this.status = status;
        this.timestamp = timestamp;
    }

    // Getters
    public String getEmail() { return email; }
    public String getNickname() { return nickname; }
    public String getPassword() { return password; }
    public String getStatus() { return status; }
    public String getTimestamp() { return timestamp; }

    @Override
    public String toString() {
        return "Artist Request: " + nickname + " (" + status + ")";
    }
}