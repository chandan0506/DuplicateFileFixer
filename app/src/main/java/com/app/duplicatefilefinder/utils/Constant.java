package com.app.duplicatefilefinder.utils;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.regex.Pattern;


public class Constant {


    private static final boolean SHOW_LOG_TAG = true;

    private static final String SCAN_TAG = "scan";
    public static final String errorString = "Something went wrong, Please try again.";
    public static final String network_error = "No network connection.";

    private static SimpleDateFormat dateFormater;
    private static SimpleDateFormat dateFormaterOne, dateFormatTwo;

    public static boolean isNotEmptyOrNull(String str) {
        return (str != null && !str.isEmpty());
    }


   /* public static SharedPreferences createPreference(Context context, String prefName) {

        SharedPreferences mSharedPreferences = context.getSharedPreferences(prefName, 0);
        return mSharedPreferences;
    }*/


    public static void savePrefBolVal(SharedPreferences mSharedPreferences, String key, boolean value) {
        if (mSharedPreferences != null)
            mSharedPreferences.edit().putBoolean(key, value).apply();
    }

    public static boolean getPrefBolVal(SharedPreferences sp, String key) {
        return sp.getBoolean(key, false);
    }


    public static void savePrefStrVal(SharedPreferences mSharedPreferences, String key, String value) {
        if (mSharedPreferences != null)
            mSharedPreferences.edit().putString(key, value).apply();
    }

    public static String getPrefStrVal(SharedPreferences sp, String key) {
        return sp.getString(key, "");
    }

    public static void clearPreferences(SharedPreferences mSharedPreferences) {
        if (mSharedPreferences != null)
            mSharedPreferences.edit().clear().apply();
    }

    public static ProgressDialog getProgressDialog(Context context, String msg) {
        ProgressDialog progressDialog = new ProgressDialog(context);
        progressDialog.setMessage(msg);
        progressDialog.setCancelable(false);
        return progressDialog;
    }


    static {
        dateFormater = new SimpleDateFormat("h:mm a");
        // dateFormater.setTimeZone(TimeZone.getTimeZone("UTC"));
        dateFormaterOne = new SimpleDateFormat("EEE, d MMM yyyy  h:mm a");

        dateFormaterOne = new SimpleDateFormat("EEE, d MMM yyyy");
        // dateFormaterOne.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    public static void sop(String msg) {
        if (msg != null) {
            System.out.println(msg);
        }
    }

    public static boolean whatsAppInstalledOrNot(Activity activity) {
        PackageManager pm = activity.getPackageManager();
        boolean app_installed;
        try {
            pm.getPackageInfo("com.whatsapp", PackageManager.GET_ACTIVITIES);
            app_installed = true;
        } catch (PackageManager.NameNotFoundException e) {
            app_installed = false;
        }
        return app_installed;
    }

    public static int getNumberOfCores() {
        if (Build.VERSION.SDK_INT >= 17) {
            return Runtime.getRuntime().availableProcessors();
        } else {
            return getNumCoresOldPhones();
        }
    }

    /**
     * Gets the number of cores available in this device, across all processors.
     * Requires: Ability to peruse the filesystem at "/sys/devices/system/cpu"
     *
     * @return The number of cores, or 1 if failed to get result
     */
    private static int getNumCoresOldPhones() {
        //Private Class to display only CPU devices in the directory listing
        class CpuFilter implements FileFilter {
            @Override
            public boolean accept(File pathname) {
                //Check if filename is "cpu", followed by a single digit number
                return Pattern.matches("cpu[0-9]+", pathname.getName());
            }
        }

        try {
            //Get directory containing CPU info
            File dir = new File("/sys/devices/system/cpu/");
            //Filter to only list the devices we care about
            File[] files = dir.listFiles(new CpuFilter());
            //Return the number of cores (virtual CPU devices)
            return files.length;
        } catch (Exception e) {
            //Default to return 1 core
            return 1;
        }
    }


    public static byte[] fileHash(File file) {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("MD5");
            if (file.isFile() && file.exists()) {
                FileInputStream fileinputstream = new FileInputStream(file);
                byte[] dataBytes = new byte[1024];
                int nread = 0;
                while ((nread = fileinputstream.read(dataBytes)) != -1) {
                    md.update(dataBytes, 0, nread);
                }
                fileinputstream.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return md.digest();
    }



    @NonNull
    public static String getStringSizeLengthFile(long size) {

        DecimalFormat df = new DecimalFormat("0.00");

        float sizeKb = 1024.0f;
        float sizeMo = sizeKb * sizeKb;
        float sizeGo = sizeMo * sizeKb;
        float sizeTerra = sizeGo * sizeKb;

        if (size < sizeKb)
            return df.format(size) + " Bytes";
        else if (size < sizeMo)
            return df.format(size / sizeKb) + " KB";
        else if (size < sizeGo)
            return df.format(size / sizeMo) + " MB";
        else if (size < sizeTerra)
            return df.format(size / sizeGo) + " GB";

        return "0 Bytes";
    }

    public static void toastShort(Context context, String msg) {
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
    }

    public static void toastLong(Context context, String msg) {
        Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
    }


    private static void myLogs(String logTag, String msg) {
        if (SHOW_LOG_TAG) {
            Log.d(logTag, msg);
        }
    }

    public static String getFormattedDate(long date) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
        String currentDate = sdf.format(date);
        return currentDate;
    }


    public static String getDaysBetweenDates(long date1, long date2) {
        Constant.myLogs(SCAN_TAG, "Previous Time : " + date1 + " Current Time : " + date2);
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
        String a = sdf.format(date1);
        String b = sdf.format(date2);
        Date d1 = null;
        Date d2 = null;
        String remaining = "";
        try {
            d1 = sdf.parse(a);
            d2 = sdf.parse(b);
            //in milliseconds
            long diff = d2.getTime() - d1.getTime();

            long diffSeconds = diff / 1000 % 60;
            long diffMinutes = diff / (60 * 1000) % 60;
            long diffHours = diff / (60 * 60 * 1000) % 24;
            long diffDays = diff / (24 * 60 * 60 * 1000);
            if (diffDays > 0) {
                remaining = diffDays + " Days Remaining";
            } else {
                remaining = "0 Days Remaining";
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
        return remaining;
    }

    public static int daysBetween(Date d1, Date d2) {
        return (int) ((d2.getTime() - d1.getTime()) / (1000 * 60 * 60 * 24));
    }

    public static String getFormattedYear(long timestamp) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy");
        dateFormat.setTimeZone(TimeZone.getDefault());
        return " " + dateFormat.format(new Date(timestamp));
    }


    public static String getLastScanTime(String date1, String date2) {
        Constant.myLogs(SCAN_TAG, "Previous Time : " + date1 + " Current Time : " + date2);
        SimpleDateFormat format = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
        Date d1 = null;
        Date d2 = null;
        String lastScan = "";
        try {
            d1 = format.parse(date1);
            d2 = format.parse(date2);

            //in milliseconds
            long diff = d2.getTime() - d1.getTime();

            long diffSeconds = diff / 1000 % 60;
            long diffMinutes = diff / (60 * 1000) % 60;
            long diffHours = diff / (60 * 60 * 1000) % 24;
            long diffDays = diff / (24 * 60 * 60 * 1000);

            System.out.print(diffDays + " days, ");
            System.out.print(diffHours + " hours, ");
            System.out.print(diffMinutes + " minutes, ");
            System.out.print(diffSeconds + " seconds.");
            if (diffDays <= 0 && diffHours <= 0 && diffMinutes <= 0) {
                lastScan = diffSeconds + " seconds ago";
            } else if (diffDays <= 0 && diffHours <= 0) {
                lastScan = "" + diffMinutes + " minutes ago";
            } else if (diffDays <= 0 && diffHours >= 0) {
                lastScan = "" + diffHours + "  hours ago";
            } else if (diffDays > 0) {
                lastScan = diffDays + " days ago";
            }

        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
        return lastScan;
    }


    public static String sentenceCase(String s) {
        String sString = s.toLowerCase();
        sString = Character.toString(sString.charAt(0)).toUpperCase() + sString.substring(1);
        return sString;
    }

    public static String getFormatedTime(long timestamp) {

        return " " + dateFormater.format(new Date(timestamp));
    }

    public static String getFormatedDateTime(long timestamp) {

        return " " + dateFormaterOne.format(new Date(timestamp));
    }

    public static String getFormatedDateOnly(long timestamp) {

        return " " + dateFormatTwo.format(new Date(timestamp));
    }


    public static int getVersionCode(Context context) {
        PackageInfo pInfo;
        try {
            pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return pInfo.versionCode;
        } catch (Exception e) {
            return 0;
        }
    }

    public static String getVersionName(Context context) {
        PackageInfo pInfo;
        try {
            pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            String vc = pInfo.versionName;
            return "Version " + vc;
        } catch (Exception e) {
            return "";
        }
    }


    public static boolean isNetworkConnected(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo ni = cm.getActiveNetworkInfo();
        if (ni == null) {
            // There are no active networks.
            return false;
        } else
            return true;
    }


    public static String getDeviceName() {
        String manufacturer = Build.MANUFACTURER;
        String model = Build.MODEL;
        if (model.startsWith(manufacturer)) {
            return capitalize(model);
        } else {
            return capitalize(manufacturer) + " " + model;
        }
    }

    private static String capitalize(String s) {
        if (s == null || s.length() == 0) {
            return "";
        }
        char first = s.charAt(0);
        if (Character.isUpperCase(first)) {
            return s;
        } else {
            return Character.toUpperCase(first) + s.substring(1);
        }
    }


    public static int androidVersion() {
        return Build.VERSION.SDK_INT;
    }

    public static void displayInfoToast(Context context, Toast mLockedToast, String msg) {
        if (mLockedToast != null) {
            mLockedToast.cancel();
        }

        mLockedToast = Toast.makeText(context, msg, Toast.LENGTH_SHORT);
        TextView v = (TextView) mLockedToast.getView().findViewById(android.R.id.message);
        if (v != null) v.setGravity(Gravity.CENTER);
        mLockedToast.show();
    }


    public static void hideSoftKeyboard(AppCompatActivity activity) {
        if (activity.getCurrentFocus() != null) {
            InputMethodManager inputMethodManager = (InputMethodManager) activity.getSystemService(activity.INPUT_METHOD_SERVICE);
            inputMethodManager.hideSoftInputFromWindow(activity.getCurrentFocus().getWindowToken(), 0);
        }
    }

    public static boolean checkInternetConnection(Context context) {
        ConnectivityManager connectivity = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivity == null) {
            return false;
        } else {
            NetworkInfo[] info = connectivity.getAllNetworkInfo();
            if (info != null) {
                for (int i = 0; i < info.length; i++) {
                    if (info[i].getState() == NetworkInfo.State.CONNECTED) {
                        return true;
                    }
                }
            }
        }
        return false;
    }


}
