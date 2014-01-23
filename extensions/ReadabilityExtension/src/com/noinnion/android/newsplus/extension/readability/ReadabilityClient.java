package com.noinnion.android.newsplus.extension.readability;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.security.KeyStore;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

import oauth.signpost.OAuthConsumer;
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;

import com.noinnion.android.newsplus.extension.readability.util.AndroidUtils;
import com.noinnion.android.newsplus.extension.readability.util.HttpUtils;
import com.noinnion.android.newsplus.extension.readability.util.MySSLSocketFactory;
import com.noinnion.android.newsplus.extension.readability.util.Utils;
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

public class ReadabilityClient extends ReaderExtension {
	private String user, password;
	public Context mContext;
	private List<String> lastItemIDList;
	
	public final int LOGIN_OK=200;
	private static final String starredTag="Tag/starred";
	
	protected DefaultHttpClient client;

	public static final int SOCKET_OPERATION_TIMEOUT = 20000; // 20s

	public ReadabilityClient() 
	{
	}
	/**
	 * do you use this Constructer and not default
	 * 
	 * @param c
	 */
	public ReadabilityClient(Context c) 
	{
		mContext = c;
		user=Prefs.getUser(c);
		password=Prefs.getPassword(c);
	}

	@Override
	public boolean disableTag(String arg0, String arg1) throws IOException,ReaderException 
	{
		return false;
	}

	@Override
	public boolean editItemTag(String[] itemUids, String[] subUids, String[] tags, int action) throws IOException, ReaderException 
	{
		mContext=getApplicationContext();
		if(action==ReaderExtension.ACTION_ITEM_TAG_ADD_LABEL)
		{
			for(String tag:tags)
			{
				if(tag.equals(starredTag))
				{
					for(String itemBookmarkId:itemUids)
					{
						try {
							doPostInputStream("https://www.readability.com/api/rest/v1/bookmarks/"+itemBookmarkId+"?favorite=1", new ArrayList<NameValuePair>());
						} catch (Exception e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}
			}
		}
		if(action==ReaderExtension.ACTION_ITEM_TAG_REMOVE_LABEL)
		{
			for(String tag:tags)
			{
				if(tag.equals(starredTag))
				{
					for(String itemBookmarkId:itemUids)
					{
						try {
							doPostInputStream("https://www.readability.com/api/rest/v1/bookmarks/"+itemBookmarkId+"?favorite=0", new ArrayList<NameValuePair>());
						} catch (Exception e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						
					}
				}
			}
		}
		return false;
	}

	@Override
	public boolean editSubscription(String uid, String title, String url, String[] tags, int action) throws IOException, ReaderException 
	{
//		Log.e("EditSubscription",title);
		loadUser();
		
        switch (action) {
        case ReaderExtension.ACTION_SUBSCRIPTION_EDIT:
                // no api
                Log.w("Test", "editSubscription: ACTION_EDIT : no api");
                break;
        case ReaderExtension.ACTION_SUBSCRIPTION_NEW_LABEL:
                break;
        case ReaderExtension.ACTION_SUBSCRIPTION_ADD_LABEL:
                // no api
                Log.w("Test", "editSubscription: ACTION_ADD_LABEL : no api");

                break;
        case ReaderExtension.ACTION_SUBSCRIPTION_REMOVE_LABEL:
                // no api
                Log.w("Test", "editSubscription: ACTION_REMOVE_LABEL : no api");
                break;
        case ReaderExtension.ACTION_SUBSCRIPTION_SUBCRIBE:
                break;
        case ReaderExtension.ACTION_SUBSCRIPTION_UNSUBCRIBE:
 
                break;
        }

        return false;
	}

	
	@Override
	public void handleItemIdList(IItemIdListHandler itemHandler, long arg1)throws IOException, ReaderException
	{
		loadUser();
		List<String>idList=new ArrayList<String>();
		
		for(String id:lastItemIDList)
		{
			idList.add(id+"");
		}
		try {
			itemHandler.items(idList);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		
	}
	
	@Override
	public void handleItemList(IItemListHandler itemHandler, long arg1)throws IOException, ReaderException 
	{
		mContext=getApplicationContext();
		try 
		{
			if(itemHandler.stream().equals(STATE_READING_LIST))
			{		
				List<Integer>articleIds=Prefs.getAllItemIDs(mContext);
				if(itemHandler.startTime()==0)
				{
					Prefs.removeALLItemIDs(mContext);
				}
				lastItemIDList=new ArrayList<String>();
				HttpResponse response=doGetInputStream("https://www.readability.com/api/rest/v1/bookmarks");
				String content=getContent(getInputStreamFromResponse(response));
				JSONObject obj=new JSONObject(content);
				JSONArray array=obj.getJSONArray("bookmarks");
				ArrayList<IItem>itemlist=new ArrayList<IItem>();
				int entryLength=0;
				for(int i=0;i<array.length();i++)
				{
					JSONObject bookmark=array.getJSONObject(i);//bookmark
					String articleHref=bookmark.getString("article_href");
					JSONArray tags=bookmark.getJSONArray("tags");
					
					if(articleIds.indexOf(new Integer(bookmark.getString("id")))>-1)
					{
						lastItemIDList.add(bookmark.getString("id"));
						continue;
					}
					Prefs.addItemID(mContext, bookmark.getInt("id"));
					response=doGetInputStream("https://www.readability.com"+articleHref);
					content=getContent(getInputStreamFromResponse(response));
					JSONObject article=null;
					try{article=new JSONObject(content);}catch(Exception e){e.printStackTrace();continue;}
					IItem item=new IItem();
					if(!article.isNull("author"))
						item.author=article.getString("author");
					if(!article.isNull("content"))
						item.content=article.getString("content");
					else
						item.content="";
					if(!article.isNull("url"))
						item.link=article.getString("url");
					if(!article.isNull("lead_image_url"))
						item.image=article.getString("lead_image_url");
					if(!article.isNull("title"))
						item.title=article.getString("title");
					if(!article.isNull("id"))
						item.uid=bookmark.getString("id");
					if(tags.length()>0)
					{
						JSONObject t=tags.getJSONObject(0);
						item.subUid=t.getString("id");
					}
					SimpleDateFormat format=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
					if(!article.isNull("date_published"))
					{
						item.publishedTime=format.parse(article.getString("date_published")).getTime();
					}
					if(!bookmark.isNull("date_added"))
						item.updatedTime=format.parse(bookmark.getString("date_added")).getTime()/1000;
					else
						item.updatedTime=item.publishedTime/1000;
					if((entryLength+item.getLength())>MAX_TRANSACTION_LENGTH)
					{
						try{itemHandler.items(itemlist, item.getLength());}catch(Exception e)
						{
							Log.e("Readability.handleItem",e.toString()+" :  TransactionLength: "+entryLength);
						}
						itemlist.clear();
						entryLength=0;
					}
					entryLength=entryLength+item.getLength();
					itemlist.add(item);
					lastItemIDList.add(item.uid);
				}
				itemHandler.items(itemlist, entryLength);
			} 
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	@Override
	public void handleReaderList(ITagListHandler tagHandler,ISubscriptionListHandler subscriptionHandler, long arg2) throws IOException,ReaderException 
	{
		mContext=getApplicationContext();
		//tags
		try {
			ArrayList<ITag>tagList=new ArrayList<ITag>();
			ITag tag=new ITag();
			tag.label="Favorites";
			tag.uid=this.starredTag;
			tag.type=ITag.TYPE_TAG_STARRED;
			tagList.add(tag);
			tagHandler.tags(tagList);
			
			HttpResponse response=doGetInputStream("https://www.readability.com/api/rest/v1/tags");
			String content=getContent(getInputStreamFromResponse(response));
			JSONObject obj=new JSONObject(content);
			
			JSONArray array=(JSONArray)obj.get("tags");
			ArrayList<ISubscription>sublist=new ArrayList<ISubscription>();
			for(int i=0;i<array.length();i++)
			{
//				{"id": 44, "text": "new yorker", "applied_count": 3, "bookmark_ids": [1, 4, 539]}
				obj=array.getJSONObject(i);
				ISubscription sub=new ISubscription();
				sub.title=obj.getString("text");
				sub.uid=obj.getString("id");
				sublist.add(sub);
//				tag=new ITag();
//				tag.label=obj.getString("text");
//				tag.uid=obj.getString("id");
//				tag.type=ITag.TYPE_TAG_LABEL;
////				Log.e("Tag","id: "+tag.uid);
//				tagList.add(tag);
			}
			subscriptionHandler.subscriptions(sublist);
				
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public boolean markAllAsRead(String stream, String title, String[] excludedStreams,long syncTime) throws IOException, ReaderException
	{
		//keine implementierung in Readability vorgesehen
		return true;
	}

	@Override
	public boolean markAsRead(String[] arg0, String[] arg1) throws IOException,ReaderException 
	{
		//keine implementierung in Readability vorgesehen
		return false;
	}

	@Override
	public boolean markAsUnread(String[] itemUids, String[] subUids, boolean keepUnread)throws IOException, ReaderException 
	{
		//keine implementierung in Readability vorgesehen
	    return false;
	}

	@Override
	public boolean renameTag(String arg0, String arg1, String arg2)throws IOException, ReaderException 
	{
		//nicht implementiert in FeedBin
		return false;
	}
	
    private void handleError(String error) throws ReaderException {
        if (error != null && error.equals("NOT_LOGGED_IN")) {
                throw new ReaderLoginException("NOT_LOGGED_IN");
        } else {
                throw new ReaderException(error);
        }
	}
    
	private Context getContext() {
		return mContext == null ? getApplicationContext() : mContext;
	}
	
	public void loadUser()
	{
		if(mContext!=null&&user!=null&&password!=null)
			return;
		mContext=getApplicationContext();
		user=Prefs.getUser(mContext);
		password=Prefs.getPassword(mContext);
	}

	

	public DefaultHttpClient getClient() {
		if (client == null)
			client = HttpUtils.createHttpClient();
		return client;
	}

	public static DefaultHttpClient createHttpClient() {
		try {
			HttpParams params = new BasicHttpParams();

			// Turn off stale checking. Our connections break all the time
			// anyway,
			// and it's not worth it to pay the penalty of checking every time.
			HttpConnectionParams.setStaleCheckingEnabled(params, false);

			// Set the timeout in milliseconds until a connection is
			// established. The default value is zero, that means the timeout is
			// not used.
			HttpConnectionParams.setConnectionTimeout(params,
					SOCKET_OPERATION_TIMEOUT);
			// Set the default socket timeout (SO_TIMEOUT) in milliseconds which
			// is the timeout for waiting for data.
			HttpConnectionParams.setSoTimeout(params, SOCKET_OPERATION_TIMEOUT);

			HttpConnectionParams.setSocketBufferSize(params, 8192);

			// Don't handle redirects -- return them to the caller. Our code
			// often wants to re-POST after a redirect, which we must do
			// ourselves.
			HttpClientParams.setRedirecting(params, false);

			// HttpProtocolParams.setUserAgent(params, userAgent);
			HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
			HttpProtocolParams.setContentCharset(params, HTTP.UTF_8);
			HttpProtocolParams.setUseExpectContinue(params, true);

			// ssl
			KeyStore trustStore = KeyStore.getInstance(KeyStore
					.getDefaultType());
			trustStore.load(null, null);

			SSLSocketFactory sf = new MySSLSocketFactory(trustStore);
			sf.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);

			SchemeRegistry registry = new SchemeRegistry();
			registry.register(new Scheme("http", PlainSocketFactory
					.getSocketFactory(), 80));
			registry.register(new Scheme("https", sf, 443));

			ClientConnectionManager ccm = new ThreadSafeClientConnManager(
					params, registry);

			return new DefaultHttpClient(ccm, params);
		} catch (Exception e) {
			return HttpUtils.createHttpClient();
		}
	}
    
    public HttpResponse doGetInputStream(String url) throws ClientProtocolException, IOException, ReaderException, OAuthMessageSignerException, OAuthExpectationFailedException, OAuthCommunicationException
    {
		HttpGet post = new HttpGet(url);
		post.addHeader("Content-Type", "application/x-www-form-urlencoded");
		
		
		String tokenSecret=Prefs.getOAuthTokenSecret(mContext);
		String token=Prefs.getOAuthToken(mContext);
		OAuthConsumer mConsumer = new CommonsHttpOAuthConsumer(Prefs.KEY, Prefs.SECRET);
		mConsumer.setTokenWithSecret(token, tokenSecret);
		mConsumer.sign(post);
		
		HttpResponse response = getClient().execute(post);
		int responseCode = response.getStatusLine().getStatusCode();

		if (responseCode == 401) {
			AndroidUtils.showToast(mContext, "Authorization Required: Authentication failed or was not provided.");
		} else if (responseCode == 404) AndroidUtils.showToast(mContext, "Not Found: The resource that you requested does not exist.");
		else if (responseCode == 500) AndroidUtils.showToast(mContext, "Internal Server Error: An unknown error has occurred.");
		else if (responseCode == 400) AndroidUtils.showToast(mContext, "Bad Request: The server could not understand your request.");
		else if (responseCode == 409) AndroidUtils.showToast(mContext, "Conflict: The resource that you are trying to create already exists.");
		else if (responseCode == 403) AndroidUtils.showToast(mContext, "Forbidden: You are not allowed to perform the requested action.");
		else return response;
		return null;
    }
	
	public HttpResponse doPostInputStream(String url,List<NameValuePair>params) throws IOException, ReaderException, OAuthMessageSignerException, OAuthExpectationFailedException, OAuthCommunicationException 
	{
		HttpClient client = new DefaultHttpClient();

		HttpPost post = new HttpPost(url);
		post.addHeader("Content-Type", "application/x-www-form-urlencoded");
		post.setEntity(new UrlEncodedFormEntity(params, HTTP.UTF_8));
		
		
		String tokenSecret=Prefs.getOAuthTokenSecret(mContext);
		String token=Prefs.getOAuthToken(mContext);
		OAuthConsumer mConsumer = new CommonsHttpOAuthConsumer(Prefs.KEY, Prefs.SECRET);
		mConsumer.setTokenWithSecret(token, tokenSecret);
		mConsumer.sign(post);
		
		HttpResponse response = getClient().execute(post);
		int responseCode = response.getStatusLine().getStatusCode();

		if (responseCode == 401) {
			AndroidUtils.showToast(mContext, "Authorization Required: Authentication failed or was not provided.");
		} else if (responseCode == 404) AndroidUtils.showToast(mContext, "Not Found: The resource that you requested does not exist.");
		else if (responseCode == 500) AndroidUtils.showToast(mContext, "Internal Server Error: An unknown error has occurred.");
		else if (responseCode == 400) AndroidUtils.showToast(mContext, "Bad Request: The server could not understand your request.");
		else if (responseCode == 409) AndroidUtils.showToast(mContext, "Conflict: The resource that you are trying to create already exists.");
		else if (responseCode == 403) AndroidUtils.showToast(mContext, "Forbidden: You are not allowed to perform the requested action.");
		else return response;
		return null;
	}
	
	public InputStream getInputStreamFromResponse(HttpResponse response) throws ReaderException, IllegalStateException, IOException
	{
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
			is = new InflaterInputStream(entity.getContent(),
					new Inflater(true));
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

	public boolean login(String user, String password) throws IOException,ReaderException, JSONException {
		
		this.user = user;
		this.password = password;		

		HttpClient client = new DefaultHttpClient();
		HttpPost request = new HttpPost(Prefs.AUTHORIZE_URL);
		CommonsHttpOAuthConsumer consumer = new CommonsHttpOAuthConsumer(Prefs.KEY, Prefs.SECRET);
		List<BasicNameValuePair> params = Arrays.asList(new BasicNameValuePair("x_auth_username", user), new BasicNameValuePair("x_auth_password", password), new BasicNameValuePair("x_auth_mode", "client_auth"));
		UrlEncodedFormEntity entity = null;
		try {
			entity = new UrlEncodedFormEntity(params, HTTP.UTF_8);
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException("wtf");
		}
		request.setEntity(entity);
		try {
			consumer.sign(request);
		} catch (OAuthMessageSignerException e) {
			return false;
		} catch (OAuthExpectationFailedException e) {
			return false;
		} catch (OAuthCommunicationException e) {
			return false;
		}
		HttpResponse response;
		InputStream data = null;
		try {
			response = client.execute(request);
			data = response.getEntity().getContent();
		} catch (ClientProtocolException e) {
			return false;
		} catch (IOException e) {
			return false;
		}

		String responseString = null;
		try {
			final char[] buffer = new char[0x10000];
			StringBuilder out = new StringBuilder();
			Reader in = new InputStreamReader(data, HTTP.UTF_8);
			int read;
			do {
				read = in.read(buffer, 0, buffer.length);
				if (read > 0) {
					out.append(buffer, 0, read);
				}
			} while (read >= 0);
			in.close();
			responseString = out.toString();
		} catch (IOException ioe) {
			// throw new IllegalStateException("Error while reading response body", ioe);
			return false;
		}
		String[]rPart=responseString.split("&");
		String tokenSecret=rPart[0].substring(rPart[0].indexOf("=")+1);
		String token=rPart[1].substring(rPart[1].indexOf("=")+1);
		Prefs.setOAuth(mContext, tokenSecret, token);
		return true;
	}

	
	
	public String getContent(InputStream in) throws IOException
	{
		return getContent(in,"UTF-8");
	}
	public String getContent(InputStream in,String encoding) throws IOException
	{
		StringBuffer sb=new StringBuffer();
		int len=10000;
		byte[]b=new byte[len];
		while(len!=1)
		{
			len=in.read(b);
			if(len==-1)
				break;
			sb.append(new String(b,0,len,encoding));
		}
		in.close();
		return sb.toString();
	}
}
