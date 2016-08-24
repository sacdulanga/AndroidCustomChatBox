package ibm.us.com.chat.common;

import android.content.Context;
import android.content.CursorLoader;
import android.database.Cursor;
import android.graphics.Point;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Display;
import android.view.WindowManager;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import ibm.us.com.chat.BaseApplication;

public class CommonUtils {

    private static CommonUtils instance = null;
    private static String pinCode = "";
    private static boolean isPinCodeSet = false;
    private static final int REQUEST_CODE_ENABLE = 11;

    private static final int SECOND_MILLIS = 1000;
    private static final int MINUTE_MILLIS = 60 * SECOND_MILLIS;
    private static final int HOUR_MILLIS = 60 * MINUTE_MILLIS;
    private static final int DAY_MILLIS = 24 * HOUR_MILLIS;

    private CommonUtils() {}

    public static CommonUtils getInstance() {
        if (instance == null) instance = new CommonUtils();
        return instance;
    }

    public boolean isNetworkConnected() {
        ConnectivityManager cm = (ConnectivityManager) BaseApplication.getBaseApplication().getSystemService(Context.CONNECTIVITY_SERVICE);
        return cm.getActiveNetworkInfo() != null;
    }

    public SimpleDateFormat getDateTimeFormatter() {
        return new SimpleDateFormat(ApplicationConstants.DEFAULT_DATE_AND_TIME_FORMAT, Locale.getDefault());
    }

    public Date getCurrentDateAndTime() throws ParseException {
        SimpleDateFormat fmtDateAndTime = getDateTimeFormatter();
        return fmtDateAndTime.parse(fmtDateAndTime.format(Calendar.getInstance().getTime()));
    }

    public String getCurrentDateAndTimeString() {
        SimpleDateFormat fmtDateAndTime = getDateTimeFormatter();
        return fmtDateAndTime.format(Calendar.getInstance().getTime());
    }

    public Date formatDate(String date) throws ParseException {
        SimpleDateFormat fmtDateAndTime = getDateTimeFormatter();
        return fmtDateAndTime.parse(date);
    }

    public Date formatDate(String date, SimpleDateFormat simpleDateFormat) throws ParseException {
        return simpleDateFormat.parse(date);
    }

    public int[] calculateScreenDimens() {
        WindowManager windowManager = (WindowManager) BaseApplication.getBaseApplication().getSystemService(Context.WINDOW_SERVICE);
        Display display = windowManager.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);

        int dimension[] = new int[2];
        dimension[0] = size.x;  //width
        dimension[1] = size.y;  //height

        return dimension;
    }

    public float dpToPx(float valueInDp) {
        try {
            DisplayMetrics metrics = BaseApplication.getBaseApplication().getResources().getDisplayMetrics();
            return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, valueInDp, metrics);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return 0.0f;
    }

    public String getPathFromUri(Context context, Uri uri) {
        Cursor cursor = null;
        try {
        String[] data = {MediaStore.Images.Media.DATA};
        CursorLoader loader = new CursorLoader(context, uri, data, null, null, null);
        cursor = loader.loadInBackground();
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        return cursor.getString(column_index);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public static String getPinCode() {
        return pinCode;
    }

    public static void setPinCode(String pinCode) {
        CommonUtils.pinCode = pinCode;
    }

    public static boolean isPinCodeSet() {
        return isPinCodeSet;
    }

    public static void setIsPinCodeSet(boolean isPinCodeSet) {
        CommonUtils.isPinCodeSet = isPinCodeSet;
    }

    public static int getRequestCodeEnable() {
        return REQUEST_CODE_ENABLE;
    }

    public static String formatDateFromString(String inputFormat, String outputFormat, String inputDate) {

        Date parsed = null;
        String outputDate = "";

        SimpleDateFormat df_input = new SimpleDateFormat(inputFormat, java.util.Locale.getDefault());
        SimpleDateFormat df_output = new SimpleDateFormat(outputFormat, java.util.Locale.getDefault());

        try {
            parsed = df_input.parse(inputDate);
            outputDate = df_output.format(parsed);
        } catch (ParseException e) {}
        return outputDate;
    }

    public String getTimeAgo(String strTime) {
        try {
            long time = formatDate(strTime).getTime();
            if (time < 1000000000000L) {
                time *= 1000; // if timestamp given in seconds, convert to millis
            }

            long now = getCurrentUTCTime().getTime();
            if (time > now || time <= 0) {
                return null;
            }

            // TODO: localize
            final long diff = now - time;
            if (diff < MINUTE_MILLIS) {
                return "now";
            } else if (diff < 2 * MINUTE_MILLIS) {
                return "1 min ago";
            } else if (diff < 60 * MINUTE_MILLIS) {
                return diff / MINUTE_MILLIS + " mins ago";
            } else if (diff < 120 * MINUTE_MILLIS) {
                return "1 hour ago";
            } else if (diff < 24 * HOUR_MILLIS) {
                return diff / HOUR_MILLIS + " hours ago";
            } else {
                return getOACExtraDateString(strTime);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return strTime;
    }

    public String getOACExtraDateString(String createdDate) {
        String timeStamp = "";
        try {
            String suffix;

            Date date = convertMsgSendTimeToDefault(createdDate);
            Calendar cal = Calendar.getInstance();
            cal.setTime(date);

            int dayOFMonth = cal.get(Calendar.DAY_OF_MONTH);
            if (dayOFMonth > 10 && dayOFMonth < 20) {
                suffix = "<sup><small>th</small></sup>";
            } else {
                switch (dayOFMonth % 10) {
                    case 1:
                        suffix = "<sup><small>st</small></sup>";
                        break;
                    case 2:
                        suffix = "<sup><small>nd</small></sup>";
                        break;
                    case 3:
                        suffix = "<sup><small>rd</small></sup>";
                        break;
                    default:
                        suffix = "<sup><small>th</small></sup>";
                        break;
                }
            }

            String dateFormat = "dd'" + suffix + "' MMM yyyy, hh:mm aa";
            String formattedDate = new SimpleDateFormat(dateFormat).format(date);

            timeStamp = formattedDate.replace(" AM", "am").replace(" PM","pm");
        } catch (Exception e) {
            e.printStackTrace();
        }

        return timeStamp;
    }

    private Date getCurrentUTCTime() throws ParseException {
        String fromTimeZone = "UTC";
        SimpleDateFormat dateFormatGmt = getDateTimeFormatter();
        dateFormatGmt.setTimeZone(TimeZone.getTimeZone(fromTimeZone));
        return formatDate(dateFormatGmt.format(new Date()));
    }

    private Date convertMsgSendTimeToDefault(String sendTime) throws ParseException {
        SimpleDateFormat sourceFormatUtc = getDateTimeFormatter();
        sourceFormatUtc.setTimeZone(TimeZone.getTimeZone("UTC"));
        Date utcDate = sourceFormatUtc.parse(sendTime);

        SimpleDateFormat dateFormatDefault = getDateTimeFormatter();
        dateFormatDefault.setTimeZone(TimeZone.getTimeZone(TimeZone.getDefault().getID()));
        return formatDate(dateFormatDefault.format(utcDate));
    }
}
