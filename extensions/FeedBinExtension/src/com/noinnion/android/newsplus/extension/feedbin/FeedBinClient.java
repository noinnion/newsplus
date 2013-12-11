package com.noinnion.android.newsplus.extension.feedbin;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.auth.AuthenticationException;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpDeleteEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
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
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;

import com.noinnion.android.newsplus.extension.feedbin.util.AndroidUtils;
import com.noinnion.android.newsplus.extension.feedbin.util.HttpUtils;
import com.noinnion.android.newsplus.extension.feedbin.util.MySSLSocketFactory;
import com.noinnion.android.newsplus.extension.feedbin.util.Utils;
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

public class FeedBinClient extends ReaderExtension {
	
	
	public static final String TAG = "FeedBinClient"; // 20s
	
	private String user, password;
	public Context mContext;
	private static final String starredTag="Tag/starred";
	
	public final int LOGIN_OK=400;
	
	protected DefaultHttpClient client;

	public static final int SOCKET_OPERATION_TIMEOUT = 20000; // 20s

	public FeedBinClient() 
	{
	}
	/**
	 * do you use this Constructer and not default
	 * 
	 * @param c
	 */
	public FeedBinClient(Context c) 
	{
		mContext = c;
		user=Prefs.getUser(c);
		password=Prefs.getPassword(c);
	}

	@Override
	public boolean disableTag(String uid, String title) throws IOException,ReaderException 
	{
		try{
			doDeleteInputStream(Prefs.TAG_DELETE_URL+uid+".json",null);
			return true;
		}catch(Exception e){}
		return false;
	}

	@Override
	public boolean editItemTag(String[] itemUids, String[] subUids, String[] tags, int action) throws IOException, ReaderException 
	{
		loadUser();
		if(action==ReaderExtension.ACTION_ITEM_TAG_ADD_LABEL)
		{
			for(String tag:tags)
			{
				if(tag.equals(starredTag))
				{
					JSONObject obj=new JSONObject();
					JSONArray array=new JSONArray();
					for(String id:itemUids)
					{
						array.put(id);
					}
					try {
						obj.put("starred_entries",array);
					} catch (JSONException e) {
						e.printStackTrace();
					}
					doPostInputStream(Prefs.STARRED_URL, obj);
				}
			}
		}
		if(action==ReaderExtension.ACTION_ITEM_TAG_REMOVE_LABEL)
		{
			for(String tag:tags)
			{
				if(tag.equals(starredTag))
				{
					JSONObject obj=new JSONObject();
					JSONArray array=new JSONArray();
					for(String id:itemUids)
					{
						array.put(id);
					}
					try {
						obj.put("starred_entries",array);
					} catch (JSONException e) {
						e.printStackTrace();
					}
					doPostInputStream(Prefs.STARRED_DELETE_URL, obj);
				}
			}
		}
		return false;
	}

	@Override
	public boolean editSubscription(String uid, String title, String url, String[] tags, int action) throws IOException, ReaderException 
	{
		loadUser();
		
        switch (action) {
        case ReaderExtension.ACTION_SUBSCRIPTION_EDIT:
                try
                {
	                String id=uid.split(",")[1]; 
	                JSONObject obj=new JSONObject();
	                obj.put("title", title);
	                doPatchInputStream(Prefs.SUBSCRIPTION_UPDATE+id, obj);
	            }
                catch(Exception e){}
                break;
        case ReaderExtension.ACTION_SUBSCRIPTION_NEW_LABEL:
        case ReaderExtension.ACTION_SUBSCRIPTION_ADD_LABEL:
        	try
        	{
        		JSONObject obj=new JSONObject();
        		obj.put("feed_id", uid.split(",")[0]);
        		obj.put("name", title);
        		doPostInputStream(Prefs.TAG_CREATE_URL, obj);
        	}
        	catch(Exception e){}
            break;
        case ReaderExtension.ACTION_SUBSCRIPTION_REMOVE_LABEL:
        	Log.w(TAG,"editSubscription.ACTION_SUBSCRIPTION_REMOVE_LABEL not supported");
            break;
        case ReaderExtension.ACTION_SUBSCRIPTION_SUBCRIBE:
        	Log.w(TAG,"editSubscription.ACTION_SUBSCRIPTION_SUBCRIBE not supported");
                break;
        case ReaderExtension.ACTION_SUBSCRIPTION_UNSUBCRIBE:
        	Log.w(TAG,"editSubscription.ACTION_SUBSCRIPTION_UNSUBCRIBE not supported");
                break;
        }
        return false;
	}

	
	@Override
	public void handleItemIdList(IItemIdListHandler itemHandler, long sync)throws IOException, ReaderException
	{
		loadUser();
		try {
			HttpResponse response=doGetInputStream(Prefs.UNREAD_ENTRIES_URL);
			InputStream in=getInputStreamFromResponse(response);
			String json=getContent(in);
			JSONArray array=new JSONArray(json);
			ArrayList<String>list=new ArrayList<String>();
			for(int i=0;i<array.length();i++)
				list.add(array.getString(i));
			
			itemHandler.items(list);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private List<ISubscription>lastSubscriptionList;
	
	@Override
	public void handleItemList(IItemListHandler itemHandler, long arg1)throws IOException, ReaderException 
	{
		loadUser();
		try
		{
			Log.w("Stream",itemHandler.stream());
			if(itemHandler.stream().equals(STATE_READING_LIST))
			{
				if(itemHandler.startTime()==0)
				{
					Prefs.removeLastUnreadId(mContext);
				}
				String date=Prefs.getLastUpdate(mContext);
				date=date==null?"":date;
				
				HttpResponse response=doGetInputStream(Prefs.UNREAD_ENTRIES_URL);//+"?since="+date);//date bringt nichts
				InputStream in=getInputStreamFromResponse(response);
				String json=getContent(in);
				
				JSONArray array=new JSONArray(json);
				int lastUnreadId=Prefs.getLastUnreadId(mContext);//speichert die maximale id für das id testen
				ArrayList<Integer>ids=new ArrayList<Integer>();
				for(int i=0;i<array.length();i++)
				{
					int val=array.getInt(i);
					if(lastUnreadId>=val)
						continue;
					lastUnreadId=val;
					ids.add(val);
				}
				Prefs.setLastUnreadId(mContext, lastUnreadId);
				loadIntoItemHandler(itemHandler,ids);
			}

		}
		catch(Exception e){e.printStackTrace();}
		Prefs.setLastUpdate(mContext, System.currentTimeMillis());
	}
	private void loadIntoItemHandler(IItemListHandler handler,ArrayList<Integer>ids)throws Exception
	{
		int counter=0;
		StringBuffer sb=new StringBuffer();
		ArrayList<IItem>itemlist=new ArrayList<IItem>();
		int entryLength=0;
		for(int i=ids.size()-1;i>=0;i--)
		{
			if(counter==0)
				sb.append(ids.get(i));
			else
				sb.append(","+ids.get(i));
			counter++;
			if(counter==100)
			{
				counter=0;
				HttpResponse response = doGetInputStream(Prefs.ENTRIES_URL+sb.toString());
				InputStream in=getInputStreamFromResponse(response);
				JSONArray itemArray=new JSONArray(getContent(in));
				entryLength=addItemToItemHandlerList(itemArray, itemlist,handler,entryLength);
				sb=new StringBuffer();
			}
		}
		//letzten einträge, die kleiner als MAX_TRANSACTION_LENGTH sind
		handler.items(itemlist, entryLength);
	}
	/**
	 * fügt die Items des Array zu der ItemHandler list hinzu
	 * 
	 * @param array
	 * @param handler
	 * @throws Exception
	 */
	private int addItemToItemHandlerList(JSONArray array,ArrayList<IItem>tempsavelist,IItemListHandler handler,int entryLength)throws Exception
	{
		SimpleDateFormat format=new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
		for(int i=0;i<array.length();i++)
		{
			JSONObject obj=array.getJSONObject(i);
			
			IItem item=new IItem();
			
			setItemSubId(item, obj.getString("feed_id"));
			item.id=obj.getLong("id");
			if(!obj.isNull("author"))
				item.author=obj.getString("author");
			if(!obj.isNull("url"))
				item.link=obj.getString("url");
			if(!obj.isNull("content"))
				item.content=obj.getString("content");
			if(!obj.isNull("title"))
				item.title=obj.getString("title");
			if(!obj.isNull("published"))
			{
				String date=obj.getString("published");
				item.publishedTime=format.parse(date.substring(0,date.lastIndexOf('.'))).getTime();
			}
			if(!obj.isNull("id"))
				item.uid=obj.getString("id");
			item.updatedTime=item.publishedTime/1000;
			if(entryLength+item.getLength()>MAX_TRANSACTION_LENGTH)
			{
				handler.items(tempsavelist, entryLength);
				tempsavelist.clear();
				entryLength=0;
			}
			entryLength=entryLength+item.getLength();
			tempsavelist.add(item);
		}
		return entryLength;
		
	}
	/**weisst dem item die subUid zu, indem die SubscriptionList mit der feedid überprüft wird*/
	private void setItemSubId(IItem item,String feedid)
	{
		for(ISubscription sub:lastSubscriptionList)
		{
			if(sub.uid.startsWith(feedid+","))
			{
				item.subUid=sub.uid;
				return;
			}
		}
	}
	/**
	 * liefert den Tag zurück wenn schon vorhanden oder null wenn nicht
	 * 
	 * @param list
	 * @param tag
	 * @return
	 */
	private ITag isDoubleITag(List<ITag>list,ITag tag)
	{
		for(ITag t:list)
		{
			if(t.label.equals(tag.label)&&t.type==tag.type)
				return t;
		}
		return null;
	}
	@Override
	public void handleReaderList(ITagListHandler tagHandler,ISubscriptionListHandler subscriptionHandler, long sync) throws IOException,ReaderException 
	{
		loadUser();
		Map<String, String> map = new HashMap<String, String>();
		
		//Alle Tags hinzufügen
		List<ITag> tagList=new ArrayList<ITag>();
		try {
			JSONArray tags=getAllTags();
			for(int i=0;i<tags.length();i++)
			{
				JSONObject obj=(JSONObject) tags.get(i);
				ITag t=new ITag();
				t.type=ITag.TYPE_FOLDER;
				t.uid=obj.getString("id");
				t.label=obj.getString("name");
				String feedId = obj.getString("feed_id");
				ITag oldTag=isDoubleITag(tagList, t);
				if(oldTag!=null)//verhindert doppelte Tags
				{
					if (feedId != null) map.put(feedId, oldTag.uid);
					continue;
				}
				if (feedId != null) map.put(feedId, t.uid);
				tagList.add(t);
			}
			
		} catch (JSONException e1) {
			e1.printStackTrace();
		} 
		//Fake Starred Tag
		ITag starredTag=new ITag();
		starredTag.type=ITag.TYPE_TAG_STARRED;
		starredTag.uid=FeedBinClient.starredTag;
		starredTag.label="Starred";
		tagList.add(starredTag);
		try {
			tagHandler.tags(tagList);
		} catch (RemoteException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		//Alle Subscritpions hinzufügen
		try {
			JSONArray subs=getAllSubscriptions();
			lastSubscriptionList=new ArrayList<ISubscription>();
			for(int i=0;i<subs.length();i++)
			{
				JSONObject obj=(JSONObject) subs.get(i);
				ISubscription sub=new ISubscription();
				sub.htmlUrl=obj.getString("feed_url");
				sub.title=obj.getString("title");
				sub.uid=obj.getString("feed_id")+","+obj.getString("id");
				sub.newestItemTime=sync;
				if (map.containsKey(obj.getString("feed_id"))) sub.addTag(map.get(obj.getString("feed_id")));
				lastSubscriptionList.add(sub);
			}
			subscriptionHandler.subscriptions(lastSubscriptionList);
		} catch (JSONException e) {
			e.printStackTrace();
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	@Override
	public boolean markAllAsRead(String stream, String title, String[] excludedStreams,long syncTime) throws IOException, ReaderException
	{
		Log.w(TAG,"markAllAsRead not supported");
		loadUser();
		return false;
	}

	@Override
	public boolean markAsRead(String[] uids, String[] arg1) throws IOException,ReaderException 
	{
//		loadUser();
//		
//		JSONArray array=new JSONArray();
//		//maximal 1000 mit einen request
//		int counter=0;
//		for(String s:uids)
//		{
//			counter++;
//			array.put(s);
//			if(counter<1000)
//			{
//				try {
//					JSONObject obj=new JSONObject();
//					obj.put("unread_entries", array);
//					doDeleteInputStream(Prefs.UNREAD_ENTRIES_URL, obj);
//				} catch (Exception e) {
//					e.printStackTrace();
//				}
//				counter=0;
//				array=new JSONArray();
//			}
//		}
//		
//		try {
//			JSONObject obj=new JSONObject();
//			obj.put("unread_entries", array);
//			HttpResponse response=doDeleteInputStream(Prefs.UNREAD_ENTRIES_URL, obj);
//			if(response.getStatusLine().getStatusCode()==200)
//				return true;
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
		return false;
	}

	@Override
	public boolean markAsUnread(String[] itemUids, String[] subUids, boolean keepUnread)throws IOException, ReaderException 
	{
		Log.w("mark","as unread");
		String url=Prefs.UNREAD_ENTRIES_URL;
		loadUser();
		
	        try {
	            
	    		JSONArray array=new JSONArray();
	    		int counter=0;//maximal 1000 pro request
	    		for(String s:itemUids)
	    		{
	    			counter++;
	    			array.put(s);
	    			if(counter<1000)
	    			{
	    				JSONObject jo = new JSONObject();
	    				jo.put("unread_entries", array);
	    				doRequest(url,jo);
	    				counter=0;
	    				array=new JSONArray();
	    			}
	    		}
	    		
	    		JSONObject jo = new JSONObject();
	            jo.put("unread_entries", array);
	
	            return doRequest(url,jo);
	
	    } catch (ReaderLoginException e) {

	    } catch (JSONException e) {
	            e.printStackTrace();
	    }
	
	    return false;
	}

	@Override
	public boolean renameTag(String arg0, String arg1, String arg2)throws IOException, ReaderException 
	{
		Log.w(TAG,"renameTag not supported");
		return false;
	}
	
    private void handleError(String error) throws ReaderException {
        if (error != null && error.equals("NOT_LOGGED_IN")) {
                throw new ReaderLoginException("NOT_LOGGED_IN");
        } else {
                throw new ReaderException(error);
        }
}
	
    public boolean doRequest(String url,JSONObject jo) throws IOException, JSONException, ReaderException
    {
        String json = Utils.convertStreamToString(getInputStreamFromResponse(doPostInputStream(url,jo)));

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


    public void httpAuthentication(HttpUriRequest post) throws ReaderException {
            String httpUsername = this.user;
            String httpPassword = this.password;
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
    public HttpResponse doDeleteInputStream(String url,JSONObject jo)throws Exception
    {
    	HttpDeleteEntity delete=new HttpDeleteEntity(url);
    	httpAuthentication(delete);
    	delete.setHeader("Content-Type","application/json; charset=utf-8");
		if(jo!=null)
		{
			StringEntity postEntity = new StringEntity(jo.toString());
			postEntity.setContentType("application/json; charset=utf-8");
			delete.setEntity(postEntity);
		}
		return getClient().execute(delete);
    }
    public HttpResponse doPatchInputStream(String url,JSONObject jo) throws Exception
    {
    	HttpPatch patch=new HttpPatch(url);
    	httpAuthentication(patch);
    	patch.setHeader("Content-Type","application/json; charset=utf-8");
		if(jo!=null)
		{
			StringEntity postEntity = new StringEntity(jo.toString());
			postEntity.setContentType("application/json; charset=utf-8");
			patch.setEntity(postEntity);
		}
		return getClient().execute(patch);
    }
    public HttpResponse doGetInputStream(String url) throws ClientProtocolException, IOException, ReaderException
    {
    	HttpGet get=new HttpGet(url);
    	get.setHeader("Content-Type","application/json; charset=utf-8");
    	httpAuthentication(get);
		HttpResponse response = getClient().execute(get);
		return response;
    }
	
	public HttpResponse doPostInputStream(String url, JSONObject jo) throws IOException, ReaderException 
	{
		HttpPost post = new HttpPost(url);
		httpAuthentication(post);
		post.setHeader("Content-Type","application/json; charset=utf-8");
		if(jo!=null)
		{
			StringEntity postEntity = new StringEntity(jo.toString());
			postEntity.setContentType("application/json; charset=utf-8");
			post.setEntity(postEntity);
		}
		HttpResponse response = getClient().execute(post);
		return response;
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

	public boolean login(String user, String password) throws IOException,
			ReaderException, JSONException {
		
		this.user = user;
		this.password = password;
		
		HttpResponse response = doPostInputStream(Prefs.getAuthURL(), null);
		if(response.getStatusLine().getStatusCode()==LOGIN_OK)
		{
			AndroidUtils.showToast(getContext(), "Login OK");
			return true;
		}
		AndroidUtils.showToast(getContext(), "Login fail");
		return false;
	}
	
	public JSONArray getAllSubscriptions() throws ClientProtocolException, IOException, ReaderException, JSONException
	{
		String url=Prefs.SUBSCRIPTION_URL;
		HttpResponse res=doGetInputStream(url);
		InputStream in=getInputStreamFromResponse(res);
		String json=getContent(in);
		JSONArray ar=new JSONArray(json);
		return ar;
	}
	public JSONObject getFeed(int id) throws ClientProtocolException, IOException, ReaderException, JSONException
	{
		String url=Prefs.FEED_URL+id+".json";
		HttpResponse res=doGetInputStream(url);
		InputStream in=getInputStreamFromResponse(res);
		String json=getContent(in);
		JSONObject obj=new JSONObject(json);
		return obj;
	}
	public JSONArray getAllTags() throws ClientProtocolException, IOException, ReaderException, JSONException
	{
		String url=Prefs.TAG_URL;
		HttpResponse res=doGetInputStream(url);
		InputStream in=getInputStreamFromResponse(res);
		String json=getContent(in);
		JSONArray ar=new JSONArray(json);
		return ar;
	}
	public JSONArray getAllFeedEntries(int feedid,String lastdate) throws ClientProtocolException, IOException, ReaderException, JSONException
	{
		String addurl=lastdate!=null?"?since="+lastdate:"";
		String url=Prefs.FEED_URL+feedid+"/entries.json"+addurl;
		HttpResponse res=doGetInputStream(url);
		InputStream in=getInputStreamFromResponse(res);
		String json=getContent(in);
		JSONArray ar=new JSONArray(json);
		return ar;
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
