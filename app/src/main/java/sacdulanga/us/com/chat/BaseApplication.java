package sacdulanga.us.com.chat;

import android.app.Application;
import android.content.SharedPreferences;

public class BaseApplication extends Application {

    private final String TAG = BaseApplication.this.getClass().getSimpleName();

    private static BaseApplication baseApplication;


    private SharedPreferences preferences;

    @Override
    public void onCreate() {
        super.onCreate();
        baseApplication = (BaseApplication) getApplicationContext();
        preferences = getSharedPreferences(getPackageName(), MODE_PRIVATE);
    }

    public BaseApplication() {
        super();
    }

    public static BaseApplication getBaseApplication() {
        return baseApplication;
    }
}