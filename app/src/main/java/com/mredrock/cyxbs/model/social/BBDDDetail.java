package com.mredrock.cyxbs.model.social;

import com.google.gson.annotations.SerializedName;
import com.redrock.common.network.RedRockApiWrapper;

import java.util.List;

/**
 * Created by skylineTan on 2016/5/1 18:04.
 */
public class BBDDDetail {

    public String id;
    public String content;

    @SerializedName("photo_src")
    public String photoSrc;
    @SerializedName("thumbnail_src")
    public String thumbnailSrc;
    @SerializedName("type_id")
    public String typeId;
    @SerializedName("updated_time")
    public String updatedTime;
    @SerializedName("created_time")
    public String createdTime;
    @SerializedName("date")
    public String date;
    @SerializedName("like_num")
    public String likeNum;
    @SerializedName("remark_num")
    public String remarkNum;
    @SerializedName("is_my_like")
    public boolean isMyLike;
    @SerializedName("user_photo")
    public String userHead;
    @SerializedName("nickname")
    public String nickName;

    public static class BBDDDetailWrapper extends RedRockApiWrapper<List<BBDDDetail>> {

    }
}
