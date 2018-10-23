package me.xuzhi.aria2cdroid;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.blankj.utilcode.util.FileIOUtils;

import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.io.InputStream;

/**
 * Created by xuzhi on 2018/3/12.
 */

public class Aria2Service extends Service {

    private boolean running;

    public boolean isRunning() {
        return running;
    }

    private File fileAria2c;

    private AriaConfig ariaConfig;

    private BinExecuter binExecuter;

    private static final String TAG = "Aria2Service";

    public static final String ARIA2_SERVICE_START_SUCCESS = "ARIA2_SERVICE_START_SUCCESS";

    public static final String ARIA2_SERVICE_START_FAIL = "ARIA2_SERVICE_START_FAIL";

    public static final String ARIA2_SERVICE_START_STOPPED = "ARIA2_SERVICE_START_STOPPED";

    public static final String ARIA2_SERVICE_BIN_CONSOLE = "ARIA2_SERVICE_BIN_CONSOLE";

    public class MyBinder extends Binder {

        public Aria2Service getService() {
            return Aria2Service.this;
        }
    }

    private MyBinder binder = new MyBinder();

    @Override
    public void onCreate() {
        super.onCreate();
        fileAria2c = new File(getFilesDir(), "aria2c");
        if (!fileAria2c.exists()) {
            try {
                InputStream ins = getResources().openRawResource(R.raw.aria2c);
                FileIOUtils.writeFileFromIS(fileAria2c, ins);
                Runtime.getRuntime().exec("chmod 777 " + fileAria2c.getAbsolutePath());
            } catch (Exception e) {
                Log.e(TAG, "onCreate: ", e);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stop();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public void start() {
        if (binExecuter == null) return;
        binExecuter.start();
        if (binExecuter.getPid() > 0) {
            sendMessage(ARIA2_SERVICE_START_SUCCESS, String.valueOf(binExecuter.getPid()));
            running = true;
        } else {
            sendMessage(ARIA2_SERVICE_START_FAIL, String.valueOf(0));
            running = false;
        }
    }

    public void stop() {
        if (binExecuter == null) return;
        binExecuter.stop();
        sendMessage(ARIA2_SERVICE_START_STOPPED, String.valueOf(0));
        running = false;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            ariaConfig = (AriaConfig) intent.getSerializableExtra("config");
            if (ariaConfig != null) {
                Log.d(TAG, ariaConfig.toString());
                binExecuter = new BinExecuter(fileAria2c.getAbsolutePath(), ariaConfig.toString());
                binExecuter.setBinExecuteCallback(new BinExecuter.BinExecuteCallback() {
                    @Override
                    public void onConsoleResponse(String text) {
                        sendMessage(ARIA2_SERVICE_BIN_CONSOLE, text);
                    }
                });
            }
        } else {
            stopSelf();
        }
        return super.onStartCommand(intent, flags, startId);
    }

    private void sendMessage(String name, String message) {
        MessageEvent genericEvent = new MessageEvent(name, message);
        EventBus.getDefault().post(genericEvent);
    }

}
