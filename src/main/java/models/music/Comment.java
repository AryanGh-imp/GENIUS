package models.music;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Comment {
    private final String userNickName;
    private String text;
    private final LocalDateTime timestamp;

    public Comment(String userNickName, String text) {
        if (userNickName == null || userNickName.trim().isEmpty()) {
            throw new IllegalArgumentException("User nickname cannot be null or empty");
        }
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("Comment text cannot be null or empty");
        }
        this.userNickName = userNickName;
        this.text = text;
        this.timestamp = LocalDateTime.now();
    }

    public String getUserNickName() {
        return userNickName;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("Comment text cannot be null or empty");
        }
        this.text = text;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return "User: " + userNickName + " | Time: " + timestamp.format(formatter) + " | Comment: " + text;
    }
}