package com.noinnion.android.newsplus.extension.google_reader;

import java.io.BufferedReader;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.json.JSONObject;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerFuture;
import android.content.Context;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.noinnion.android.newsplus.extension.google_reader.util.HtmlUtils;
import com.noinnion.android.newsplus.extension.google_reader.util.HttpUtils;
import com.noinnion.android.newsplus.extension.google_reader.util.UrlUtils;
import com.noinnion.android.newsplus.extension.google_reader.util.Utils;
import com.noinnion.android.newsplus.extension.google_reader.util.compat.AccountManagerHelper;
import com.noinnion.android.reader.api.ReaderException;
import com.noinnion.android.reader.api.ReaderException.ReaderLoginException;
import com.noinnion.android.reader.api.ReaderExtension;
import com.noinnion.android.reader.api.internal.IItemIdListHandler;
import com.noinnion.android.reader.api.internal.IItemListHandler;
import com.noinnion.android.reader.api.internal.ISubscriptionListHandler;
import com.noinnion.android.reader.api.internal.ITagListHandler;
import com.noinnion.android.reader.api.provider.IItem;
import com.noinnion.android.reader.api.provider.ISubscription;
import com.noinnion.android.reader.api.provider.ITag;

public class GoogleReaderClient extends ReaderExtension {

	public static final String	TAG						= "GoogleReaderClient";
	
	public static final String	URL_LOGIN						= "https://www.google.com/accounts/ClientLogin";
	public static final String	URL_BASE_						= "https://www.google.com/reader";
	public static final String	URL_API_						= URL_BASE_ + "/api/0";
	public static final String	URL_ATOM_						= URL_BASE_ + "/atom";
	public static final String	URL_API_TOKEN					= URL_API_ + "/token";
	public static final String	URL_API_SUB_LIST				= URL_API_ + "/subscription/list";
	public static final String	URL_API_TAG_LIST				= URL_API_ + "/tag/list";
	public static final String	URL_API_RECOMMENDATION_LIST		= URL_API_ + "/recommendation/list";
	public static final String	URL_API_UNREAD_COUNT			= URL_API_ + "/unread-count";
	public static final String	URL_API_STREAM_CONTENTS			= URL_API_ + "/stream/contents";
	public static final String	URL_API_STREAM_ITEM_IDS			= URL_API_ + "/stream/items/ids";
	public static final String	URL_API_STREAM_ITEMS_CONTENTS	= URL_API_ + "/stream/items/contents";
	public static final String	URL_API_FEED_FINDER				= URL_API_ + "/feed-finder?output=json";
	public static final String	URL_API_EDIT_TAG				= URL_API_ + "/edit-tag?client=scroll";
	public static final String	URL_API_EDIT_ITEM				= URL_API_ + "/item/edit?client=scroll";
	public static final String	URL_API_RENAME_TAG				= URL_API_ + "/rename-tag?client=scroll";
	public static final String	URL_API_DISABLE_TAG				= URL_API_ + "/disable-tag?client=scroll";
	public static final String	URL_API_MARK_ALL_AS_READ		= URL_API_ + "/mark-all-as-read?client=scroll";
	public static final String	URL_API_SUBSCIPTION				= URL_API_ + "/subscription/edit?client=scroll";
	public static final String	URL_API_BUNDLES					= URL_API_ + "/bundles?output=json";
	public static final String	URL_API_PREFERENCE_LIST			= URL_API_ + "/preference/list?output=json";
	public static final String	URL_API_PREFERENCE_STREAM_LIST	= URL_API_ + "/preference/stream/list?output=json";

	private static final int	SYNC_MAX_ITEMS					= 1000;											// 1000

	//	private static final String	TAG							= "GoogleReaderClient";
	private static final long	TOKEN_TIME						= 15 * 60 * 1000;								// 15 min
	private static final long	AUTH_EXPIRE_TIME				= 3 * 86400 * 1000;								// 3 days
	private static final long	OAUTH_EXPIRE_TIME				= 3540 * 1000;									// 59 min

	private String				loginId;
	private String				password;
	private String				auth;
	private String				token;
	private long				tokenExpiredTime;

	protected DefaultHttpClient	client;

	public DefaultHttpClient getClient() {
		if (client == null) client = HttpUtils.createHttpClient();
		return client;
	}

	public String unEscapeEntities(String text) {
		if (text == null) return "";
		return text.replace("&amp;", "&").replace("&#39;", "'").replace("&quot;", "\"");
		//		return text.replace("&lt;", "<").replace("&gt;", ">").replace("&amp;", "&").replace("&#39;", "'").replace("&quot;", "\"");
	}		
	
	// tag:google.com,2005:reader/item/ac2817bda074995e => ac2817bda074995e
	public static String stripItemUid(String uid) {
		if (uid == null) return "";
		return uid.replaceFirst("tag:google.com,2005:reader/item/", "");
	}

	// tag:google.com,2005:reader/item/ac2817bda074995e => ac2817bda074995e
	public static String getItemUid(String id) {
		if (id == null) return "";
		return id.startsWith("tag") ? id : "tag:google.com,2005:reader/item/" + id;
	}
	
	public static String dec2Hex(String dec) {
		if (dec == null) return null;
		long n = new BigInteger(dec).longValue();
		String hex = Long.toHexString(n);
		// leading zeros for 16 digit
		while (hex.length() < 16) {
			hex = "0" + hex;
		}
		return hex;
	}
	
	public static String getTagUIdPath(String uid) {
		int pos = uid.indexOf("/label/");
		return uid.substring(0, pos + "/label/".length());
	}
	
	
	public java.io.Reader doPostReader(String url, List<NameValuePair> params) throws IOException, ReaderException {
		return new InputStreamReader(doPostInputStream(url, params), HTTP.UTF_8);
	}

	public InputStream doPostInputStream(String url, List<NameValuePair> params) throws IOException, ReaderException {
		// Log.d(TAG, "[DEBUG] POST: " + url);
		// Log.d(TAG, "[DEBUG] PARAMS: " + params);
		HttpPost post = new HttpPost(url);
		post.setEntity(new UrlEncodedFormEntity(params, HTTP.UTF_8));

		final Context context = getApplicationContext();
		int login = Prefs.getLoginMethod(context);
		if (login == Prefs.PREF_LOGIN_CLIENT || login == Prefs.PREF_LOGIN_ACCOUNT) {
			if (this.auth != null) post.setHeader("Authorization", "GoogleLogin auth=" + this.auth);
		} else if (login == Prefs.PREF_LOGIN_OAUTH) {
			post.setHeader("Authorization", "Bearer " + this.auth);
		}

		// gzip
		post.setHeader("User-agent", "gzip");
		// post.setHeader("Accept-Encoding", "gzip");

		HttpResponse res = getClient().execute(post);
		int resStatus = res.getStatusLine().getStatusCode();

		if (resStatus == HttpStatus.SC_FORBIDDEN) {
			throw new ReaderLoginException("Login failure");
		} else if (resStatus == HttpStatus.SC_UNAUTHORIZED) {
			throw new ReaderLoginException("Authentication fails (" + resStatus + "): " + url);
		} else if (resStatus != HttpStatus.SC_OK) {
			throw new ReaderException("Invalid http status " + resStatus + ": " + url);
		}

		final HttpEntity entity = res.getEntity();
		if (entity == null) {
			throw new ReaderException("null response entity");
		}

		return new FilterInputStream(entity.getContent()) {
			@Override
			public void close() throws IOException {
				super.close();
				// entity.consumeContent();
			}
		};
	}

	public java.io.Reader doGetReader(String url) throws IOException, ReaderException {
		InputStream in = doGetInputStream(url);
		if (in == null) { return null; }
		return new InputStreamReader(in, HTTP.UTF_8);
	}

	public InputStream doGetInputStream(String url) throws IOException, ReaderException {
		// Log.d(TAG, "[DEBUG] GET: " + url);
		HttpGet get = new HttpGet(url);

		final Context context = getApplicationContext();
		int login = Prefs.getLoginMethod(context);
		if (login == Prefs.PREF_LOGIN_CLIENT || login == Prefs.PREF_LOGIN_ACCOUNT) {
			if (this.auth != null) get.setHeader("Authorization", "GoogleLogin auth=" + this.auth);
		} else if (login == Prefs.PREF_LOGIN_OAUTH) {
			get.setHeader("Authorization", "Bearer " + this.auth);
		}

		// gzip
		get.setHeader("User-agent", "gzip");
		// get.setHeader("Accept-Encoding", "gzip");

		HttpResponse res = getClient().execute(get);
		int resStatus = res.getStatusLine().getStatusCode();

		if (resStatus == HttpStatus.SC_FORBIDDEN) {
			throw new ReaderLoginException("Login failure");
		} else if (resStatus == HttpStatus.SC_UNAUTHORIZED) {
			throw new ReaderLoginException("Authentication fails (" + resStatus + "): " + url);
		} else if (resStatus != HttpStatus.SC_OK) {
			throw new ReaderException("Invalid http status " + resStatus + ": " + url);
		}

		final HttpEntity entity = res.getEntity();
		if (entity == null) {
			throw new ReaderException("null response entity");
		}

		InputStream in = entity.getContent();
		return in;
	}

	public boolean login() throws IOException, ReaderException {
		final Context context = getApplicationContext();

		int login = Prefs.getLoginMethod(context);
		if (login == Prefs.PREF_LOGIN_ACCOUNT) {
			this.loginId = Prefs.getGoogleId(context);
		} else if (login == Prefs.PREF_LOGIN_CLIENT) {
			String loginId = Prefs.getGoogleId(context);
			String password = Prefs.getGooglePasswd(context);
			return login(loginId, password, false);
		} else if (login == Prefs.PREF_LOGIN_NONE) {
			return false;
		}

		// oauth login
		return true;
	}

	public boolean login(String loginId, String password, boolean forcedLogout) throws IOException, ReaderException {
		this.loginId = loginId;
		this.password = password;
		initAuth();
		return isLoggedIn();
	}

	private String initAuth() throws IOException, ReaderException {
		final Context context = getApplicationContext();

		int login = Prefs.getLoginMethod(context);

		this.auth = Prefs.getGoogleAuth(context);
		long authTime = Prefs.getGoogleAuthTime(context);
		if (this.auth != null) {
			long diff = System.currentTimeMillis() - authTime;

			if (login == Prefs.PREF_LOGIN_OAUTH && diff < OAUTH_EXPIRE_TIME) return this.auth;
			else if (login != Prefs.PREF_LOGIN_OAUTH && diff < AUTH_EXPIRE_TIME) return this.auth;
		}

		if (login == Prefs.PREF_LOGIN_ACCOUNT) {
			if (this.loginId == null || this.loginId.length() == 0) {
				throw new ReaderLoginException("No account data");
			}

			try {
				final AccountManager manager = AccountManager.get(context);

				Account account = new Account(this.loginId, AuthHelper.GOOGLE_ACCOUNT_TYPE);
				manager.invalidateAuthToken(AuthHelper.GOOGLE_ACCOUNT_TYPE, this.auth);
				AccountManagerFuture<Bundle> accountManagerFuture = AccountManagerHelper.getAuthToken(manager, account, AuthHelper.AUTH_TOKEN_TYPE, true, null, null);
				final Bundle bundle = accountManagerFuture.getResult();
				if (bundle.containsKey(AccountManager.KEY_AUTHTOKEN)) {
					this.auth = bundle.getString(AccountManager.KEY_AUTHTOKEN);
				}
			} catch (Throwable t) {
				t.printStackTrace();
			}
		} else if (login == Prefs.PREF_LOGIN_CLIENT) {
			if (this.loginId == null || this.password == null) {
				throw new ReaderLoginException("No Login Data");
			}

			List<NameValuePair> params = new ArrayList<NameValuePair>(4);
			params.add(new BasicNameValuePair("accountType", "GOOGLE"));
			params.add(new BasicNameValuePair("Email", this.loginId));
			params.add(new BasicNameValuePair("Passwd", this.password));
			params.add(new BasicNameValuePair("service", "reader"));

			BufferedReader in = new BufferedReader(doPostReader(URL_LOGIN, params));
			try {
				String line;
				while ((line = in.readLine()) != null) {
					if (line.indexOf("Auth=") == 0) {
						this.auth = line.substring("Auth=".length());
						break;
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				in.close();
			}
		} else if (login == Prefs.PREF_LOGIN_OAUTH) {
			String refreshToken = Prefs.getOAuth2RefreshToken(context);
			if (refreshToken == null) {
				throw new ReaderLoginException("No Login Data");
			}

			List<NameValuePair> postParams = new ArrayList<NameValuePair>(4);
			postParams.add(new BasicNameValuePair("refresh_token", refreshToken));
			postParams.add(new BasicNameValuePair("client_id", GoogleReaderAPI.OAUTH2_CLIENT_ID));
			postParams.add(new BasicNameValuePair("client_secret", GoogleReaderAPI.OAUTH2_CLIENT_SECRET));
			postParams.add(new BasicNameValuePair("grant_type", "refresh_token"));

			HttpPost post = new HttpPost(GoogleReaderAPI.OAUTH2_TOKEN_URL);
			post.setEntity(new UrlEncodedFormEntity(postParams, HTTP.UTF_8));
			post.setHeader("Content-Type", "application/x-www-form-urlencoded");

			HttpResponse res = getClient().execute(post);
			int resStatus = res.getStatusLine().getStatusCode();

			if (resStatus == HttpStatus.SC_FORBIDDEN) {
				throw new ReaderLoginException("Login failure");
			} else if (resStatus == HttpStatus.SC_UNAUTHORIZED) {
				throw new ReaderLoginException("Authentication fails (" + resStatus + ")");
			} else if (resStatus != HttpStatus.SC_OK) {
				throw new ReaderException("Invalid http status " + resStatus);
			}

			final HttpEntity entity = res.getEntity();
			if (entity == null) {
				throw new ReaderException("null response entity");
			}

			try {
				String content = Utils.convertStreamToString(entity.getContent());
				JSONObject jsonObject = new JSONObject(content);
				String accessToken = jsonObject.getString("access_token");

				if (accessToken != null) {
					this.auth = accessToken;
					Prefs.setOAuth2Tokens(context, accessToken, refreshToken, Prefs.PREF_LOGIN_OAUTH);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		if (this.auth == null) {
			throw new ReaderException("Login Failure");
		}
		Prefs.setGoogleAuth(context, this.auth);
		return this.auth;
	}

	private String initToken() throws IOException, ReaderException {
		initAuth();
		long now = System.currentTimeMillis();
		if (this.token != null && now < this.tokenExpiredTime) {
			return this.token;
		}
		Reader in = doGetReader(URL_API_TOKEN);
		try {
			char[] cbuf = new char[64];
			int len = in.read(cbuf);
//			if (len != 57) {
//				Log.w(TAG, "unknown token length " + len + ", " + new String(cbuf, 0, len));
// 				throw new ReaderException("invalid token length " + len + ", " + new String(cbuf, 0, len));
//			}
			this.token = new String(cbuf, 0, len);
			this.tokenExpiredTime = now + TOKEN_TIME;
		} finally {
			in.close();
		}
		return this.token;
	}


	public boolean isLoggedIn() {
		return (this.auth != null);
	}

// tag label set (sync only items from tags)
//	public Set<String>	mTagLabelSet	= null;
//
//	public Set<String> getTagLabelSet(Context c) {
//		if (mTagLabelSet == null) {
//			mTagLabelSet = ReaderManager.newInstance(c).getTagLabelSet();
//		}
//		return mTagLabelSet;
//	}
	
	public boolean addUserLabel(String category, IItem entry) {
		if (category.contains("/label/") && category.startsWith("user/")) {
			return true;
//			return ReaderApp.getTagLabelSet(context).contains(category);
		} else if (category.endsWith("/state/com.google/starred") && category.startsWith("user/")) {
			entry.starred = true;
			return true;
		} else if (category.contains("itemrecs") && category.startsWith("user/")) {
			return true;
		}
		return false;
	}

	@Override
	public void handleReaderList(ITagListHandler tagHandler, ISubscriptionListHandler subHandler, long syncTime) throws IOException, ReaderException {
		Log.e(TAG, "handleReaderList");
		
		Reader in = null;
		try {
			in = readTagList(syncTime);
			parseTagList(in, tagHandler);
		} catch (JsonParseException e) {
			throw new ReaderException("data parse error", e);
		} catch (RemoteException e) {
			throw new ReaderException("remote connection error", e);			
		} finally {
			if (in != null) in.close();
		}

		try {
			in = readSubList(syncTime);
			parseSubList(in, subHandler);
		} catch (JsonParseException e) {
			throw new ReaderException("data parse error", e);
		} catch (RemoteException e) {
			throw new ReaderException("remote connection error", e);			
		} finally {
			if (in != null) in.close();
		}

//		in = readUnreadCount(syncTime);
//		try {
//			parseUnreadCountList(in, unreadHandler);
//		} catch (JsonParseException e) {
//			throw new ReaderException("data parse error", e);
//		} finally {
//			in.close();
//		}		
	}

	// NOTE: /api/0/tag/list
	private Reader readTagList(long syncTime) throws IOException, ReaderException {
		Log.e(TAG, "readTagList");
		initAuth();
		
		StringBuilder buff = new StringBuilder(URL_API_TAG_LIST.length() + 48);
		buff.append(URL_API_TAG_LIST);
		buff.append("?client=scroll&output=json&ck=").append(syncTime);

		return doGetReader(new String(buff));
	}

	private void parseTagList(Reader in, ITagListHandler handler) throws JsonParseException, IOException, RemoteException {
		Log.e(TAG, "parseTagList");
		
		JsonFactory f = new JsonFactory();
		JsonParser jp = f.createJsonParser(in);

		String currName;

		ITag tag = null;
		List<ITag> tagList = new ArrayList<ITag>();

		jp.nextToken(); // will return JsonToken.START_OBJECT (verify?)
		while (jp.nextToken() != JsonToken.END_OBJECT) {
			currName = jp.getCurrentName();
			jp.nextToken(); // move to value, or START_OBJECT/START_ARRAY
			if (currName.equals("tags")) { // contains an object
				// start items
				while (jp.nextToken() != JsonToken.END_ARRAY) {
					if (jp.getCurrentToken() == JsonToken.START_OBJECT) {
						tag = new ITag();
					} else if (jp.getCurrentToken() == JsonToken.END_OBJECT) {
						tagList.add(tag);
						tag = null;
					}

					currName = jp.getCurrentName();
					if (currName == null) continue;
					jp.nextToken();
					if (currName.equals("id")) {
						tag.uid = jp.getText();
						int i = tag.uid.indexOf("/label/");
						if (i != -1) {
							tag.label = tag.uid.substring(i + "/label/".length());
							// NOTE: @see ReaderManager#updateTagTypes
							tag.type = ITag.TYPE_TAG_LABEL;
						} else if (tag.uid.endsWith("state/com.google/starred")) {
							tag.label = "state/com.google/starred";
							tag.type = ITag.TYPE_TAG_STARRED;
						}
					} else if (currName.equals("sortid")) {
						tag.sortid = jp.getText();
					} else {
						jp.skipChildren();
					}
				}

				handler.tags(tagList);

			} else {
				jp.skipChildren();
			}
		}
	}	

	// NOTE: /api/0/subscription/list
	private Reader readSubList(long syncTime) throws IOException, ReaderException {
		Log.e(TAG, "readSubList");
		initAuth();

		StringBuilder buff = new StringBuilder(URL_API_SUB_LIST.length() + 48);
		buff.append(URL_API_SUB_LIST);
		buff.append("?client=scroll&output=json&ck=").append(syncTime);

		return doGetReader(new String(buff));
	}

	private void parseSubList(Reader in, ISubscriptionListHandler handler) throws JsonParseException, IOException, RemoteException {
		JsonFactory f = new JsonFactory();
		JsonParser jp = f.createJsonParser(in);

		String currName;

		ISubscription feed = null;
		List<ISubscription> feedList = new ArrayList<ISubscription>();

		jp.nextToken(); // will return JsonToken.START_OBJECT (verify?)
		while (jp.nextToken() != JsonToken.END_OBJECT) {			
			currName = jp.getCurrentName();

			jp.nextToken(); // move to value, or START_OBJECT/START_ARRAY
			if (currName.equals("subscriptions")) { // contains an object
				// start items
				while (jp.nextToken() != JsonToken.END_ARRAY) {
					if (jp.getCurrentToken() == JsonToken.START_OBJECT) {
						feed = new ISubscription();
					} else if (jp.getCurrentToken() == JsonToken.END_OBJECT) {
						feedList.add(feed);
						feed = null;
					}

					currName = jp.getCurrentName();

					if (currName == null) continue;
					jp.nextToken();
					if (currName.equals("id")) {
						feed.uid = jp.getText();
					} else if (currName.equals("title")) {
						feed.title = unEscapeEntities(jp.getText());
					} else if (currName.equals("sortid")) {
						feed.sortid = jp.getText();
					} else if (currName.equals("htmlUrl")) {
						feed.htmlUrl = jp.getText();
					} else if (currName.equals("categories")) {
						while (jp.nextToken() != JsonToken.END_ARRAY) {
							currName = jp.getCurrentName();
							if (currName == null) continue;
							jp.nextToken();
							if (currName.equals("id")) {
								feed.addCategory(jp.getText());
							} else {
								jp.skipChildren();
							}
						}
					} else {
						jp.skipChildren();
					}
				}

				handler.subscriptions(feedList);

			} else {
				jp.skipChildren();
			}
		}
	}

	// NOTE: /api/0/unread-count
//	private Reader readUnreadCount(long syncTime) throws IOException, ReaderException {
//		initAuth();
//
//		StringBuilder buff = new StringBuilder(URL_API_UNREAD_COUNT.length() + 48);
//		buff.append(URL_API_UNREAD_COUNT);
//		buff.append("?client=scroll&output=json&ck=").append(syncTime);
//
//		return doGetReader(new String(buff));
//	}
//
//	private void parseUnreadCountList(Reader in, UnreadCountHandler handler) throws JsonParseException, IOException {
//		JsonFactory f = new JsonFactory();
//		JsonParser jp = f.createJsonParser(in);
//
//		String currName;
//
//		Unread unread = null;
//		List<Unread> unreadList = new ArrayList<Unread>();
//
//		jp.nextToken(); // will return JsonToken.START_OBJECT (verify?)
//		while (jp.nextToken() != JsonToken.END_OBJECT) {
//			currName = jp.getCurrentName();
//			jp.nextToken(); // move to value, or START_OBJECT/START_ARRAY
//			if (currName.equals("unreadcounts")) { // contains an object
//				// start items
//				while (jp.nextToken() != JsonToken.END_ARRAY) {
//					if (jp.getCurrentToken() == JsonToken.START_OBJECT) {
//						unread = new Unread();
//					} else if (jp.getCurrentToken() == JsonToken.END_OBJECT) {
//						unreadList.add(unread);
//						unread = null;
//					}
//
//					currName = jp.getCurrentName();
//					if (currName == null) continue;
//					jp.nextToken();
//					if (currName.equals("id")) {
//						unread.id = jp.getText();
//					} else if (currName.equals("newestItemTimestampUsec")) {
//						unread.newestTime = Utils.asLong(jp.getText().substring(0, jp.getText().length() - 6)); // millis -> unixtime
//					} else {
//						jp.skipChildren();
//					}
//				}
//
//				try {
//					handler.unreadCounts(unreadList);
//				} catch (ReaderException e) {
//				}
//
//			} else {
//				jp.skipChildren();
//			}
//		}
//	}

	@Override
	public void handleItemList(IItemListHandler handler, long syncTime) throws IOException, ReaderException {
		// http://www.google.com/reader/api/0/stream/contents/user%2F-%2Fstate%2Fcom.google%2Fread?client=scroll&output=json&ck=1276066665822&n=20&r=n
		Log.e(TAG, "handleItemList");
		Reader in = null;
		try {
//			long startTime = handler.getStartTime();
			final Context c = getApplicationContext();
			long startTime = Prefs.getLastSyncTime(c);
			in = readStreamContents(syncTime, startTime, handler, null);
			
			String continuation = parseItemList(in, handler);

			int limit = handler.getLimit();
			int max = limit > SYNC_MAX_ITEMS ? SYNC_MAX_ITEMS : limit;
			int count = 1; // continuation count
			while ((limit == 0 || limit > max * count) && handler.isContinuation() && continuation != null && continuation.length() > 0) {
				in.close();
				in = readStreamContents(syncTime, startTime, handler, continuation);
				continuation = parseItemList(in, handler);
				count++;
			}
		} catch (JsonParseException e) {
			throw new ReaderException("data parse error", e);
		} catch (RemoteException e) {
			throw new ReaderException("remote connection error", e);			
		} finally {
			if (in != null) in.close(); // ensure resources get cleaned up timely
		}
	}

	// NOTE: /api/0/stream/contents
	private Reader readStreamContents(long syncTime, long startTime, IItemListHandler handler, String continuation) throws IOException, ReaderException, RemoteException {
		initAuth();

		StringBuilder buff = new StringBuilder(URL_API_STREAM_CONTENTS.length() + 128);
		buff.append(URL_API_STREAM_CONTENTS);
		String subUid = handler.getUid();
		if (subUid != null) {
			buff.append("/").append(UrlUtils.encode(subUid));
		}
		buff.append("?client=scroll&ck=").append(syncTime);
		if (handler.isExcludeRead()) {
			buff.append("&xt=").append(STATE_READ);
		}
		if (continuation != null && continuation.length() > 0) {
			buff.append("&c=").append(continuation);
		}
		if (startTime > 0) {
			buff.append("&ot=").append(startTime);
		}
		int limit = handler.getLimit();
		limit = (limit > SYNC_MAX_ITEMS || limit == 0) ? SYNC_MAX_ITEMS : limit;
		if (limit > 0) {
			// google only allows max 1000 at once
			buff.append("&n=").append(limit > SYNC_MAX_ITEMS ? SYNC_MAX_ITEMS : limit);
		}
		buff.append("&r=").append(handler.isNewer() ? "n" : "o");
		return doGetReader(new String(buff));
	}

	private String parseItemList(Reader in, IItemListHandler handler) throws JsonParseException, IOException, RemoteException {
		JsonFactory f = new JsonFactory();
		JsonParser jp = f.createJsonParser(in);

		String currName;

		IItem entry = null;
		String continuation = null;
		List<IItem> itemList = new ArrayList<IItem>();

		List<String> excludedSubs = handler.excludeSubs(); // excluded subscriptions

		String additionalCategory = null;
		if (handler.getUid() != null && handler.getUid().contains("itemrecs")) {
			additionalCategory = STATE_ITEMRECS;
		}

		jp.nextToken(); // will return JsonToken.START_OBJECT (verify?)
		while (jp.nextToken() != JsonToken.END_OBJECT) {

			currName = jp.getCurrentName();
			jp.nextToken(); // move to value, or START_OBJECT/START_ARRAY
			if ("continuation".equals(currName)) {
				continuation = jp.getText();
			} else if ("items".equals(currName)) { // contains an object
				// start items
				while (jp.nextToken() != JsonToken.END_ARRAY) {
					// request stop
					if (handler.requestStop()) throw new JsonParseException(null, null);

					if (jp.getCurrentToken() == JsonToken.START_OBJECT) {
						entry = new IItem();
					} else if (jp.getCurrentToken() == JsonToken.END_OBJECT) {
						if (entry != null && entry.uid.length() > 0) {
							// add additional category to item
							if (additionalCategory != null) entry.addCategory(additionalCategory);
							itemList.add(entry);
						}
						if (itemList.size() % 100 == 0) {
							handler.items(itemList);
							itemList.clear();
						}
						entry = null;
					}

					currName = jp.getCurrentName();
					if (currName == null) continue;

					jp.nextToken(); // move to value
					if (currName.equals("id")) {
						entry.uid = stripItemUid(jp.getText());
					} else if (currName.equals("crawlTimeMsec")) {
						entry.updatedTime = Long.valueOf(jp.getText()) / 1000;
					} else if (currName.equals("title")) {
						entry.title = HtmlUtils.stripTags(unEscapeEntities(jp.getText()), true);
					} else if (currName.equals("categories")) {
						while (jp.nextToken() != JsonToken.END_ARRAY) {
							String category = jp.getText();
							if (category != null && addUserLabel(category, entry)) {
								entry.addCategory(category);
							}
							if (category != null && category.endsWith("/state/com.google/read")) {
								entry.read = true;
							}
						}
					} else if (currName.equals("published")) {
						entry.publishedTime = jp.getLongValue();
					} else if (currName.equals("alternate")) {
						while (jp.nextToken() != JsonToken.END_ARRAY) {
							currName = jp.getCurrentName();
							if (currName == null) continue;
							jp.nextToken();
							if (currName.equals("href")) {
								entry.link = jp.getText();
							} else {
								jp.skipChildren();
							}
						}
					} else if (currName.equals("enclosure")) {
						while (jp.nextToken() != JsonToken.END_ARRAY) {
							currName = jp.getCurrentName();
							if (currName == null) continue;
							jp.nextToken();
							if (currName.equals("href")) {
								entry.media = jp.getText();
							} else if (currName.equals("type")) {
								String type = jp.getText();
								if (!type.startsWith("application")) entry.mediaType = type;
								else entry.media = null;
							} else {
								jp.skipChildren();
							}
						}
					} else if (currName.equals("summary") || currName.equals("content")) {
						while (jp.nextToken() != JsonToken.END_OBJECT) {
							currName = jp.getCurrentName();
							if (currName == null) continue;
							jp.nextToken();
							if (currName.equals("content")) {
								entry.content = unEscapeEntities(jp.getText());
							} else {
								jp.skipChildren();
							}
						}
					} else if (currName.equals("author")) {
						entry.author = jp.getText();
					} else if (currName.equals("origin")) {
						while (jp.nextToken() != JsonToken.END_OBJECT) {
							currName = jp.getCurrentName();
							if (currName == null) continue;
							jp.nextToken();
							if (currName.equals("streamId")) {
								String streamId = jp.getText();
								if (streamId != null && (excludedSubs == null || !excludedSubs.contains(streamId))) {
									entry.subUid = streamId;
								} else entry = null;
							} else {
								jp.skipChildren();
							}
						}
					} else {
						jp.skipChildren();
					}
				}

				handler.items(itemList);
				itemList.clear();

			} else {
				jp.skipChildren();
			}
		}

		return continuation;
	}
	
	@Override
	public void handleItemIdList(IItemIdListHandler handler, long syncTime) throws IOException, ReaderException {
		// http://www.google.com/reader/api/0/stream/contents/user%2F-%2Fstate%2Fcom.google%2Fread?client=scroll&output=json&ck=1276066665822&n=20&r=n
		Reader in = null;
		try {
			in = readStreamItemIds(syncTime, handler);
			parseItemIdList(in, handler);
		} catch (JsonParseException e) {
			throw new ReaderException("data parse error", e);
		} catch (RemoteException e) {
			throw new ReaderException("remote connection error", e);			
		} finally {
			if (in != null) in.close();
		}
	}

	// NOTE: /api/0/stream/items/ids
	private Reader readStreamItemIds(long syncTime, IItemIdListHandler handler) throws IOException, ReaderException, RemoteException {
		initAuth();

		StringBuilder buff = new StringBuilder(URL_API_STREAM_ITEM_IDS.length() + 128);
		buff.append(URL_API_STREAM_ITEM_IDS);
		buff.append("?output=json"); // xml or json
		String uid = handler.getUid();
		if (uid != null) {
			buff.append("&s=");
			buff.append(UrlUtils.encode(uid));
//			if (uids.length > 1) buff.append("&merge=true&includeAllDirectStreamIds=true");
		}
		if (handler.isExcludeRead()) {
			buff.append("&xt=").append(STATE_READ);
		}
		long startTime = handler.getStartTime();
		if (startTime > 0) {
			buff.append("&ot=").append(startTime);
		}
		int limit = handler.getLimit();
		if (limit > 0) {
			buff.append("&n=").append(limit);
		}
		buff.append("&r=").append(handler.isNewer() ? "n" : "o");
		return doGetReader(buff.toString());
	}

	private void parseItemIdList(Reader in, IItemIdListHandler handler) throws JsonParseException, IOException, RemoteException {
		JsonFactory f = new JsonFactory();
		JsonParser jp = f.createJsonParser(in);

		boolean hex = handler.isHex();
		String currName;

		List<String> idList = new ArrayList<String>();

		jp.nextToken(); // will return JsonToken.START_OBJECT (verify?)
		while (jp.nextToken() != JsonToken.END_OBJECT) {
			currName = jp.getCurrentName();
			jp.nextToken(); // move to value, or START_OBJECT/START_ARRAY
			if (currName.equals("itemRefs")) { // contains an object
				// start items
				while (jp.nextToken() != JsonToken.END_ARRAY) {
					currName = jp.getCurrentName();
					if (currName == null) continue;
					jp.nextToken();
					if (currName.equals("id")) {
						idList.add(hex ? dec2Hex(jp.getText()) : jp.getText()); // convert dec to hex
					} else {
						jp.skipChildren();
					}
				}

				handler.items(idList);

			} else {
				jp.skipChildren();
			}
		}
	}	
	
	@Override
	public boolean markAsRead(List<String> itemUids, List<String> subUids) throws IOException, ReaderException {
		List<String> add = new ArrayList<String>();
		add.add(STATE_READ);
		return editItemTag(itemUids, subUids, add, null);
	}

	@Override
	public boolean markAsUnread(List<String> itemUids, List<String> subUids, boolean keepUnread) throws IOException, ReaderException {
		List<String> remove = new ArrayList<String>();
		remove.add(STATE_READ);
		return editItemTag(itemUids, subUids, null, remove);
	}

	@Override
	public boolean markAllAsRead(String s, String t, long syncTime) throws IOException, ReaderException {
		if (s == null) {
			s = STATE_READING_LIST;
			t = "all";
		}
		String token = initToken();
		List<NameValuePair> params = new ArrayList<NameValuePair>(5);
		params.add(new BasicNameValuePair("T", token));
		params.add(new BasicNameValuePair("s", s));
		params.add(new BasicNameValuePair("t", t));
		params.add(new BasicNameValuePair("ts", syncTime + "999"));
	
		Reader in = doPostReader(URL_API_MARK_ALL_AS_READ, params);
		try {
			char[] cbuf = new char[128];
			int len = in.read(cbuf);
			if (len != -1) {
				String res = new String(cbuf, 0, len);
				return res.equals("OK");
			}
		} finally {
			in.close();
		}
		return false;
	}

	@Override
	public boolean editItemTag(List<String> itemUids, List<String> subUids, List<String> addTags, List<String> removeTags) throws IOException, ReaderException {
		Log.e(TAG, "editItemTag");
		
		String token = initToken();
		List<NameValuePair> params = new ArrayList<NameValuePair>(5);
		params.add(new BasicNameValuePair("T", token));
	
		if (addTags != null && addTags.size() > 0) {
			for (String tagUid : addTags) {
				params.add(new BasicNameValuePair("a", tagUid));
			}
		}
	
		if (removeTags != null && removeTags.size() > 0) {
			for (String tagUid : removeTags) {
				params.add(new BasicNameValuePair("r", tagUid));
			}
		}
	
		if (itemUids != null && itemUids.size() > 0) {
			for (int i = 0; i < itemUids.size(); i++) {
				params.add(new BasicNameValuePair("i", getItemUid(itemUids.get(i))));
				params.add(new BasicNameValuePair("s", subUids.get(i)));
			}
		}
	
		params.add(new BasicNameValuePair("async", "true"));
	
		Reader in = doPostReader(URL_API_EDIT_TAG, params);
		try {
			char[] cbuf = new char[128];
			int len = in.read(cbuf);
			if (len != -1) {
				String res = new String(cbuf, 0, len);
				return res.equals("OK");
			}
		} finally {
			in.close();
		}
		return false;
	}

	@Override
	public boolean editSubscription(String uid, String title, String url, String tag, int action, long syncTime) throws IOException, ReaderException {
		String token = initToken();
		List<NameValuePair> params = new ArrayList<NameValuePair>(4);
		params.add(new BasicNameValuePair("T", token));
		if (title != null) {
			params.add(new BasicNameValuePair("t", title));
		}
		switch (action) {
			case ReaderExtension.SUBSCRIPTION_ACTION_EDIT:
				params.add(new BasicNameValuePair("ac", "edit"));
				break;
			case ReaderExtension.SUBSCRIPTION_ACTION_ADD_LABEL:
				params.add(new BasicNameValuePair("ac", "edit"));
				params.add(new BasicNameValuePair("a", tag));
				break;
			case ReaderExtension.SUBSCRIPTION_ACTION_REMOVE_LABEL:
				params.add(new BasicNameValuePair("ac", "edit"));
				params.add(new BasicNameValuePair("r", tag));
				break;
			case ReaderExtension.SUBSCRIPTION_ACTION_SUBCRIBE:
				params.add(new BasicNameValuePair("ac", "subscribe"));
				break;
			case ReaderExtension.SUBSCRIPTION_ACTION_UNSUBCRIBE:
				params.add(new BasicNameValuePair("ac", "unsubscribe"));
				break;
		}
		params.add(new BasicNameValuePair("s", "feed/" + url));
	
		Reader in = doPostReader(URL_API_SUBSCIPTION, params);
		try {
			char[] cbuf = new char[128];
			int len = in.read(cbuf);
			if (len != -1) {
				String res = new String(cbuf, 0, len);
				return res.equals("OK");
			}
		} finally {
			in.close();
		}
		return false;
	}

	@Override
	public boolean renameTag(String tagUid, String oldLabel, String newLabel) throws IOException, ReaderException {
		String token = initToken();
		List<NameValuePair> params = new ArrayList<NameValuePair>(5);
		params.add(new BasicNameValuePair("T", token));
		params.add(new BasicNameValuePair("s", tagUid));
		params.add(new BasicNameValuePair("t", oldLabel));
		params.add(new BasicNameValuePair("dest", getTagUIdPath(tagUid) + newLabel));
	
		Reader in = doPostReader(URL_API_RENAME_TAG, params);
		try {
			char[] cbuf = new char[128];
			int len = in.read(cbuf);
			if (len != -1) {
				String res = new String(cbuf, 0, len);
				return res.equals("OK");
			}
		} finally {
			in.close();
		}
		return false;
	}

	@Override
	public boolean disableTag(String tagUid, String label) throws IOException, ReaderException {
		String token = initToken();
		List<NameValuePair> params = new ArrayList<NameValuePair>(5);
		params.add(new BasicNameValuePair("T", token));
		params.add(new BasicNameValuePair("s", tagUid));
		params.add(new BasicNameValuePair("t", label));
	
		Reader in = doPostReader(URL_API_DISABLE_TAG, params);
		try {
			char[] cbuf = new char[128];
			int len = in.read(cbuf);
			if (len != -1) {
				String res = new String(cbuf, 0, len);
				return res.equals("OK");
			}
		} finally {
			in.close();
		}
		return false;
	}

}
