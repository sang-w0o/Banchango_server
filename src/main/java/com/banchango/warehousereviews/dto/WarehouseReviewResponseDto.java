package com.banchango.warehousereviews.dto;

import com.banchango.domain.warehousereviews.WarehouseReviews;
import com.banchango.tools.ObjectMaker;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.json.JSONObject;

@NoArgsConstructor
@Getter
@Setter
public class WarehouseReviewResponseDto {

    private int reviewId;
    private int rating;
    private String content;
    private int userId;

    public WarehouseReviewResponseDto(WarehouseReviews reviews) {
        this.reviewId = reviews.getReviewId();
        this.rating = reviews.getRating();
        this.content = reviews.getContent();
        this.userId = reviews.getUserId();
    }

    public JSONObject toJSONObject() {
        JSONObject jsonObject = ObjectMaker.getJSONObject();
        jsonObject.put("reviewId", reviewId);
        jsonObject.put("rating", rating);
        jsonObject.put("content", content);
        return jsonObject;
    }
}
