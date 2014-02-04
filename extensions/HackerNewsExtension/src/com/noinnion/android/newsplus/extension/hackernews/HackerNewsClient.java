package com.noinnion.android.newsplus.extension.hackernews;

import java.io.BufferedReader;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicTokenIterator;
import org.apache.http.protocol.HTTP;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import com.noinnion.android.newsplus.extension.hackernews.util.AndroidUtils;
import com.noinnion.android.newsplus.extension.hackernews.util.Utils;
import com.noinnion.android.reader.api.ReaderException;
import com.noinnion.android.reader.api.ReaderExtension;
import com.noinnion.android.reader.api.ReaderException.ReaderLoginException;
import com.noinnion.android.reader.api.internal.IItemIdListHandler;
import com.noinnion.android.reader.api.internal.IItemListHandler;
import com.noinnion.android.reader.api.internal.ISubscriptionListHandler;
import com.noinnion.android.reader.api.internal.ITagListHandler;
import com.noinnion.android.reader.api.provider.IItem;
import com.noinnion.android.reader.api.provider.ISubscription;
import com.noinnion.android.reader.api.provider.ITag;

public class HackerNewsClient extends ReaderExtension
{
	private String user,password,auth;
	public Context mContext;
	protected DefaultHttpClient	client;
	public static final String TAG_STARRED="Tag/Strarred";
	public static final String SUB_ITEMS="Feed/Items";
	
	public HackerNewsClient(){}
	
	public HackerNewsClient(Context c)
	{
		mContext = c;
	}
	
	@Override
	public boolean disableTag(String arg0, String arg1) throws IOException,ReaderException 
	{
		Log.e("Test","disabletag");
		return true;
	}

	@Override
	public boolean editItemTag(String[] arg0, String[] arg1, String[] arg2,int arg3) throws IOException, ReaderException 
	{
		Log.e("Test","edititemtag");
		return true;
	}

	@Override
	public boolean editSubscription(String arg0, String arg1, String arg2,String[] arg3, int arg4) throws IOException, ReaderException 
	{
		Log.e("Test","editsub");
		return true;
	}

	@Override
	public void handleItemIdList(IItemIdListHandler itemIdHandler, long arg1)throws IOException, ReaderException 
	{
		try
		{
			HttpResponse response=doGet("http://api.ihackernews.com/page");
			ArrayList<String>itemList=new ArrayList<String>();
			JSONObject obj=new JSONObject(getContent(response));
			JSONArray items=obj.getJSONArray("items");
			
			for(int i=0;i<items.length();i++)
			{
				JSONObject itemObj=items.getJSONObject(i);
				itemList.add(itemObj.getString("id"));
			}
			itemIdHandler.items(itemList);
		}
		catch(Exception exc){exc.printStackTrace();}
	}

	@Override
	public void handleItemList(IItemListHandler itemHandler, long time)throws IOException, ReaderException 
	{
		mContext=getApplicationContext();
		HttpResponse response=doGet("http://api.ihackernews.com/page");
		try {
			String htmlStartContent="<html>HeckerNews don't supported Content. Please see the original Webside<br><a href=\"";
			String htmlEndContent="\">Original Link</a><br><br>";
			ArrayList<IItem>itemList=new ArrayList<IItem>();
			String content=getContent(response);
			if(content.startsWith("<html>"))
			{
				AndroidUtils.showToast(mContext, "Error by Server connection, try again later");
				throw new ReaderException("Error by Server connection, try again later");
			}
			JSONObject obj=new JSONObject(content);
			JSONArray items=obj.getJSONArray("items");
			int size=0;
			for(int i=0;i<items.length();i++)
			{
				JSONObject itemObj=items.getJSONObject(i);
				IItem item=new IItem();
				item.author=itemObj.getString("postedBy");
				item.title=itemObj.getString("title");
				item.link=itemObj.getString("url");
				item.subUid=SUB_ITEMS;
				item.content=htmlStartContent+item.link+htmlEndContent+"Created: "+itemObj.getString("postedAgo")+"<br> <a href=\"https://news.ycombinator.com/item?id="+item.uid+"\">Comments:"+itemObj.getString("commentCount")+"</a><br>Points: "+itemObj.getString("points");
				item.updatedTime=createPublishedTime(itemObj.getString("postedAgo"));
				item.publishedTime=System.currentTimeMillis();
				item.uid=itemObj.getLong("id")+"";
				if(size+item.getLength()>=MAX_TRANSACTION_LENGTH)
				{
					itemHandler.items(itemList, size);
					size=0;
					itemList.clear();
				}
				size=size+item.getLength();
				itemList.add(item);
			}
			itemHandler.items(itemList, size);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	private long createPublishedTime(String timeS)
	{
		String[]sp=timeS.split(" ");
		Calendar c=Calendar.getInstance();
		c.setTimeInMillis(System.currentTimeMillis());
		
		int hourOrMin=new Integer(sp[0]);
		if(sp[1].trim().startsWith("hour"))
		{
			int hour=c.get(Calendar.HOUR_OF_DAY);
			hour=hour-hourOrMin;
			if(hour<0)
			{
				hour=24+hour;//hour ist minus, daher + 
				int day=c.get(Calendar.DAY_OF_MONTH);
				day=day-1;
				if(day<1)
				{
					int month=c.get(Calendar.MONTH);
					month=month-1;
					if(month<0)
					{
						int year=c.get(Calendar.YEAR);
						c.set(Calendar.YEAR, year-1);
					}
					c.set(Calendar.MONTH, month);
				}
				c.set(Calendar.DAY_OF_MONTH, day);
			}
			c.set(Calendar.HOUR_OF_DAY, hour);
		}
		else if(sp[1].trim().startsWith("minute"))
		{
			int min=c.get(Calendar.MINUTE);
			min=min-hourOrMin;
			if(min<0)
				min=0;
			c.set(Calendar.MINUTE, min);
		}
		return (long)(c.getTimeInMillis()/1000);
	}

	@Override
	public void handleReaderList(ITagListHandler tagHandler,ISubscriptionListHandler subscriptionHandler, long time) throws IOException,ReaderException 
	{
		ArrayList<ITag>tagList=new ArrayList<ITag>();
		ITag tag=new ITag();
		tag.label="Starred Items";
		tag.type=ITag.TYPE_TAG_STARRED;
		tag.id=1;
		tag.uid=TAG_STARRED;
		tagList.add(tag);
		try {
			tagHandler.tags(tagList);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		ArrayList<ISubscription>subList=new ArrayList<ISubscription>();
		ISubscription sub=new ISubscription();
		sub.id=1;
		sub.uid=SUB_ITEMS;
		sub.title="HackerNews";
		sub.htmlUrl="http://api.ihackernews.com/";
		subList.add(sub);
		try {
			subscriptionHandler.subscriptions(subList);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	@Override
	public boolean markAllAsRead(String arg0, String arg1, String[] arg2,long arg3) throws IOException, ReaderException 
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean markAsRead(String[] arg0, String[] arg1) throws IOException,ReaderException 
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean markAsUnread(String[] arg0, String[] arg1, boolean arg2)throws IOException, ReaderException
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean renameTag(String arg0, String arg1, String arg2)throws IOException, ReaderException 
	{
		// TODO Auto-generated method stub
		return false;
	}
	private Context getContext() 
	{
		return mContext == null ? getApplicationContext() : mContext;
	}
	
	public HttpResponse doGet(String url) throws ClientProtocolException, IOException
	{
		HttpGet get=new HttpGet(url);
		return getClient().execute(get);
	}
	public String getContent(HttpResponse response)
	{
		HttpEntity enty=response.getEntity();
		try {
			InputStream stream=enty.getContent();
			StringBuffer sb=new StringBuffer();
			int len=10000;
			byte[]b=new byte[len];
			while(len>-1)
			{
				len=stream.read(b);
				if(len==-1)
					break;
				sb.append(new String(b,0,len));
			}
			stream.close();
			return sb.toString();
		} catch (Exception e) {
			e.printStackTrace();
		} 
		return null;
	}

	public DefaultHttpClient getClient() 
	{
		if (client == null) client = Utils.createHttpClient();
		return client;
	}
}
