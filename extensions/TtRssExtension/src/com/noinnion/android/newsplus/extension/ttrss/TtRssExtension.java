package com.noinnion.android.newsplus.extension.ttrss;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.auth.AuthenticationException;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.noinnion.android.newsplus.extension.ttrss.utils.HttpUtils;
import com.noinnion.android.newsplus.extension.ttrss.utils.StringUtils;
import com.noinnion.android.newsplus.extension.ttrss.utils.Utils;
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

public class TtRssExtension extends ReaderExtension {

	private final String		TAG					= TtRssExtension.class.getSimpleName();

	public static final String	URL_API_			= "api/";

	private static final int	API_STATUS_OK		= 0;
//	private static final int	API_STATUS_ERR		= 1;

	private static final int	CAT_SPECIAL			= -1;
	private static final int	CAT_LABELS			= -2;

	private static final int	FEED_ALL_ITEMS		= -4;
	private static final int	FEED_STARRED		= -1;

	private static final int	MAX_SYNC_ITEMS		= 60;									// api_level <= 5
	private static final int	MAX_SYNC_ITEMS_V6	= 200;									// api_level = 6

	private String				mServer;
	private String				mHttpUsername;
	private String				mHttpPassword;
	private String				mSessionId;

	private int					mSavedFirstId 		= 0;

	private int getMaxItemSync() {
		final Context c = getApplicationContext();
		int api = Prefs.getApiLevel(c);
		if (api >= 6) return MAX_SYNC_ITEMS_V6;
		else return MAX_SYNC_ITEMS;
	}

	private String getServer() {
		if (mServer == null) {
			final Context c = getApplicationContext();
			mServer = Prefs.getServer(c) + URL_API_;
		}
		return mServer;
	}

	private String getHttpUsername() {
		final Context c = getApplicationContext();
		if (mHttpUsername == null) mHttpUsername = Prefs.getHttpUsername(c);
		if (mHttpUsername == null) mHttpUsername = ""; // indicatate initialized
		return mHttpUsername;
	}

	private String getHttpPassword() {
		final Context c = getApplicationContext();
		if (mHttpPassword == null) mHttpPassword = Prefs.getHttpPassword(c);
		if (mHttpPassword == null) mHttpPassword = ""; // indicatate initialized
		return mHttpPassword;
	}

	private String getSessionId() {
		final Context c = getApplicationContext();
		if (mSessionId == null) mSessionId = Prefs.getSessionId(c);
		return mSessionId;
	}

	protected DefaultHttpClient	client;

	public static final int		SOCKET_OPERATION_TIMEOUT	= 20000;	// 20s

	public DefaultHttpClient getClient() {
		if (client == null) client = HttpUtils.createHttpClient();
		return client;
	}

	public static DefaultHttpClient createHttpClient() {
		try {
			HttpParams params = new BasicHttpParams();

			// Turn off stale checking. Our connections break all the time anyway,
			// and it's not worth it to pay the penalty of checking every time.
			HttpConnectionParams.setStaleCheckingEnabled(params, false);

			// Set the timeout in milliseconds until a connection is established. The default value is zero, that means the timeout is not used.
			HttpConnectionParams.setConnectionTimeout(params, SOCKET_OPERATION_TIMEOUT);
			// Set the default socket timeout (SO_TIMEOUT) in milliseconds which is the timeout for waiting for data.
			HttpConnectionParams.setSoTimeout(params, SOCKET_OPERATION_TIMEOUT);

			HttpConnectionParams.setSocketBufferSize(params, 8192);

			// Don't handle redirects -- return them to the caller. Our code
			// often wants to re-POST after a redirect, which we must do ourselves.
	        HttpClientParams.setRedirecting(params, false);

//	        HttpProtocolParams.setUserAgent(params, userAgent);
			HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
			HttpProtocolParams.setContentCharset(params, HTTP.UTF_8);
		    HttpProtocolParams.setUseExpectContinue(params, true);
		        
			// ssl
			KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
			trustStore.load(null, null);

			SSLSocketFactory sf = new MySSLSocketFactory(trustStore);
			sf.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);

			SchemeRegistry registry = new SchemeRegistry();
			registry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
			registry.register(new Scheme("https", sf, 443));

			ClientConnectionManager ccm = new ThreadSafeClientConnManager(params, registry);
			
			return new DefaultHttpClient(ccm, params);
		} catch (Exception e) {
			return HttpUtils.createHttpClient();
		}
	}

	public InputStream doPostInputStream(JSONObject jo) throws IOException, ReaderException {
		HttpPost post = new HttpPost(getServer());
		httpAuthentication(post);
		
		StringEntity postEntity = new StringEntity(jo.toString(), HTTP.UTF_8);
		postEntity.setContentType("application/json");
		post.setEntity(postEntity);

		// gzip
		post.setHeader("Accept-Encoding", "gzip, deflate");

		HttpResponse response = getClient().execute(post);

		int resStatus = response.getStatusLine().getStatusCode();
		if (resStatus != HttpStatus.SC_OK) {
			throw new ReaderException("Invalid http status " + resStatus + ": " + post.getURI());
		}

		final HttpEntity entity = response.getEntity();
		if (entity == null) {
			throw new ReaderException("null response entity");
		}

		InputStream is = null;

		// create the appropriate stream wrapper based on the encoding type
		String encoding = HttpUtils.getHeaderValue(entity.getContentEncoding());
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

	public boolean doRequest(JSONObject jo) throws IOException, JSONException, ReaderException  {
		String json = Utils.convertStreamToString(doPostInputStream(jo));

		JSONObject resJsonObject = new JSONObject(json);
		if (resJsonObject != null) {
			if (resJsonObject.has("error")) {
				String error = resJsonObject.getString("error");
				if (error != null) handleError(error);
			}
			
			if (resJsonObject.has("content")) {				
				JSONObject contentJo = resJsonObject.getJSONObject("content");
				if (contentJo.has("status")) {
					String status = contentJo.getString("status");
					if (status != null && status.equals("OK")) return true;
				}
			}
			
		}

		return false;
	}
	
	public static String unEscapeEntities(String text) {
//		return StringEscapeUtils.unescapeHtml4(text);
		if (text == null) return "";
		return text.replace("&amp;", "&").replace("&#39;", "'").replace("&quot;", "\"");
//		return text.replace("&lt;", "<").replace("&gt;", ">").replace("&amp;", "&").replace("&#39;", "'").replace("&quot;", "\"");
	}

	public void httpAuthentication(HttpPost post) throws ReaderException {
		String httpUsername = getHttpUsername();
		String httpPassword = getHttpPassword();
		if (!TextUtils.isEmpty(httpUsername) && !TextUtils.isEmpty(httpPassword)) {
			try {
				UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(httpUsername, httpPassword);
				BasicScheme scheme = new BasicScheme();
				Header authorizationHeader;
				authorizationHeader = scheme.authenticate(credentials, post);
				post.addHeader(authorizationHeader);
			} catch (AuthenticationException e) {
				e.printStackTrace();
				throw new ReaderException("http auth: " + e.getLocalizedMessage());
			}
		}
	}

	boolean hasReAuthenthicated = false;

	private void login() throws ClientProtocolException, IOException, AuthenticationException, JSONException, ReaderLoginException {
		hasReAuthenthicated = true;

		final Context c = getApplicationContext();

		String	server = getServer();
		String	username = Prefs.getUsername(c);
		String	password = Prefs.getPassword(c);
		String	httpUsername = Prefs.getHttpUsername(c);
		String	httpPassword = Prefs.getHttpPassword(c);

		HttpPost post = new HttpPost(server);

		if (!TextUtils.isEmpty(httpUsername) && !TextUtils.isEmpty(httpPassword)) {
			UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(httpUsername, httpPassword);
			BasicScheme scheme = new BasicScheme();
			Header authorizationHeader = scheme.authenticate(credentials, post);
			post.addHeader(authorizationHeader);
		}

		JSONObject jo = new JSONObject();
		jo.put("op", "login");
		jo.put("user", username);
		jo.put("password", password);

		StringEntity postEntity = new StringEntity(jo.toString(), HTTP.UTF_8);
		postEntity.setContentType("application/json");
		post.setEntity(postEntity);

		DefaultHttpClient client = TtRssExtension.createHttpClient();
		HttpResponse response = client.execute(post);

		final HttpEntity entity = response.getEntity();
		InputStream is = new FilterInputStream(entity.getContent()) {
			@Override
			public void close() throws IOException {
				super.close();
				entity.consumeContent();
			}
		};
		String content = Utils.convertStreamToString(is);

		JSONObject resJsonObject = new JSONObject(content);
		if (resJsonObject != null) {
			JSONObject contentObject = resJsonObject.getJSONObject("content");
			if (contentObject != null) {
				// handle error
				if (contentObject.has("error")) {
					throw new ReaderLoginException(contentObject.getString("error"));
				}

				if (contentObject.has("session_id")) {
					mSessionId = contentObject.getString("session_id");
					Prefs.setSessionId(c, mSessionId);

					if (contentObject.has("api_level")) {
						Prefs.setApiLevel(c, contentObject.getInt("api_level"));
					}
				}
			}
		}

	}

	private String getError(JsonParser jp) throws JsonParseException, IOException {
		if (jp == null) return null;
		String currName;
		while (jp.nextToken() != JsonToken.END_OBJECT) {
			currName = jp.getCurrentName();
			if (currName == null) continue;

			jp.nextToken();
			if (currName.equals("content")) {
				while (jp.nextToken() != JsonToken.END_OBJECT) {
					currName = jp.getCurrentName();
					if (currName == null) continue;

					jp.nextToken();
					if (currName.equals("error")) {
						return unEscapeEntities(jp.getText());
					} else {
						jp.skipChildren();
					}
				}
			} else {
				jp.skipChildren();
			}
		}
		return null;
	}

	private void handleError(String error) throws ReaderException {
		if (error != null && error.equals("NOT_LOGGED_IN")) {
			throw new ReaderLoginException("NOT_LOGGED_IN");
		} else {
			throw new ReaderException(error);
		}
	}
	
	@Override
	public void handleReaderList(ITagListHandler tagHandler, ISubscriptionListHandler subHandler, long syncTime) throws IOException, ReaderException {
		Reader in = null;
		try {
			in = readReaderList(syncTime);
			parseReaderList(in, tagHandler, subHandler, getFeedList(syncTime));
		} catch (ReaderLoginException e) {
			if (hasReAuthenthicated) throw e;
			try {
				login();
				handleReaderList(tagHandler, subHandler, syncTime);
			} catch (Exception ex) {
				ex.printStackTrace();
				throw new ReaderLoginException(e.getLocalizedMessage());
			}
		} catch (JsonParseException e) {
			throw new ReaderException("data parse error", e);
		} catch (JSONException e) {
			throw new ReaderException("data parse error", e);
		} catch (RemoteException e) {
			throw new ReaderException("remote connection error", e);
		} finally {
			if (in != null) in.close();
		}
	}

	private Reader readReaderList(long syncTime) throws IOException, ReaderException, JSONException {
		JSONObject jo = new JSONObject();
		jo.put("sid", getSessionId());
		jo.put("op", "getFeedTree");

		InputStream in = doPostInputStream(jo);
		return new InputStreamReader(in, HTTP.UTF_8);
	}

	private void parseReaderList(Reader in, ITagListHandler tagHandler, ISubscriptionListHandler subHandler, Map<String, ISubscription> feedList) throws IOException, ReaderException, RemoteException {
		JsonFactory f = new JsonFactory();
		JsonParser jp = f.createParser(in);

		String currName;
		String currCategoryId = null;

		ISubscription sub = null;
		ITag tag = null;

		List<ITag> tags = new ArrayList<ITag>();
		List<ISubscription> feeds = new ArrayList<ISubscription>();

		jp.nextToken(); // will return JsonToken.START_OBJECT (verify?)
		while (jp.nextToken() != JsonToken.END_OBJECT) {
			currName = jp.getCurrentName();
			if (currName == null) continue;

			jp.nextToken(); // move to value, or START_OBJECT/START_ARRAY
			if (currName.equals("status")) { // contains an object
				int status = jp.getIntValue();
				if (status != API_STATUS_OK) handleError(getError(jp));
			} else if (currName.equals("content")) {

				while (jp.nextToken() != JsonToken.END_OBJECT) {
					currName = jp.getCurrentName();
					if (currName == null) continue;

					jp.nextToken();
					if (currName.equals("categories")) {

						while (jp.nextToken() != JsonToken.END_OBJECT) {
							currName = jp.getCurrentName();
							if (currName == null) continue;

							jp.nextToken();

							if (currName.equals("items")) {

								// start items
								while (jp.nextToken() != JsonToken.END_ARRAY) {
									if (jp.getCurrentToken() == JsonToken.START_OBJECT) {
										tag = new ITag();
									} else if (jp.getCurrentToken() == JsonToken.END_OBJECT) {
										if (tag != null) {
											if (tag.uid.startsWith("CAT:")) {
												if (tag.id == CAT_LABELS) {
													tag.id = 0; // reset temp id (used for bare_id)
												} else if (tag.id != CAT_SPECIAL) {
													tag.id = 0; // reset temp id (used for bare_id)
													tags.add(tag);
												}
											} else if (tag.uid.startsWith("FEED:")) {
												if (tag.id == FEED_STARRED) {
													tag.id = 0; // reset temp id (used for bare_id)
													tags.add(tag);
												} else if (tag.id > 0) {
													sub = new ISubscription();
													sub.uid = tag.uid;
													sub.title = tag.label;
													
													ISubscription temp = feedList.get(String.valueOf(tag.id));
													if (temp != null) {
														sub.htmlUrl = temp.htmlUrl;
														sub.newestItemTime = temp.newestItemTime;
													}
													feeds.add(sub);
													sub = null;
												}
											}
										}
										tag = null;
									}

									currName = jp.getCurrentName();
									if (currName == null || tag == null) continue;

									jp.nextToken();
									if (currName.equals("id")) {
										tag.uid = currCategoryId = jp.getText();
									} else if (currName.equals("name")) {
										tag.label = unEscapeEntities(jp.getText());
									} else if (currName.equals("bare_id")) {
										tag.id = jp.getIntValue();
									} else if (currName.equals("items")) {
										while (jp.nextToken() != JsonToken.END_ARRAY) {
											if (jp.getCurrentToken() == JsonToken.START_OBJECT) {
												sub = new ISubscription();
											} else if (jp.getCurrentToken() == JsonToken.END_OBJECT) {
												if (sub != null) {
													if (sub.id < -10) {
														// labels
														ITag labelTag = new ITag();
														labelTag.label = sub.title;
														labelTag.type = ITag.TYPE_TAG_LABEL;
														labelTag.uid = sub.uid;
														tags.add(labelTag);
													} else {
														feeds.add(sub);
													}
												}
												sub = null;
											}

											currName = jp.getCurrentName();
											if (currName == null || sub == null) continue;

											jp.nextToken();
											if (currName.equals("id")) {
												sub.uid = jp.getText();
												sub.addTag(currCategoryId);
											} else if (currName.equals("name")) {
												sub.title = unEscapeEntities(jp.getText());
											} else if (currName.equals("bare_id")) {
												int id = jp.getIntValue();
												if (id == FEED_STARRED) {
													ITag starredTag = new ITag();
													starredTag.label = "Starred articles";
													starredTag.type = ITag.TYPE_TAG_STARRED;
													starredTag.uid = "FEED:" + FEED_STARRED;
													tags.add(starredTag);
												} else if (id > 0) {
													ISubscription temp = feedList.get(String.valueOf(id));
													if (temp != null) {
														sub.htmlUrl = temp.htmlUrl;
														sub.newestItemTime = temp.newestItemTime;
													}
												} else if (id < -10) {
													// labels
													sub.id = id;
												} else {
													sub = null;
												}
											} else {
												jp.skipChildren();
											}
										}
									} else {
										jp.skipChildren();
									}
								}
							} else {
								jp.skipChildren();
							}
						}
					} else {
						jp.skipChildren();
					}
				}

			} else {
				jp.skipChildren();
			}
		}

		tagHandler.tags(tags);
		subHandler.subscriptions(feeds);
	}

	// update html url of feeds
	private Map<String, ISubscription> getFeedList(long syncTime) {
		Reader in = null;
		try {
			in = readFeedList(syncTime);
			Map<String, ISubscription> subs = parseFeedList(in);
			return subs;
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (in != null) in.close();
			} catch (IOException e) {}
		}
		return null;
	}

	private Reader readFeedList(long syncTime) throws IOException, ReaderException, JSONException {
		JSONObject jo = new JSONObject();
		jo.put("sid", getSessionId());
		jo.put("op", "getFeeds");

		jo.put("cat_id", -3); // All feeds, excluding virtual feeds (e.g. Labels and such)
		jo.put("unread_only", false);

		InputStream in = doPostInputStream(jo);
		return new InputStreamReader(in, HTTP.UTF_8);
	}

	private Map<String, ISubscription> parseFeedList(Reader in) throws IOException, JSONException, ReaderException {
		JsonFactory f = new JsonFactory();
		JsonParser jp = f.createParser(in);

		String currName;

		ISubscription sub = null;
		Map<String, ISubscription> feeds = new HashMap<String, ISubscription>();

		jp.nextToken(); // will return JsonToken.START_OBJECT (verify?)
		while (jp.nextToken() != JsonToken.END_OBJECT) {
			currName = jp.getCurrentName();
			jp.nextToken(); // move to value, or START_OBJECT/START_ARRAY
			if (currName.equals("status")) { // contains an object
				int status = jp.getIntValue();
				if (status != API_STATUS_OK) handleError(getError(jp));
			} else if (currName.equals("content")) { // contains an object
				// start items
				while (jp.nextToken() != JsonToken.END_ARRAY) {
					if (jp.getCurrentToken() == JsonToken.START_OBJECT) {
						sub = new ISubscription();
					} else if (jp.getCurrentToken() == JsonToken.END_OBJECT) {
						feeds.put(sub.uid, sub);
						sub = null;
					}

					currName = jp.getCurrentName();
					if (currName == null) continue;
					jp.nextToken();
					if (currName.equals("id")) {
						sub.uid = jp.getText();
					} else if (currName.equals("feed_url")) {
						sub.htmlUrl = jp.getText();
					} else if (currName.equals("last_updated")) {
//						sub.newestItemTime = jp.getLongValue();
					} else {
						jp.skipChildren();
					}
				}

			} else {
				jp.skipChildren();
			}
		}

		return feeds;
	}

	@Override
	public void handleItemList(final IItemListHandler handler, long syncTime) throws IOException, ReaderException {
		final Context c = getApplicationContext();

		int count = 0;
		int lastCount = 0;

		Reader in = null;
		try {
			boolean readingList = handler.stream().equals(ReaderExtension.STATE_READING_LIST);
			long startTime = handler.startTime();
			int sinceId = startTime > 0 ? Prefs.getSinceId(c) : 0;

			in = readStreamContents(handler, 0, sinceId);
			lastCount = parseItemList(in, handler, readingList);
			count = lastCount;

			int maxSync = getMaxItemSync();

			int limit = handler.limit();
			int max = limit > maxSync ? maxSync : limit;
			int index = 1; // continuation count

			while ((limit == 0 || limit > max * index) && handler.continuation() && lastCount == maxSync) {
				int skip = count;
				in.close();
				in = readStreamContents(handler, skip, sinceId);
				lastCount = parseItemList(in, handler, readingList);
				count += lastCount;
				index++;
			}

			if (mSavedFirstId > 0) Prefs.setSinceId(c, mSavedFirstId);
		} catch (ReaderLoginException e) {
			if (hasReAuthenthicated) throw e;
			try {
				login();
				handleItemList(handler, syncTime);
			} catch (Exception ex) {
				ex.printStackTrace();
				throw new ReaderLoginException(e.getLocalizedMessage());
			}
		} catch (JSONException e) {
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
	private Reader readStreamContents(IItemListHandler handler, int skip, int sinceId) throws IOException, ReaderException, JSONException, RemoteException {
		JSONObject jo = new JSONObject();
		jo.put("sid", getSessionId());
		jo.put("op", "getHeadlines");

		String uid = handler.stream();
		if (uid.equals(ReaderExtension.STATE_READING_LIST)) jo.put("feed_id", FEED_ALL_ITEMS);
		else if (uid.equals(ReaderExtension.STATE_STARRED)) jo.put("feed_id", FEED_STARRED);
		else if (uid.equals("FEED:-1")) jo.put("feed_id", FEED_STARRED);
		else if (uid.startsWith("CAT")) {
			jo.put("is_cat", true);
			jo.put("feed_id", Integer.valueOf(uid.substring("CAT:".length())));
		} else if (uid.startsWith("FEED")) {
			jo.put("is_cat", false);
			jo.put("feed_id", Integer.valueOf(uid.substring("FEED:".length())));
		}
		jo.put("limit", handler.limit());
		jo.put("view_mode", handler.excludeRead() ? "unread" : "all_items");
		jo.put("show_content", true);
		jo.put("include_nested", true);
		jo.put("include_attachments", true);
		jo.put("order_by", handler.newestFirst() ? "feed_dates" : "date_reverse");

		if (skip > 0) jo.put("skip", skip);
		if (sinceId > 0) jo.put("since_id", sinceId);

		InputStream in = doPostInputStream(jo);
		return new InputStreamReader(in, HTTP.UTF_8);
	}

	private int parseItemList(Reader in, IItemListHandler handler, boolean saveSinceId) throws IOException, JSONException, ReaderException, RemoteException {
		if (in == null) return 0;
		long length = 0;

		JsonFactory f = new JsonFactory();
		JsonParser jp = f.createParser(in);

		String currName;
		String mediaUrl = null;
		String mediaType = null;
		
		int count = 0;
		IItem item = null;
		List<IItem> items = new ArrayList<IItem>();

		String additionalCategory = null;
		String uid = handler.stream();
		if (uid != null && uid.startsWith("FEED:")) {
			if (Integer.valueOf(uid.substring("FEED:".length())) < 10)
			additionalCategory = uid;
		}

		List<String> excludedStreams = handler.excludedStreams(); // excluded subscriptions

		jp.nextToken();
		while (jp.nextToken() != JsonToken.END_OBJECT) {
			currName = jp.getCurrentName();
			jp.nextToken();
			if (currName.equals("status")) {
				int status = jp.getIntValue();
				if (status != API_STATUS_OK) handleError(getError(jp));
			} else if (currName.equals("content")) { // contains an object
				// start items
				while (jp.nextToken() != JsonToken.END_ARRAY) {
					if (jp.getCurrentToken() == JsonToken.START_OBJECT) {
						item = new IItem();
					} else if (jp.getCurrentToken() == JsonToken.END_OBJECT) {
						if (item != null && item.uid.length() > 0) {
							
							if (length + item.getLength() > MAX_TRANSACTION_LENGTH) {
								handler.items(items, STRATEGY_INSERT_DEFAULT);
								items.clear();
								length = 0;						
							}
							
							if (additionalCategory != null) item.addTag(additionalCategory);
							items.add(item);
							count++;
							length += item.getLength();
						}
						
						if (items.size() % 200 == 0 || length > MAX_TRANSACTION_LENGTH) {	// avoid TransactionTooLargeException, android only allows 1mb
							handler.items(items, STRATEGY_INSERT_DEFAULT);
							items.clear();
							length = 0;
						}
						item = null;
					}

					currName = jp.getCurrentName();
					if (currName == null || item == null) continue;

					jp.nextToken();
					if (currName.equals("id")) {
						item.uid = jp.getText();

						// save for since id
						if (saveSinceId) {
							int id = Integer.valueOf(item.uid);
							if (id > mSavedFirstId) mSavedFirstId = id;
						}
					} else if (currName.equals("feed_id")) {
						item.subUid = "FEED:" + jp.getText();
						if (excludedStreams != null && excludedStreams.contains(item.subUid)) {
							item = null;
						}
					} else if (currName.equals("unread")) {
						item.read = !jp.getBooleanValue();
					} else if (currName.equals("marked")) {
						item.starred = jp.getBooleanValue();
						if (item.starred) item.addTag("FEED:" + FEED_STARRED);
					} else if (currName.equals("title")) {
						item.title = unEscapeEntities(jp.getText());
					} else if (currName.equals("link")) {
						item.link = jp.getText();
					} else if (currName.equals("content")) {
						item.content = unEscapeEntities(jp.getText());
					} else if (currName.equals("author")) {
						item.author = unEscapeEntities(jp.getText());
					} else if (currName.equals("updated")) {
						item.publishedTime = item.updatedTime = jp.getIntValue();
					} else if (currName.equals("attachments")) {

						while (jp.nextToken() != JsonToken.END_ARRAY) {
							currName = jp.getCurrentName();
//							if (currName == null || (item.media != null && item.mediaType != null)) continue;
							if (currName == null) continue;

							jp.nextToken();
							if (currName.equals("content_url")) {
								mediaUrl = jp.getText();
							} else if (currName.equals("content_type")) {
								mediaType = jp.getText();
								if (mediaType.startsWith("image")) {
									item.addImage(mediaUrl, mediaType);
								} else if (mediaType.startsWith("video")) {
									item.addVideo(mediaUrl, mediaType);									
								} else if (mediaType.startsWith("audio")) {
									item.addAudio(mediaUrl, mediaType);									
								}
								
								mediaUrl = null;
								mediaType = null;
							} else {
								jp.skipChildren();
							}
						}

					} else {
						jp.skipChildren();
					}
				}

			} else {
				jp.skipChildren();
			}
		}

		handler.items(items, STRATEGY_INSERT_DEFAULT);
		items.clear();

		return count;
	}

	@Override
	public void handleItemIdList(IItemIdListHandler handler, long syncTime) throws IOException, ReaderException {
		Reader in = null;
		try {
			in = readStreamIds(handler);
			parseItemIdList(in, handler);
		} catch (ReaderLoginException e) {
			if (hasReAuthenthicated) throw e;
			try {
				login();
				handleItemIdList(handler, syncTime);
			} catch (Exception ex) {
				ex.printStackTrace();
				throw new ReaderLoginException(e.getLocalizedMessage());
			}
		} catch (JSONException e) {
			e.printStackTrace();
		} catch (RemoteException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (in != null) in.close(); // ensure resources get cleaned up timely
		}
	}

	// NOTE: /api/0/stream/contents
	private Reader readStreamIds(IItemIdListHandler handler) throws IOException, ReaderException, JSONException, RemoteException {
		JSONObject jo = new JSONObject();
		jo.put("sid", getSessionId());
		jo.put("op", "getCompactHeadlines");

		String uid = handler.stream();
		if (uid.equals(ReaderExtension.STATE_READING_LIST)) jo.put("feed_id", FEED_ALL_ITEMS);
		else if (uid.equals(ReaderExtension.STATE_STARRED)) jo.put("feed_id", FEED_STARRED);
		else if (uid.equals("FEED:-1")) jo.put("feed_id", FEED_STARRED);
		else if (uid.startsWith("CAT")) {
			jo.put("is_cat", true);
			jo.put("feed_id", Integer.valueOf(uid.substring("CAT:".length())));
		} else if (uid.startsWith("FEED")) {
			jo.put("is_cat", false);
			jo.put("feed_id", Integer.valueOf(uid.substring("FEED:".length())));
		}
		jo.put("limit", handler.limit());
		jo.put("view_mode", handler.excludeRead() ? "unread" : "all_items");
		jo.put("order_by", handler.newestFirst() ? "feed_dates" : "date_reverse");

		InputStream in = doPostInputStream(jo);
		return new InputStreamReader(in, HTTP.UTF_8);
	}

	private void parseItemIdList(Reader in, IItemIdListHandler handler) throws IOException, JSONException, ReaderException, RemoteException {
		if (in == null) return;

		JsonFactory f = new JsonFactory();
		JsonParser jp = f.createParser(in);

		String currName;

		String id = null;
		List<String> items = new ArrayList<String>();

		jp.nextToken();
		while (jp.nextToken() != JsonToken.END_OBJECT) {
			currName = jp.getCurrentName();
			jp.nextToken();
			if (currName.equals("status")) {
				int status = jp.getIntValue();
				if (status != API_STATUS_OK) {
					// handle unknown method exception
					String error = getError(jp);
					if (error != null && error.equals("UNKNOWN_METHOD")) {
						Log.w(TAG, "UNKNOWN_METHOD: getCompactHeadlines");
						return;
					}
					else handleError(error);
				}
			} else if (currName.equals("content")) { // contains an object
				// start items
				while (jp.nextToken() != JsonToken.END_ARRAY) {
					if (jp.getCurrentToken() == JsonToken.START_OBJECT) {
					} else if (jp.getCurrentToken() == JsonToken.END_OBJECT) {
						if (id != null && id.length() > 0) {
							items.add(id);
						}
						id = null;
					}

					currName = jp.getCurrentName();
					if (currName == null) continue;

					jp.nextToken();
					if (currName.equals("id")) {
						id = jp.getText();
					} else {
						jp.skipChildren();
					}
				}

			} else {
				jp.skipChildren();
			}
		}

		handler.items(items);
		items.clear();
	}

	@Override
	public boolean markAsRead(String[] itemUids, String[] subUids) throws IOException, ReaderException {
		try {
			JSONObject jo = new JSONObject();

			jo.put("sid", getSessionId());
			jo.put("op", "updateArticle");

			jo.put("article_ids", StringUtils.implode(itemUids, ","));
			jo.put("mode", 0);
			jo.put("field", 2);

			return doRequest(jo);

		} catch (ReaderLoginException e) {
			if (hasReAuthenthicated) throw e;
			try {
				login();
				markAsRead(itemUids, subUids);
			} catch (Exception ex) {
				ex.printStackTrace();
				throw new ReaderLoginException(e.getLocalizedMessage());
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}

		return false;
	}

	@Override
	public boolean markAsUnread(String[] itemUids, String[] subUids, boolean keepUnread) throws IOException, ReaderException {
		try {
			JSONObject jo = new JSONObject();

			jo.put("sid", getSessionId());
			jo.put("op", "updateArticle");

			jo.put("article_ids", StringUtils.implode(itemUids, ","));
			jo.put("mode", 1);
			jo.put("field", 2);

			return doRequest(jo);

		} catch (ReaderLoginException e) {
			if (hasReAuthenthicated) throw e;
			try {
				login();
				markAsUnread(itemUids, subUids, keepUnread);
			} catch (Exception ex) {
				ex.printStackTrace();
				throw new ReaderLoginException(e.getLocalizedMessage());
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}

		return false;
	}

	@Override
	public boolean markAllAsRead(String stream, String title, String[] excludedStreams, long syncTime) throws IOException, ReaderException {
		try {
			JSONObject jo = new JSONObject();
			jo.put("sid", getSessionId());
			jo.put("op", "catchupFeed");

			if (stream == null) stream = STATE_READING_LIST;

			if (stream.equals(ReaderExtension.STATE_READING_LIST)) jo.put("feed_id", FEED_ALL_ITEMS);
			else if (stream.equals(ReaderExtension.STATE_STARRED)) jo.put("feed_id", FEED_STARRED);
			else if (stream.startsWith("CAT")) {
				jo.put("feed_id", Integer.valueOf(stream.substring("CAT:".length())));
				jo.put("is_cat", true);
			} else if (stream.startsWith("FEED")) {
				jo.put("feed_id", Integer.valueOf(stream.substring("FEED:".length())));
				jo.put("is_cat", false);
			}

			return doRequest(jo);

		} catch (ReaderLoginException e) {
			if (hasReAuthenthicated) throw e;
			try {
				login();
				markAllAsRead(stream, title, excludedStreams, syncTime);
			} catch (Exception ex) {
				ex.printStackTrace();
				throw new ReaderLoginException(e.getLocalizedMessage());
			}
		} catch (JSONException e) {
			e.printStackTrace();
			throw new ReaderException("connection error", e);
		}

		return false;
	}

	@Override
	public boolean editItemTag(String[] itemUids, String[] subUids, String[] tags, int action) throws IOException, ReaderException {
		boolean success = true;

		switch (action) {
			case ACTION_ITEM_TAG_ADD_LABEL:
				if (tags != null && tags.length > 0) {
					for (String tag : tags) {
						if (tag.endsWith("FEED:-1")) {
							success = updateArticle(itemUids, 0, true);
						} else {
							success = setArticleLabel(itemUids, Integer.valueOf(tag.replace("FEED:", "")), true);					
						};
					}
				}
				break;
			case ACTION_ITEM_TAG_REMOVE_LABEL:
				if (tags != null && tags.length > 0) {
					for (String tag : tags) {
						if (tag.endsWith("FEED:-1")) {
							success = updateArticle(itemUids, 0, false);
						} else {
							success = setArticleLabel(itemUids, Integer.valueOf(tag.replace("FEED:", "")), false);					
						};
					}
				}
				break;
			case ACTION_ITEM_TAG_NEW_LABEL:
				Log.w(TAG, "newLabel: not implemented");
				success = false;
//				if (tags != null && tags.length > 0) {
//					for (String tag : tags) {
//						
//					}
//				}
				break;
		}
		
		return success;
	}
	
	/*
	 * updateArticle
	 * article_ids (comma-separated list of integers) - article IDs to operate on
	 * mode (integer) - type of operation to perform (0 - set to false, 1 - set to true, 2 - toggle)
	 * field (integer) - field to operate on (0 - starred, 1 - published, 2 - unread, 3 - article note since api level 1)
	 * data (string) - optional data parameter when setting note field (since api level 1)
	 *  
	 */
	public boolean updateArticle(String[] itemUids, int field, boolean mode) throws IOException, ReaderException {
		try {
			JSONObject jo = new JSONObject();
			jo.put("sid", getSessionId());
			jo.put("op", "updateArticle");
	
			jo.put("article_ids", StringUtils.implode(itemUids, ","));
			
			jo.put("mode", mode ? 1 : 0); // set false
			jo.put("field", field); // starred
	
			return doRequest(jo);

		} catch (ReaderLoginException e) {
			if (hasReAuthenthicated) throw e;
			try {
				login();
				updateArticle(itemUids, field, mode);
			} catch (Exception ex) {
				ex.printStackTrace();
				throw new ReaderLoginException(e.getLocalizedMessage());
			}			
		} catch (JSONException e) {
			throw new ReaderException("connection error", e);			
		}
		
		return false;
	}
	
	/*
	 * updateArticle
	 * article_ids - comma-separated list of article ids
	 * label_id (int) - label id, as returned in getLabels
	 * assign (boolean) - assign or remove label
	 *  
	 */
	public boolean setArticleLabel(String[] itemUids, int labelId, boolean assign) throws IOException, ReaderException {
		try {
			JSONObject jo = new JSONObject();
			jo.put("sid", getSessionId());
			jo.put("op", "setArticleLabel");
	
			jo.put("article_ids", StringUtils.implode(itemUids, ","));
			
			jo.put("label_id", labelId); // set false
			jo.put("assign", assign); // starred
			
			return doRequest(jo);

		} catch (ReaderLoginException e) {
			if (hasReAuthenthicated) throw e;
			try {
				login();
				setArticleLabel(itemUids, labelId, assign);
			} catch (Exception ex) {
				ex.printStackTrace();
				throw new ReaderLoginException(e.getLocalizedMessage());
			}			
		} catch (JSONException e) {
			throw new ReaderException("connection error", e);			
		}

		return false;
	}
	
	@Override
	public boolean editSubscription(String uid, String title, String url, String[] tags, int action) throws IOException, ReaderException {
		switch (action) {
			case ReaderExtension.ACTION_SUBSCRIPTION_EDIT:
				// no api
				Log.w(TAG, "editSubscription: ACTION_EDIT : no api");
				break;
			case ReaderExtension.ACTION_SUBSCRIPTION_NEW_LABEL:
				// no api
				Log.w(TAG, "editSubscription: ACTION_NEW_LABEL : no api");
				break;
			case ReaderExtension.ACTION_SUBSCRIPTION_ADD_LABEL:
				// no api
				Log.w(TAG, "editSubscription: ACTION_ADD_LABEL : no api");
//				if (tag != null && tag.startsWith("CAT")) {
//					tagId = Integer.valueOf(tag.substring("CAT:".length()));
//				}
				break;
			case ReaderExtension.ACTION_SUBSCRIPTION_REMOVE_LABEL:
				// no api
				Log.w(TAG, "editSubscription: ACTION_REMOVE_LABEL : no api");
				break;
			case ReaderExtension.ACTION_SUBSCRIPTION_SUBCRIBE:
				return subscribeFeed(url, 0);
			case ReaderExtension.ACTION_SUBSCRIPTION_UNSUBCRIBE:
				if (uid.startsWith("FEED")) {
					int feedId = Integer.valueOf(uid.substring("FEED:".length()));
					return unsubscribeFeed(feedId);
				}
				break;
		}

		return false;
	}

	public boolean subscribeFeed(String url, int categoryId) throws IOException, ReaderException {
		try {
			JSONObject jo = new JSONObject();
			jo.put("sid", getSessionId());
			jo.put("op", "subscribeToFeed");

			jo.put("feed_url", url);
			if (categoryId > -1) jo.put("category_id", categoryId);

			return doRequest(jo);

		} catch (ReaderLoginException e) {
			if (hasReAuthenthicated) throw e;
			try {
				login();
				subscribeFeed(url, categoryId);
			} catch (Exception ex) {
				ex.printStackTrace();
				throw new ReaderLoginException(e.getLocalizedMessage());
			}
		} catch (JSONException e) {
			throw new ReaderException("connection error", e);
		}

		return false;
	}

	public boolean unsubscribeFeed(int feedId) throws IOException, ReaderException {
		try {
			JSONObject jo = new JSONObject();
			jo.put("sid", getSessionId());
			jo.put("op", "unsubscribeFeed");
			jo.put("feed_id", feedId);
			
			return doRequest(jo);

		} catch (ReaderLoginException e) {
			if (hasReAuthenthicated) throw e;
			try {
				login();
				unsubscribeFeed(feedId);
			} catch (Exception ex) {
				ex.printStackTrace();
				throw new ReaderLoginException(e.getLocalizedMessage());
			}
		} catch (JSONException e) {
			throw new ReaderException("connection error", e);
		}

		return false;
	}

	@Override
	public boolean renameTag(String tagUid, String oldLabel, String newLabel) throws IOException, ReaderException {
		Log.w(TAG, "renameTag: not implemented");
		return false;
	}

	@Override
	public boolean disableTag(String tagUid, String label) throws IOException, ReaderException {
		Log.w(TAG, "disableTag: not implemented");
		return false;
	}

}
