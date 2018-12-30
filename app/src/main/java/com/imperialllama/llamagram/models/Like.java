package com.imperialllama.llamagram.models;

public class Like {
    public String imageId;
    public String userId;

    public Like() {
        // Default constructor required for calls to DataSnapshot.getValue(Like.class)
    }

    public Like(String imageId, String userId) {
        this.imageId = imageId;
        this.userId = userId;
    }
}
