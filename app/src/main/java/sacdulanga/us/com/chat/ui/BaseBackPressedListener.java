package sacdulanga.us.com.chat.ui;

import android.support.v4.app.FragmentActivity;

/**
 * Created by kosala_m on 8/5/2016.
 */
public class BaseBackPressedListener {
    private final FragmentActivity activity;

    public BaseBackPressedListener(FragmentActivity activity) {
        this.activity = activity;
    }

    /** fragment on back pressed interface */
    public interface OnBackPressedListener {
        void doBack();
    }
}
