package com.mredrock.cyxbs.component.remind_service.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.coolerfall.daemon.Daemon;
import com.mredrock.cyxbs.component.remind_service.Reminder;
import com.mredrock.cyxbs.component.remind_service.receiver.RemindReceiver;
import com.redrock.common.util.TimeUtils;

import java.util.ArrayList;

import static android.app.PendingIntent.FLAG_UPDATE_CURRENT;
import static com.mredrock.cyxbs.component.remind_service.RemindManager.EXTRA_REMINDABLE;
import static com.mredrock.cyxbs.component.remind_service.RemindManager.INTENT_FLAG;
import static com.mredrock.cyxbs.component.remind_service.RemindManager.INTENT_FLAG_PUSH;

/**
 * Created by simonla on 2016/11/2.
 * 下午8:42
 */

public class NotificationService extends Service {

    public static final String TAG = NotificationService.class.getSimpleName();

    public static final String EXTRA_NOTIFY_TITLE = "remind_title";
    public static final String EXTRA_NOTIFY_SUBTITLE = "remind_sub_title";

    private AlarmManager mAlarmManager;

    @Override
    public void onCreate() {
        super.onCreate();
        Daemon();
    }

    //加入守护线程保证服务存活
    private void Daemon() {
        Daemon.run(this, NotificationService.class, Daemon.INTERVAL_ONE_MINUTE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "remind service living");
        mAlarmManager = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
        if (intent != null) {
            ArrayList<Reminder> reminders = intent.getParcelableArrayListExtra(EXTRA_REMINDABLE);
            if (reminders != null && reminders.size() != 0) {
                int flag = intent.getIntExtra(INTENT_FLAG, INTENT_FLAG_PUSH);
                for (Reminder r : reminders) {
                    setAlarm(r, flag);
                }
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    private void setAlarm(Reminder reminder, int flag) {
        Intent intent = new Intent(this, RemindReceiver.class);
        intent.putExtra(EXTRA_NOTIFY_TITLE, reminder.getTitle());
        intent.putExtra(EXTRA_NOTIFY_SUBTITLE, reminder.getSubTitle());
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, reminder.hashCode()
                , intent, FLAG_UPDATE_CURRENT);
        if (flag == INTENT_FLAG_PUSH) {
            push(reminder, pendingIntent);
        } else {
            cancel(reminder, pendingIntent);
        }
    }

    private void cancel(Reminder reminder, PendingIntent pendingIntent) {
        mAlarmManager.cancel(pendingIntent);
        Log.d(TAG, "提醒已取消:" + TimeUtils.timeStampToStr(reminder.getCalendar().
                getTimeInMillis() / 1000) + " title： " + reminder.getTitle() + " subTitle: " + reminder.getSubTitle());
    }

    private void push(Reminder reminder, PendingIntent pendingIntent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            //忽略Doze模式
            mAlarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminder.getCalendar().
                    getTimeInMillis(), pendingIntent);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            mAlarmManager.setExact(AlarmManager.RTC_WAKEUP, reminder.getCalendar().
                    getTimeInMillis(), pendingIntent);
        } else {
            mAlarmManager.set(AlarmManager.RTC_WAKEUP, reminder.getCalendar().
                    getTimeInMillis(), pendingIntent);
        }
        Log.d(TAG, "提醒已设置:" + TimeUtils.timeStampToStr(reminder.getCalendar().
                getTimeInMillis() / 1000) + " title： " + reminder.getTitle() + " subTitle: " + reminder.getSubTitle());
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
