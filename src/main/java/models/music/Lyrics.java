package models.music;

import java.util.ArrayList;
import java.util.List;

public class Lyrics {
    private String originalLyrics;
    private final List<String> suggestedEdits;
    private final List<String> approvedEdits;

    public Lyrics(String originalLyrics) {
        if (originalLyrics == null || originalLyrics.trim().isEmpty()) {
            throw new IllegalArgumentException("Original lyrics cannot be null or empty");
        }
        this.originalLyrics = originalLyrics;
        this.suggestedEdits = new ArrayList<>();
        this.approvedEdits = new ArrayList<>();
    }

    public String getOriginalLyrics() {
        return originalLyrics;
    }

    public void suggestEdit(String editedLyrics) {
        if (editedLyrics == null || editedLyrics.trim().isEmpty()) {
            throw new IllegalArgumentException("Suggested lyrics cannot be null or empty");
        }
        suggestedEdits.add(editedLyrics);
    }

    public List<String> getSuggestedEdits() {
        return new ArrayList<>(suggestedEdits);
    }

    public void approveEdit(String approvedLyrics) {
        if (approvedLyrics == null || approvedLyrics.trim().isEmpty()) {
            throw new IllegalArgumentException("Approved lyrics cannot be null or empty");
        }
        approvedEdits.add(approvedLyrics);
        this.originalLyrics = approvedLyrics;
    }

    public String getApprovedLyrics() {
        return approvedEdits.isEmpty() ? originalLyrics : approvedEdits.getLast();
    }

    @Override
    public String toString() {
        return "Lyrics: " + originalLyrics +
                ", Suggested Edits: " + suggestedEdits +
                ", Approved Edits: " + approvedEdits;
    }
}