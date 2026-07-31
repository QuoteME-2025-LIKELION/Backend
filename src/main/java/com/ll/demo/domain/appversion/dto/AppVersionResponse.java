package com.ll.demo.domain.appversion.dto;

public class AppVersionResponse {
    private final String latestVersion;
    private final String updateNote;

    public AppVersionResponse(String latestVersion, String updateNote) {
        this.latestVersion = latestVersion;
        this.updateNote = updateNote;
    }

    public String getLatestVersion() {
        return latestVersion;
    }

    public String getUpdateNote() {
        return updateNote;
    }
}
