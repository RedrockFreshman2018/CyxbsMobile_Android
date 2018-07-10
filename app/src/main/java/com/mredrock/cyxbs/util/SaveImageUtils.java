package com.mredrock.cyxbs.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import com.mredrock.cyxbs.MainApp;
import com.redrock.common.config.Config;
import com.mredrock.cyxbs.subscriber.SimpleObserver;
import com.mredrock.cyxbs.subscriber.SubscriberListener;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by wusui on 16/11/13.
 */

public class SaveImageUtils {
    public static void imageSave(final Bitmap bitmap,final String url,Context context){
        Observable.create(subscriber -> subscriber.onNext(bitmap)).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe((Consumer<? super Object>) new SimpleObserver<Bitmap>(context, new SubscriberListener<Bitmap>() {
                    @Override
                    public void onStart() {
                        super.onStart();
                    }

                    @Override
                     public void onComplete() {
                        super.onComplete();
                    }

                    @Override
                    public boolean onError(Throwable e) {
                        super.onError(e);
                        Log.e("SaveImageUtils",e.toString());
                        Toast.makeText(MainApp.getContext(),"图片保存失败",Toast.LENGTH_LONG).show();
                        return true;
                    }

                    @Override
                    public void onNext(Bitmap bitmap) {
                        super.onNext(bitmap);
                        File imageFile = new File(Environment.getExternalStorageDirectory() + Config.DIR_PHOTO);
                        if (!imageFile.exists())imageFile.mkdirs();
                        FileOutputStream outputStream = null;
                        Log.e("TAG",Environment.getExternalStorageDirectory().toString());

                        try {
                            String name = Environment.getExternalStorageDirectory() +Config.DIR_PHOTO+"/"+Utils.md5Hex(url)+ ".jpg";
                            File file = new File(name);
                            if (!file.exists()){
                                try {
                                    file.createNewFile();
                                }catch (IOException e){
                                    e.printStackTrace();
                                }
                            }
                            outputStream = new FileOutputStream(file);
                            //Bitmap image = bitmap.getDrawingCache();
                            bitmap.compress(Bitmap.CompressFormat.JPEG,100,outputStream);
                            outputStream.flush();
                            outputStream.close();
                            Toast.makeText(MainApp.getContext(),"图片成功保存至"+name+"目录",Toast.LENGTH_LONG).show();
                        }catch (IOException e){
                            e.printStackTrace();
                            onError(e);
                        }
                    }
                }));
    }
}
