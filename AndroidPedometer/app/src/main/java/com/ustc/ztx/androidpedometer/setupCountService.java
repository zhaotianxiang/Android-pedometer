package com.ustc.ztx.androidpedometer;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;

/**
 * 作者：ztx
 * 时间：2018/4/5:14:43
 */
public class setupCountService extends Service {
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
