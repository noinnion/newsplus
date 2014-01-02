package com.noinnion.android.newsplus.extension.feedbin;

import java.util.Calendar;

import android.content.Context;
import android.content.SharedPreferences;

import com.noinnion.android.reader.api.ExtensionPrefs;

public class Prefs extends ExtensionPrefs {

	public static final String	USER				= "username";
	public static final String	PASSWORD			= "password";
	public static final String	LAST_UPDATE			= "lastupdate";
	public static final String	LAST_UNREAD_ID		= "lastunreadid";

	public static final String	LOGGED_IN			= "logged_in";

	public static final String	SUBSCRIPTION_URL	= "https://api.feedbin.me/v2/subscriptions.json";
	public static final String	TAG_DELETE_URL		= "https://api.feedbin.me/v2/taggings/";
	public static final String	SUBSCRIPTION_UPDATE	= "https://api.feedbin.me/v2/subscriptions/";
	public static final String	FEED_URL			= "https://api.feedbin.me/v2/feeds/";
	public static final String	TAG_URL				= "https://api.feedbin.me/v2/taggings.json";
	public static final String	TAG_CREATE_URL		= "https://api.feedbin.me/v2/taggings.json";
	public static final String	UNREAD_ENTRIES_URL	= "https://api.feedbin.me/v2/unread_entries.json";
	public static final String	ENTRIES_URL			= "https://api.feedbin.me/v2/entries.json?ids=";
	public static final String	STARRED_URL			= "https://api.feedbin.me/v2/starred_entries.json";
	public static final String	STARRED_DELETE_URL	= "https://api.feedbin.me/v2/starred_entries/delete.json";

	// public static final String DELETE_UNREAD_ENTRY_URL="https://api.feedbin.me/v2/starred_entries/delete.json";

	public static boolean isLoggedIn(Context c) {
		return getBoolean(c, LOGGED_IN, false);
	}

	public static void setLoggedIn(Context c, boolean value) {
		putBoolean(c, LOGGED_IN, value);
	}

	public static void setLastUnreadId(Context c, int id) {
		putInt(c, LAST_UNREAD_ID, id);
	}

	public static int getLastUnreadId(Context c) {
		return getInt(c, LAST_UNREAD_ID, 0);
	}

	public static void removeLastUnreadId(Context c) {
		putInt(c, LAST_UNREAD_ID, 0);
	}

	public static void removeLoginData(Context c) {
		SharedPreferences sp = Prefs.getPrefs(c);
		SharedPreferences.Editor editor = sp.edit();
		editor.remove(LAST_UPDATE);
		editor.remove(USER);
		editor.remove(PASSWORD);
		editor.remove(LAST_UNREAD_ID);
		editor.commit();
	}

	public static void setLastUpdate(Context c, long date) {
		// 2013-02-02T14:07:33.000000Z
		Calendar ca = Calendar.getInstance();
		ca.setTimeInMillis(date);
		String d = numberConverter(ca.get(Calendar.YEAR)) + "-" + (numberConverter(ca.get(Calendar.MONTH) + 1)) + "-" + numberConverter(ca.get(Calendar.DAY_OF_MONTH)) + "T" + numberConverter(ca.get(Calendar.HOUR_OF_DAY)) + ":" + numberConverter(ca.get(Calendar.MINUTE)) + ":" + numberConverter(ca.get(Calendar.SECOND)) + ".000000Z";
		putString(c, LAST_UPDATE, d);
	}

	private static String numberConverter(int val) {
		if (val < 10) return "0" + val;
		return "" + val;
	}

	public static String getLastUpdate(Context c) {
		return getString(c, LAST_UPDATE, null);
	}

	public static String getUser(Context c) {
		SharedPreferences sp = Prefs.getPrefs(c);
		return sp.getString(USER, null);
	}

	public static String getPassword(Context c) {
		SharedPreferences sp = Prefs.getPrefs(c);
		return sp.getString(PASSWORD, null);
	}

	public static void setUserPasswd(Context c, String user, String password) {
		SharedPreferences sp = Prefs.getPrefs(c);
		SharedPreferences.Editor editor = sp.edit();
		editor.putString(USER, user);
		editor.putString(PASSWORD, password);
		editor.commit();
	}

	public static String getAuthURL() {
		return SUBSCRIPTION_URL;
	}
}
