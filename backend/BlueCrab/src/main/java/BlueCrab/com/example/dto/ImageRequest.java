package BlueCrab.com.example.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 프로필 이미지 조회 요청 DTO
 */
public class ImageRequest {
    
    @JsonProperty("imageKey")
    private String imageKey;
    
    public ImageRequest() {}
    
    public ImageRequest(String imageKey) {
        this.imageKey = imageKey;
    }
    
    public String getImageKey() {
        return imageKey;
    }
    
    public void setImageKey(String imageKey) {
        this.imageKey = imageKey;
    }
    
    @Override
    public String toString() {
        return "ImageRequest{" +
                "imageKey='" + imageKey + '\'' +
                '}';
    }
}