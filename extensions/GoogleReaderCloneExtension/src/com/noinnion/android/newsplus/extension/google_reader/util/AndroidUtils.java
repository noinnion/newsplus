package com.noinnion.android.newsplus.extension.google_reader.util;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

/**
 * Static helper utilities for android related tasks (<b>Beta</b>).
 * 
 * @author Viktor Reiser &lt;<a href="mailto:viktorreiser@gmx.de">viktorreiser@gmx.de</a>&gt;
 */
public class AndroidUtils {

	// PUBLIC =====================================================================================

	public static int getBuildVersion() {
		//		return Integer.parseInt(Build.VERSION.SDK);
		return Build.VERSION.SDK_INT;
	}

	public static String getVersionName(Context c) {
		try {
			PackageInfo info = c.getPackageManager().getPackageInfo(c.getPackageName(), 0);
			return info.versionName;
		} catch (NameNotFoundException e) {}
		return null;
	}	

	public static int getVersionCode(Context context) {
		try {
			PackageInfo info = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
			return info.versionCode;
		} catch (PackageManager.NameNotFoundException e) {}
		return -1;
	}

	public static boolean isDebuggable(Context c) {
		return ( 0 != ( c.getApplicationInfo().flags &= ApplicationInfo.FLAG_DEBUGGABLE ) );
	}

	public static void onError(String tag, Throwable e) {
		Log.e(tag,  e.getClass().getSimpleName() + ": " + e.getLocalizedMessage());
	}
	
	public static void onError(String tag, String message) {
		Log.e(tag,  message);
	}	

	public static void printCurrentStackTrace() {
		for (StackTraceElement ste : Thread.currentThread().getStackTrace()) {
		    Log.i("DEBUG", ste.toString());
		}
	}
	
	public static boolean isFroyo() {
		// Can use static final constants like FROYO, declared in later versions
		// of the OS since they are inlined at compile time. This is guaranteed behavior.
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO;
	}
	
	public static boolean isGingerBread() {
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD;
	}    

	public static boolean isHoneycomb() {
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB;
	}
	
	public static boolean isHoneycombMR2() {
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2;
	}

	public static boolean isJellyBean() {
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN;
	}


	public static void showToast(final Context c, final CharSequence text) {
		new Handler(Looper.getMainLooper()).post(new Runnable() {
			public void run() {
				if (c == null) return;
				Toast.makeText(c, text, Toast.LENGTH_SHORT).show();
			}
		});
	}
	
	public static void showToast(final Context c, int resId) {
		String text = c.getString(resId);
		showToast(c, text);
	}	
	

		

	
	// PRIVATE ====================================================================================

	/**
	 * No constructor for a static class.
	 */
	private AndroidUtils() {

	}
}
