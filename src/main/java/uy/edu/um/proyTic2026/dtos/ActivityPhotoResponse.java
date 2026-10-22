package uy.edu.um.proyTic2026.dtos;

import uy.edu.um.proyTic2026.entities.ActivityPhoto;

public record ActivityPhotoResponse(Long id, String url, String fileName) {
    public static ActivityPhotoResponse from(Long activityId, ActivityPhoto photo) {
        return new ActivityPhotoResponse(photo.getId(), "/api/activities/" + activityId + "/photos/" + photo.getId(), photo.getFileName());
    }
}
