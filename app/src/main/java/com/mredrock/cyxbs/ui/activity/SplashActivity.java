package com.mredrock.cyxbs.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.mredrock.cyxbs.R;
import com.mredrock.cyxbs.component.remind_service.RemindManager;
import com.mredrock.cyxbs.model.StartPage;
import com.mredrock.cyxbs.network.RequestManager;
import com.mredrock.cyxbs.subscriber.SimpleObserver;
import com.mredrock.cyxbs.subscriber.SubscriberListener;
import com.umeng.analytics.MobclickAgent;

import java.util.Timer;
import java.util.TimerTask;

import butterknife.BindView;
import butterknife.ButterKnife;

public class SplashActivity extends Activity {

    public static final String TAG = "SplashActivity";

    @BindView(R.id.iv_splash)
    ImageView mIvSplash;

    @Override
    public void onResume() {
        super.onResume();
        MobclickAgent.onResume(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        MobclickAgent.onPause(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        ButterKnife.bind(this);

        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                startActivity(new Intent(SplashActivity.this, MainActivity.class));
                SplashActivity.this.finish();
            }
        }, 1000);

        RemindManager.getInstance().pushAll(this);

        RequestManager.getInstance().getStartPage(new SimpleObserver<>(this, new SubscriberListener<StartPage>() {
            @Override
            public void onNext(StartPage startPage) {
                if (startPage != null) {
                    Glide.with(SplashActivity.this).load(startPage.getPhoto_src()).into(mIvSplash);
                }
            }
        }));
    }
}
