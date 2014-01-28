package com.noinnion.android.newsplus.extension.readability;

import java.util.ArrayList;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.noinnion.android.reader.api.ExtensionPrefs;

public class Prefs extends ExtensionPrefs {

	public static final String USER="username";
	public static final String PASSWORD="password";
	public static final String LAST_UPDATE="lastupdate";
	public static final String ALL_IDS="allids";
	
	public static final String	LOGGED_IN				= "logged_in";
	
	public static final String TOKEN="2383cd9146f253585656026c04db230efbb9d764";
	public static final String KEY="ApiProgger";
	public static final String SECRET="CFVX5Yk2vGcAMshWJC25f9ef7HPqEXxQ";
	
	
	public static final String AUTHORIZE_URL="https://www.readability.com/api/rest/v1/oauth/access_token/";
	public static final String OAUTH_TOKEN_SECRET="oauthtokensecret";
	public static final String OAUTH_TOKEN="oauthtoken";

	public static void removeALLItemIDs(Context c)
	{
		putString(c, ALL_IDS, "");
	}
	public static void addItemID(Context c,int id)
	{
		String s=getString(c, ALL_IDS);
		if(s==null||s.equals(""))
			s=id+"";
		else
			s=s+";"+id;
		putString(c, ALL_IDS, s);
	}
	public static java.util.List<Integer> getAllItemIDs(Context c)
	{
		String str=getString(c, ALL_IDS);
		if(str==null||str.equals(""))
			return new ArrayList<Integer>();
		String[]s=str.split(";");
		ArrayList<Integer>list=new ArrayList<Integer>(s.length);
		for(String sp:s)
			list.add(new Integer(sp));
		return list;
	}
	
	public static boolean isLoggedIn(Context c) 
	{
		return getBoolean(c, LOGGED_IN, false);
	}

	public static void setLoggedIn(Context c, boolean value) {
		putBoolean(c, LOGGED_IN, value);
	}
	public static void setOAuth(Context c,String oauthtokensecret,String oauthtoken)
	{
		putString(c, OAUTH_TOKEN, oauthtoken);
		putString(c, OAUTH_TOKEN_SECRET, oauthtokensecret);
	}
	public static String getOAuthToken(Context c)
	{
		return getString(c, OAUTH_TOKEN);
	}
	public static String getOAuthTokenSecret(Context c)
	{
		return getString(c, OAUTH_TOKEN_SECRET);
	}
	
	public static void removeLoginData(Context c)
	{
		SharedPreferences sp = Prefs.getPrefs(c);
		SharedPreferences.Editor editor = sp.edit();
		editor.remove(LAST_UPDATE);
		editor.remove(USER);
		editor.remove(PASSWORD);
		editor.remove(OAUTH_TOKEN);
		editor.remove(OAUTH_TOKEN_SECRET);
		editor.remove(ALL_IDS);
		editor.commit();
	}
	public static void setLastUpdate(Context c,String date)
	{
		putString(c, LAST_UPDATE, date);
	}
	public static String getLastUpdate(Context c)
	{
		return getString(c, LAST_UPDATE,null);
	}

	public static String getUser(Context c)
	{
		SharedPreferences sp = Prefs.getPrefs(c);
		return sp.getString(USER, null);
	}
	public static String getPassword(Context c)
	{
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
}
