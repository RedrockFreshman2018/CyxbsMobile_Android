package com.redrock.common.network.interceptor;


import com.redrock.common.account.UserManager;
import com.redrock.common.config.Const;

import java.io.IOException;

import okhttp3.FormBody;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * A okhttp3 interceptor for new api version 20160805
 * Auto add optional parameters idNum & stuNum when user has login
 * Intercepted API:
 * {@link com.redrock.common.config.Const#API_SOCIAL_HOT_LIST}
 * {@link com.redrock.common.config.Const#API_SOCIAL_BBDD_LIST}
 * {@link com.redrock.common.config.Const#API_GET_PERSON_LATEST}
 * @author Haruue Icymoon haruue@caoyue.com.cn
 */

public class StudentNumberInterceptor implements Interceptor {
    @Override
    public Response intercept(Chain chain) throws IOException {
        String url = chain.request().url().toString();
        if (UserManager.isLogin() && (
                   url.equals(Const.END_POINT_RED_ROCK + Const.API_SOCIAL_HOT_LIST)
                || url.equals(Const.END_POINT_RED_ROCK + Const.API_SOCIAL_BBDD_LIST)
                || url.equals(Const.END_POINT_RED_ROCK + Const.API_GET_PERSON_LATEST)
                || url.equals(Const.END_POINT_RED_ROCK + Const.API_TREND_DETAIL)
                || url.equals(Const.END_POINT_RED_ROCK + Const.API_SOCIAL_OFFICIAL_NEWS_LIST))) {
            return doIntercept(chain);
        } else {
            return chain.proceed(chain.request());
        }
    }

    private Response doIntercept(Chain chain) throws IOException {
        Request originalRequest = chain.request();
        Request.Builder builder = originalRequest.newBuilder();
        RequestBody originalFormBody = originalRequest.body();
        if (originalFormBody instanceof FormBody) {
            FormBody.Builder newBuilder = new FormBody.Builder();
            for (int i = 0; i < ((FormBody) originalFormBody).size(); i++) {
                newBuilder.addEncoded(((FormBody) originalFormBody).encodedName(i), ((FormBody) originalFormBody).encodedValue(i));
            }
            // Add optional idNum and stuNum
            newBuilder.add("idNum", UserManager.getUser().idNum);
            newBuilder.add("stuNum", UserManager.getUser().stuNum);
            builder.method(originalRequest.method(), newBuilder.build());
        }
        return chain.proceed(builder.build());
    }

}
