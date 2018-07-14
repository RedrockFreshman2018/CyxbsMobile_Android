package com.mredrock.cyxbs.model;

import com.redrock.common.network.RedRockApiWrapper;

import java.io.Serializable;
import java.util.List;

/**
 * Created by skylineTan on 2016/4/10 16:44.
 */
public class AboutMe implements Serializable {

    public String type;
    public String content;
    public String article_content;
    public String article_photo_src;
    public String created_time;
    public String article_id;
    public String stunum;
    public String nickname;
    public String photo_src;

    public static class AboutMeWapper extends RedRockApiWrapper<List<AboutMe>> {
    }
}
