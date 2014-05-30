package com.noinnion.android.newsplus.extension.google_reader;

import java.io.BufferedReader;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

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
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.os.RemoteException;
import android.text.TextUtils;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.noinnion.android.newsplus.extension.google_reader.util.Utils;
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

	public static final String	TAG								= "GoogleReaderClient";

	public static final String	URL_LOGIN						= "/accounts/ClientLogin";
	public static final String	URL_API_						= "/api/0";
	public static final String	URL_ATOM_						= "/atom";
	public static final String	URL_API_TOKEN					= URL_API_ + "/token";
	public static final String	URL_API_USER_INFO				= URL_API_ + "/user-info";
	public static final String	URL_API_SUB_LIST				= URL_API_ + "/subscription/list";
	public static final String	URL_API_TAG_LIST				= URL_API_ + "/tag/list";
	public static final String	URL_API_RECOMMENDATION_LIST		= URL_API_ + "/recommendation/list";
	public static final String	URL_API_UNREAD_COUNT			= URL_API_ + "/unread-count";
	public static final String	URL_API_STREAM_CONTENTS			= URL_API_ + "/stream/contents";
	public static final String	URL_API_STREAM_ITEM_IDS			= URL_API_ + "/stream/items/ids";
	public static final String	URL_API_STREAM_ITEMS_CONTENTS	= URL_API_ + "/stream/items/contents";
	public static final String	URL_API_FEED_FINDER				= URL_API_ + "/feed-finder?output=json";
	public static final String	URL_API_EDIT_TAG				= URL_API_ + "/edit-tag?client=newsplus";
	public static final String	URL_API_EDIT_ITEM				= URL_API_ + "/item/edit?client=newsplus";
	public static final String	URL_API_RENAME_TAG				= URL_API_ + "/rename-tag?client=newsplus";
	public static final String	URL_API_DISABLE_TAG				= URL_API_ + "/disable-tag?client=newsplus";
	public static final String	URL_API_MARK_ALL_AS_READ		= URL_API_ + "/mark-all-as-read?client=newsplus";
	public static final String	URL_API_SUBSCIPTION				= URL_API_ + "/subscription/edit?client=newsplus";
	public static final String	URL_API_BUNDLES					= URL_API_ + "/bundles?output=json";
	public static final String	URL_API_PREFERENCE_LIST			= URL_API_ + "/preference/list?output=json";
	public static final String	URL_API_PREFERENCE_STREAM_LIST	= URL_API_ + "/preference/stream/list?output=json";

	public static final String	STATE_GOOGLE_READING_LIST		= "user/-/state/com.google/reading-list";
	public static final String	STATE_GOOGLE_READ				= "user/-/state/com.google/read";
	public static final String	STATE_GOOGLE_STARRED			= "user/-/state/com.google/starred";
	public static final String	STATE_GOOGLE_ITEMRECS			= "user/-/state/com.google/itemrecs";
	public static final String	STATE_GOOGLE_LABEL				= "user/-/label/";

	private static final int	SYNC_MAX_ITEMS					= 1000;											// 1000

	//	private static final String	TAG							= "GoogleReaderClient";
	private static final long	TOKEN_TIME						= 15 * 60 * 1000;								// 15 min
	private static final long	AUTH_EXPIRE_TIME				= 3 * 86400 * 1000;								// 3 days

	private String				mServer;
	private String				mLoginId;
	private String				mPassword;
	private String				auth;
	private String				token;
	private long				tokenExpiredTime;

	public Context mContext;

	public GoogleReaderClient() {
	}

	public GoogleReaderClient(Context c) {
		mContext = c;
	}

	private Context getContext() {
		return mContext == null ? getApplicationContext() : mContext;
	}

	protected DefaultHttpClient	client;

	public DefaultHttpClient getClient() {
		if (client == null) client = Utils.createHttpClient();
		return client;
	}

	private String getServer() {
		if (mServer == null) {
			mServer = Prefs.getServer(getContext());
		}
		return mServer;
	}

	private String getApiUrl(String api) {
		return getServer() + "/reader" + api;
	}

	private String getLoginUrl() {
		String server = getServer();

		// inoreader
		if (server != null && server.contains("inoreader.com") && server.startsWith("http://")) {
			server = server.replace("http://", "https://");
		}

		return server + URL_LOGIN;
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

	public static String getTagUidPath(String uid) {
		int pos = uid.indexOf("/label/");
		return uid.substring(0, pos + "/label/".length());
	}

	public java.io.Reader doPostReader(String url, List<NameValuePair> params) throws IOException, ReaderException {
		return new InputStreamReader(doPostInputStream(url, params), HTTP.UTF_8);
	}

	public InputStream doPostInputStream(String url, List<NameValuePair> params) throws IOException, ReaderException {
//		Log.e(TAG, "[DEBUG] POST: " + url);
// 		Log.e(TAG, "[DEBUG] PARAMS: " + params);
		HttpPost post = new HttpPost(url);
		post.setEntity(new UrlEncodedFormEntity(params, HTTP.UTF_8));

		if (this.auth != null) post.setHeader("Authorization", "GoogleLogin auth=" + this.auth);

		// gzip
		post.setHeader("User-agent", "gzip");
		post.setHeader("Accept-Encoding", "gzip");

		HttpResponse res = getClient().execute(post);
		int resStatus = res.getStatusLine().getStatusCode();

		if (resStatus == HttpStatus.SC_FORBIDDEN) throw new ReaderLoginException("Login failure");
		else if (resStatus == HttpStatus.SC_UNAUTHORIZED) throw new ReaderLoginException("Authentication fails (" + resStatus + "): " + url);
		else if (resStatus != HttpStatus.SC_OK) { throw new ReaderException("Invalid http status " + resStatus + ": " + url); }

		final HttpEntity entity = res.getEntity();
		if (entity == null) { throw new ReaderException("null response entity"); }

		InputStream is = null;

		// create the appropriate stream wrapper based on the encoding type
		String encoding = Utils.getHeaderValue(entity.getContentEncoding());
		if (encoding != null && encoding.equalsIgnoreCase("gzip")) {
			is = new GZIPInputStream(entity.getContent());
		} else if (encoding != null && encoding.equalsIgnoreCase("deflate")) {
			is = new InflaterInputStream(entity.getContent(), new Inflater(true));
		} else {
			is = entity.getContent();
		}

		return new FilterInputStream(is) {
			@Override
			public void close() throws IOException {
				super.close();
				entity.consumeContent();
			}
		};
	}

	public java.io.Reader doGetReader(String url) throws IOException, ReaderException {
		InputStream in = doGetInputStream(url);
		if (in == null) { return null; }
		return new InputStreamReader(in, HTTP.UTF_8);
	}

	public InputStream doGetInputStream(String url) throws IOException, ReaderException {
//		Log.e(TAG, "[DEBUG] GET: " + url);
//		Log.e(TAG, "[DEBUG] Authorization: " + "GoogleLogin auth=" + this.auth);
		HttpGet get = new HttpGet(url);

		if (this.auth != null) get.setHeader("Authorization", "GoogleLogin auth=" + this.auth);
		
		// gzip
		get.setHeader("User-agent", "gzip");
		get.setHeader("Accept-Encoding", "gzip");

		HttpResponse res = getClient().execute(get);
		int resStatus = res.getStatusLine().getStatusCode();

		if (resStatus == HttpStatus.SC_FORBIDDEN) throw new ReaderLoginException("Login failure");
		else if (resStatus == HttpStatus.SC_UNAUTHORIZED) throw new ReaderLoginException("Authentication fails (" + resStatus + "): " + url);
		else if (resStatus != HttpStatus.SC_OK) { throw new ReaderException("Invalid http status " + resStatus + ": " + url); }

		final HttpEntity entity = res.getEntity();
		if (entity == null) { throw new ReaderException("null response entity"); }

		InputStream is = null;

		// create the appropriate stream wrapper based on the encoding type
		String encoding = Utils.getHeaderValue(entity.getContentEncoding());
		if (encoding != null && encoding.equalsIgnoreCase("gzip")) {
			is = new GZIPInputStream(entity.getContent());
		} else if (encoding != null && encoding.equalsIgnoreCase("deflate")) {
			is = new InflaterInputStream(entity.getContent(), new Inflater(true));
		} else {
			is = entity.getContent();
		}

		return new FilterInputStream(is) {
			@Override
			public void close() throws IOException {
				super.close();
				entity.consumeContent();
			}
		};
	}

	public boolean login() throws IOException, ReaderException {
		final Context context = getContext();

		String server = Prefs.getServer(context);
		String loginId = Prefs.getGoogleId(context);
		String password = Prefs.getGooglePasswd(context);
		return login(server, loginId, password, false);
	}

	public boolean login(String server, String loginId, String password, boolean forcedLogout) throws IOException, ReaderException {
		this.mServer = server;
		this.mLoginId = loginId;
		this.mPassword = password;
		initAuth();
		return isLoggedIn();
	}

	private String initAuth() throws IOException, ReaderException {
		final Context context = getContext();

		this.auth = Prefs.getGoogleAuth(context);
		long authTime = Prefs.getGoogleAuthTime(context);
		if (this.auth != null) {
			long diff = System.currentTimeMillis() - authTime;
			if (diff < AUTH_EXPIRE_TIME) return this.auth;
		}

		if (TextUtils.isEmpty(this.mLoginId)) this.mLoginId = Prefs.getGoogleId(context);
		if (TextUtils.isEmpty(this.mPassword)) this.mPassword = Prefs.getGooglePasswd(context);
		if (this.mLoginId == null || this.mPassword == null) {
			throw new ReaderLoginException("No Login Data");
		}

		List<NameValuePair> params = new ArrayList<NameValuePair>(4);
		params.add(new BasicNameValuePair("accountType", "GOOGLE"));
		params.add(new BasicNameValuePair("Email", this.mLoginId));
		params.add(new BasicNameValuePair("Passwd", this.mPassword));
		params.add(new BasicNameValuePair("service", "reader"));

		BufferedReader in = new BufferedReader(doPostReader(getLoginUrl(), params));
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

		if (this.auth == null) throw new ReaderException("Login Failure");

		Prefs.setGoogleAuth(context, this.auth);
		return this.auth;
	}

	private String initToken() throws IOException, ReaderException {
		initAuth();
		long now = System.currentTimeMillis();
		if (this.token != null && now < this.tokenExpiredTime) { return this.token; }
		Reader in = doGetReader(getApiUrl(URL_API_TOKEN));
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

	public boolean addUserLabel(String category, IItem entry) {
		if (category.startsWith("user/") && category.contains("/label/")) {
			return true;
		} else if (category.startsWith("user/") && category.endsWith("/state/com.google/starred")) {
			entry.starred = true;
			return true;
		} else if (category.contains("itemrecs") && category.startsWith("user/")) {
			return true;
		}
		return false;
	}
	
	public String getUserId() throws IOException, ReaderException {
		String userId = null;
		InputStream in = doGetInputStream(getApiUrl(URL_API_USER_INFO));
		try {
			String content = Utils.convertStreamToString(in);
			if (TextUtils.isEmpty(content)) return null;
			JSONObject jo = new JSONObject(content);
			if (jo.has("userId")) return jo.getString("userId");
		} catch (JSONException e) {
			e.printStackTrace();
		} finally {
			if (in != null) in.close();
		}
		return userId;
	}

	@Override
	public void handleReaderList(ITagListHandler tagHandler, ISubscriptionListHandler subHandler, long syncTime) throws IOException, ReaderException {
		Reader in = null;
		try {
			in = readTagList(syncTime);
			parseTagList(in, tagHandler);
		} catch (JsonParseException e) {
			e.printStackTrace();
			throw new ReaderException("data parse error", e);
		} catch (RemoteException e) {
			e.printStackTrace();
			throw new ReaderException("remote connection error", e);
		} finally {
			if (in != null) in.close();
		}

		try {
			in = readSubList(syncTime);
//			parseSubList(in, subHandler, getUnreadList(syncTime));
			parseSubList(in, subHandler, null);
		} catch (JsonParseException e) {
			e.printStackTrace();
			throw new ReaderException("data parse error", e);
		} catch (RemoteException e) {
			e.printStackTrace();
			throw new ReaderException("remote connection error", e);
		} finally {
			if (in != null) in.close();
		}
	}

	// NOTE: /api/0/tag/list
	private Reader readTagList(long syncTime) throws IOException, ReaderException {
		initAuth();

		StringBuilder buff = new StringBuilder();
		buff.append(getApiUrl(URL_API_TAG_LIST));
		buff.append("?client=newsplus&output=json&ck=").append(syncTime);

		return doGetReader(buff.toString());
	}

	private void parseTagList(Reader in, ITagListHandler handler) throws JsonParseException, IOException, RemoteException {
		JsonFactory f = new JsonFactory();
		JsonParser jp = f.createParser(in);

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
		initAuth();

		StringBuilder buff = new StringBuilder();
		buff.append(getApiUrl(URL_API_SUB_LIST));
		buff.append("?client=newsplus&output=json&ck=").append(syncTime);

		return doGetReader(buff.toString());
	}

	private void parseSubList(Reader in, ISubscriptionListHandler handler, Map<String, Long> updatedTimes) throws JsonParseException, IOException, RemoteException {
		JsonFactory f = new JsonFactory();
		JsonParser jp = f.createParser(in);

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
						if (updatedTimes != null && updatedTimes.containsKey(feed.uid)) feed.newestItemTime = updatedTimes.get(feed.uid);
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
								feed.addTag(jp.getText());
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

	// not needed anymore, main app updates subscription newest times from items
//	private Map<String, Long> getUnreadList(long syncTime) {
//		Reader in = null;
//		try {
//			in = readUnreadCount(syncTime);
//			Map<String, Long> unreads = parseUnreadCountList(in);
//			return unreads;
//		} catch (Exception e) {
//			e.printStackTrace();
//		} finally {
//			try {
//				if (in != null) in.close();
//			} catch (IOException e) {}
//		}
//		return null;
//	}

	// NOTE: /api/0/unread-count
//	private Reader readUnreadCount(long syncTime) throws IOException, ReaderException {
//		initAuth();
//
//		StringBuilder buff = new StringBuilder(URL_API_UNREAD_COUNT.length() + 48);
//		buff.append(getApiUrl(URL_API_UNREAD_COUNT));
//		buff.append("?client=newsplus&output=json&ck=").append(syncTime);
//
//		return doGetReader(buff.toString());
//	}

//	private Map<String, Long> parseUnreadCountList(Reader in) throws JsonParseException, IOException {
//		JsonFactory f = new JsonFactory();
//		JsonParser jp = f.createParser(in);
//
//		String currName;
//
//		String uid = null;
//		Long timestamp = null;
//		int len;
//		String text = null;
//		Map<String, Long> unreadList = new HashMap<String, Long>();
//
//		jp.nextToken(); // will return JsonToken.START_OBJECT (verify?)
//		while (jp.nextToken() != JsonToken.END_OBJECT) {
//			currName = jp.getCurrentName();
//			jp.nextToken(); // move to value, or START_OBJECT/START_ARRAY
//			if (currName.equals("unreadcounts")) { // contains an object
//				// start items
//				while (jp.nextToken() != JsonToken.END_ARRAY) {
//					if (jp.getCurrentToken() == JsonToken.START_OBJECT) {
//						// do nothing
//					} else if (jp.getCurrentToken() == JsonToken.END_OBJECT) {
//						if (!unreadList.containsKey(uid)) unreadList.put(uid, timestamp);
//					}
//
//					currName = jp.getCurrentName();
//					if (currName == null) continue;
//
//					jp.nextToken();
//					if (currName.equals("id")) {
//						uid = jp.getText();
//					} else if (currName.equals("newestItemTimestampUsec")) {
//						text = jp.getText();
//						len = text.length();
//						if (len > 13) text = jp.getText().substring(0, jp.getText().length() - 6);
//						else if (len > 10) text = jp.getText().substring(0, jp.getText().length() - 3);
//						timestamp = Utils.asLong(text); // millis -> unixtime
//					} else {
//						jp.skipChildren();
//					}
//				}
//			} else {
//				jp.skipChildren();
//			}
//		}
//		
//		return unreadList;
//	}

	@Override
	public void handleItemList(IItemListHandler handler, long syncTime) throws IOException, ReaderException {
		// http://www.google.com/reader/api/0/stream/contents/user%2F-%2Fstate%2Fcom.google%2Fread?client=newsplus&output=json&ck=1276066665822&n=20&r=n
		Reader in = null;
		try {
			long startTime = handler.startTime();
			in = readStreamContents(syncTime, startTime, handler, null);

			String continuation = parseItemList(in, handler);

			int limit = handler.limit();
			int max = limit > SYNC_MAX_ITEMS ? SYNC_MAX_ITEMS : limit;
			int count = 1; // continuation count
			while ((limit == 0 || limit > max * count) && handler.continuation() && continuation != null && continuation.length() > 0) {
				in.close();
				in = readStreamContents(syncTime, startTime, handler, continuation);
				continuation = parseItemList(in, handler);
				count++;
			}
		} catch (JsonParseException e) {
			e.printStackTrace();
			throw new ReaderException("data parse error", e);
		} catch (RemoteException e) {
			e.printStackTrace();
			throw new ReaderException("remote connection error", e);
		} finally {
			if (in != null) in.close(); // ensure resources get cleaned up timely
		}
	}

	// NOTE: /api/0/stream/contents
	private Reader readStreamContents(long syncTime, long startTime, IItemListHandler handler, String continuation) throws IOException, ReaderException, RemoteException {
		initAuth();

		StringBuilder buff = new StringBuilder();
		buff.append(getApiUrl(URL_API_STREAM_CONTENTS));
		String stream = handler.stream();

		if (stream == null || stream.equals(STATE_READING_LIST)) stream = STATE_GOOGLE_READING_LIST;
		else if (stream.equals(STATE_STARRED)) stream = STATE_GOOGLE_STARRED;
		buff.append("/").append(Utils.encode(stream));

		buff.append("?client=newsplus&ck=").append(syncTime);
		if (handler.excludeRead()) {
			buff.append("&xt=").append(STATE_GOOGLE_READ);
		}
		if (continuation != null && continuation.length() > 0) {
			buff.append("&c=").append(continuation);
		}
		if (startTime > 0) {
			buff.append("&ot=").append(startTime);
		}
		int limit = handler.limit();
		limit = (limit > SYNC_MAX_ITEMS || limit == 0) ? SYNC_MAX_ITEMS : limit;
		if (limit > 0) {
			// google only allows max 1000 at once
			buff.append("&n=").append(limit > SYNC_MAX_ITEMS ? SYNC_MAX_ITEMS : limit);
		}
		buff.append("&r=").append(handler.newestFirst() ? "n" : "o");

		return doGetReader(buff.toString());
	}

	private String parseItemList(Reader in, IItemListHandler handler) throws JsonParseException, IOException, RemoteException {
		JsonFactory f = new JsonFactory();
		JsonParser jp = f.createParser(in);

		long length = 0;
		String currName;
		String mediaUrl = null;
		String mediaType = null;

		IItem entry = null;
		String continuation = null;
		List<IItem> itemList = new ArrayList<IItem>();

		List<String> excludedStreams = handler.excludedStreams(); // excluded subscriptions

		String additionalCategory = null;
		if (handler.stream() != null && handler.stream().contains("itemrecs")) {
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
					if (jp.getCurrentToken() == JsonToken.START_OBJECT) {
						entry = new IItem();
					} else if (jp.getCurrentToken() == JsonToken.END_OBJECT) {
						if (entry != null && entry.uid.length() > 0) {
							if (length + entry.getLength() > MAX_TRANSACTION_LENGTH) {
								handler.items(itemList, STRATEGY_INSERT_DEFAULT);
								itemList.clear();
								length = 0;						
							}

							// add additional category to item
							if (additionalCategory != null) entry.addTag(additionalCategory);
							itemList.add(entry);
							length += entry.getLength();
						}
						
						if (itemList.size() % 200 == 0 || length > MAX_TRANSACTION_LENGTH) {	// avoid TransactionTooLargeException, android only allows 1mb
							handler.items(itemList, STRATEGY_INSERT_DEFAULT);
							itemList.clear();
							length = 0;
						}
						entry = null;
					}

					currName = jp.getCurrentName();
					if (currName == null || entry == null) continue;

					jp.nextToken(); // move to value
					if (currName.equals("id")) {
						entry.uid = stripItemUid(jp.getText());
					} else if (currName.equals("crawlTimeMsec")) {
						entry.updatedTime = Long.valueOf(jp.getText()) / 1000;
					} else if (currName.equals("title")) {
						entry.title = unEscapeEntities(jp.getText());
					} else if (currName.equals("categories")) {
						while (jp.nextToken() != JsonToken.END_ARRAY) {
							String category = jp.getText();
							if (category != null && addUserLabel(category, entry)) {
								entry.addTag(category);
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
								mediaUrl = jp.getText();
							} else if (currName.equals("type")) {
								mediaType = jp.getText();
								if (mediaType.startsWith("image")) {
									entry.addImage(mediaUrl, mediaType);
								} else if (mediaType.startsWith("video")) {
									entry.addVideo(mediaUrl, mediaType);									
								} else if (mediaType.startsWith("audio")) {
									entry.addAudio(mediaUrl, mediaType);									
								}
								
								mediaUrl = null;
								mediaType = null;
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
								if (streamId != null && (excludedStreams == null || !excludedStreams.contains(streamId))) {
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

				handler.items(itemList, STRATEGY_INSERT_DEFAULT);
				itemList.clear();

			} else {
				jp.skipChildren();
			}
		}

		return continuation;
	}

	@Override
	public void handleItemIdList(IItemIdListHandler handler, long syncTime) throws IOException, ReaderException {
		// http://www.google.com/reader/api/0/stream/contents/user%2F-%2Fstate%2Fcom.google%2Fread?client=newsplus&output=json&ck=1276066665822&n=20&r=n
		Reader in = null;
		try {
			in = readStreamItemIds(syncTime, handler);
			parseItemIdList(in, handler);
		} catch (JsonParseException e) {
			e.printStackTrace();
			throw new ReaderException("data parse error", e);
		} catch (RemoteException e) {
			e.printStackTrace();
			throw new ReaderException("remote connection error", e);
		} finally {
			if (in != null) in.close();
		}
	}

	// NOTE: /api/0/stream/items/ids
	private Reader readStreamItemIds(long syncTime, IItemIdListHandler handler) throws IOException, ReaderException, RemoteException {
		initAuth();

		StringBuilder buff = new StringBuilder();
		buff.append(getApiUrl(URL_API_STREAM_ITEM_IDS));
		buff.append("?output=json"); // xml or json
		
		String stream = handler.stream();
		if (stream == null || stream.equals(STATE_READING_LIST)) stream = STATE_GOOGLE_READING_LIST;
		else if (stream.equals(STATE_STARRED)) stream = STATE_GOOGLE_STARRED;
		buff.append("&s=").append(Utils.encode(stream));
		
		if (handler.excludeRead()) {
			buff.append("&xt=").append(STATE_GOOGLE_READ);
		}
		
		int limit = handler.limit();
		if (limit > 0) {
			buff.append("&n=").append(limit);
		}
		
		buff.append("&r=").append(handler.newestFirst() ? "n" : "o");

		return doGetReader(buff.toString());
	}

	private void parseItemIdList(Reader in, IItemIdListHandler handler) throws JsonParseException, IOException, RemoteException {
		JsonFactory f = new JsonFactory();
		JsonParser jp = f.createParser(in);

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
						idList.add(Utils.dec2Hex(jp.getText())); // convert dec to hex
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
	public boolean markAsRead(String[] itemUids, String[] subUids) throws IOException, ReaderException {
		return editItemTag(itemUids, subUids, new String[] { STATE_GOOGLE_READ }, ACTION_ITEM_TAG_ADD_LABEL);
	}

	@Override
	public boolean markAsUnread(String[] itemUids, String[] subUids, boolean keepUnread) throws IOException, ReaderException {
		return editItemTag(itemUids, subUids, new String[] { STATE_GOOGLE_READ }, ACTION_ITEM_TAG_REMOVE_LABEL);
	}

	@Override
	public boolean markAllAsRead(String stream, String title, String[] excludedStreams, long syncTime) throws IOException, ReaderException {
		if (stream == null) {
			stream = STATE_GOOGLE_READING_LIST;
			title = "all";
		}
		String token = initToken();
		List<NameValuePair> params = new ArrayList<NameValuePair>(5);
		params.add(new BasicNameValuePair("T", token));
		params.add(new BasicNameValuePair("s", stream));
		params.add(new BasicNameValuePair("t", title));
		params.add(new BasicNameValuePair("ts", syncTime + "999"));

		Reader in = doPostReader(getApiUrl(URL_API_MARK_ALL_AS_READ), params);
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
	public boolean editItemTag(String[] itemUids, String[] subUids, String[] tags, int action) throws IOException, ReaderException {
		String token = initToken();

		List<NameValuePair> params = new ArrayList<NameValuePair>(5);
		params.add(new BasicNameValuePair("T", token));

		switch (action) {
			case ACTION_ITEM_TAG_ADD_LABEL:
				if (tags != null && tags.length > 0) {
					for (String tag : tags) {
						params.add(new BasicNameValuePair("a", tag));
					}
				}
				break;
			case ACTION_ITEM_TAG_REMOVE_LABEL:
				if (tags != null && tags.length > 0) {
					for (String tag : tags) {
						params.add(new BasicNameValuePair("r", tag));
			}
		}
				break;
			case ACTION_ITEM_TAG_NEW_LABEL:
				if (tags != null && tags.length > 0) {
					String userId = getUserId();
					for (String tag : tags) {
						params.add(new BasicNameValuePair("a", "user/" + userId + "/label/" + tag));
					}
			}
				break;
		}

		if (itemUids != null && itemUids.length > 0) {
			for (int i = 0; i < itemUids.length; i++) {
				params.add(new BasicNameValuePair("i", getItemUid(itemUids[i])));
				params.add(new BasicNameValuePair("s", subUids[i]));
			}
		}

		params.add(new BasicNameValuePair("async", "true"));

		Reader in = doPostReader(getApiUrl(URL_API_EDIT_TAG), params);
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
	public boolean editSubscription(String uid, String title, String url, String[] tags, int action) throws IOException, ReaderException {
		String token = initToken();
		List<NameValuePair> params = new ArrayList<NameValuePair>(4);
		params.add(new BasicNameValuePair("T", token));
		if (title != null) {
			params.add(new BasicNameValuePair("t", title));
		}
		switch (action) {
			case ACTION_SUBSCRIPTION_EDIT:
				params.add(new BasicNameValuePair("ac", "edit"));
				break;
			case ReaderExtension.ACTION_SUBSCRIPTION_NEW_LABEL:
				params.add(new BasicNameValuePair("ac", "edit"));
				String userId = getUserId();
				for (String tag : tags) {
					params.add(new BasicNameValuePair("a", "user/" + userId + "/label/" + tag));
				}
				break;
			case ACTION_SUBSCRIPTION_ADD_LABEL:
				params.add(new BasicNameValuePair("ac", "edit"));
				for (String tag : tags) {
					params.add(new BasicNameValuePair("a", tag));
				}
				break;
			case ACTION_SUBSCRIPTION_REMOVE_LABEL:
				params.add(new BasicNameValuePair("ac", "edit"));
				for (String tag : tags) {
					params.add(new BasicNameValuePair("r", tag));
				}
				break;
			case ACTION_SUBSCRIPTION_SUBCRIBE:
				params.add(new BasicNameValuePair("ac", "subscribe"));
				break;
			case ACTION_SUBSCRIPTION_UNSUBCRIBE:
				params.add(new BasicNameValuePair("ac", "unsubscribe"));
				break;
		}
		
		String s = uid != null ? uid : "feed/" + url;
		params.add(new BasicNameValuePair("s", s));

		Reader in = doPostReader(getApiUrl(URL_API_SUBSCIPTION), params);
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
		params.add(new BasicNameValuePair("dest", getTagUidPath(tagUid) + newLabel));

		Reader in = doPostReader(getApiUrl(URL_API_RENAME_TAG), params);
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

		Reader in = doPostReader(getApiUrl(URL_API_DISABLE_TAG), params);
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
