package so.ldd.linnotify;

import android.app.Application;

public class LinNotifyApplication extends Application {
    private static LinNotifyApplication sInstance;

    public static Application getInstance() {
        return sInstance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        sInstance = this;
    }
}
