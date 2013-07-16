package com.noinnion.android.newsplus.extension.cnn;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.xmlpull.v1.XmlPullParser;

import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;

import com.androidquery.AQuery;
import com.androidquery.callback.AjaxCallback;
import com.androidquery.callback.AjaxStatus;
import com.androidquery.util.AQUtility;
import com.noinnion.android.reader.api.ReaderException;
import com.noinnion.android.reader.api.ReaderExtension;
import com.noinnion.android.reader.api.internal.IItemIdListHandler;
import com.noinnion.android.reader.api.internal.IItemListHandler;
import com.noinnion.android.reader.api.internal.ISubscriptionListHandler;
import com.noinnion.android.reader.api.internal.ITagListHandler;
import com.noinnion.android.reader.api.provider.IItem;
import com.noinnion.android.reader.api.provider.ISubscription;
import com.noinnion.android.reader.api.provider.ITag;

public class CnnExtension extends ReaderExtension {

//	private final String	TAG	= CnnExtension.class.getSimpleName();

	public String[][] CATEGORIES = new String [][] { 
		 {"CAT:Politics", "Politics"},
		 {"CAT:Sport", "Sport"},
         {"LABEL:Favorites", "Favorites"},
     };
	
	public String[][] FEEDS = new String [][] { 
		 {"FEED:http://www.engadget.com/rss.xml", "Top Stories", "http://edition.cnn.com/", ""},
		 {"FEED:http://rss.cnn.com/rss/edition_world.rss", "World", "http://edition.cnn.com/WORLD/", "CAT:Politics"},
		 {"FEED:http://rss.cnn.com/rss/edition_europe.rss", "Europe", "http://edition.cnn.com/EUROPE/", "CAT:Politics"},
		 {"FEED:http://rss.cnn.com/rss/edition_sport.rss", "Football", "http://edition.cnn.com/FOOTBALL/", "CAT:Sport"},
         {"FEED:http://rss.cnn.com/rss/edition_tennis.rss", "Tennis", "http://edition.cnn.com/TENNIS/", "CAT:Sport"},
     };
	

	@Override
	public void handleReaderList(ITagListHandler tagHandler, ISubscriptionListHandler subHandler, long syncTime) throws IOException, ReaderException {
		List<ITag> tags = new ArrayList<ITag>();
		List<ISubscription> feeds = new ArrayList<ISubscription>();
		
		try {
			for (String[] cat : CATEGORIES) {
				ITag tag = new ITag();
				tag.uid = cat[0];
				tag.label = cat[1];
				if (tag.uid.startsWith("LABEL")) tag.type = ITag.TYPE_TAG_LABEL;
				else if (tag.uid.startsWith("CAT")) tag.type = ITag.TYPE_FOLDER;
				tags.add(tag);
			}
			
			for (String[] feed : FEEDS) {
				ISubscription sub = new ISubscription();
				sub.uid = feed[0];
				sub.title = feed[1];
				sub.htmlUrl = feed[2];
				if (!TextUtils.isEmpty(feed[3])) {
					sub.addCategory(feed[3]);
				}
				feeds.add(sub);
			}
			
			tagHandler.tags(tags);
			subHandler.subscriptions(feeds);
		} catch (RemoteException e) {
			throw new ReaderException("remote connection error", e);			
		}
	}	
	
	@Override
	public void handleItemList(final IItemListHandler handler, long syncTime) throws IOException, ReaderException {
		try {
			String uid = handler.stream(); 
			if (uid.equals(ReaderExtension.STATE_READING_LIST)) {
				for (String[] f : FEEDS) {
					String url = f[0].replace("FEED:", "");
					parseItemList(url, handler, f[0]);
				}
			} else if (uid.startsWith("CAT:")) {
				for (String[] f : FEEDS) {
					if (f[2].equals(uid)) {
						String url = f[0].replace("FEED:", "");
						parseItemList(url, handler, f[0]);						
					}
				}
			} else if (uid.startsWith("FEED:")) {
				String url = handler.stream().replace("FEED:", "");
				parseItemList(url, handler, handler.stream());
			} else if (uid.startsWith("LABEL:")) {
				Log.e("Test", "No url for label");
			}
		} catch (RemoteException e1) {
			e1.printStackTrace();
		}
	}

	public void parseItemList(String url, final IItemListHandler handler, final String cat) throws IOException, ReaderException {
		AQuery aq = new AQuery(this);
		aq.ajax(url, XmlPullParser.class, new AjaxCallback<XmlPullParser>() {

			public void callback(String url, XmlPullParser xpp, AjaxStatus status) {
				List<IItem> items = new ArrayList<IItem>();
				IItem item = null;

				try {
					int eventType = xpp.getEventType();
					while (eventType != XmlPullParser.END_DOCUMENT) {

						String tag = xpp.getName();
						if (eventType == XmlPullParser.START_TAG) {
							if ("item".equals(tag)) {
								item = new IItem();
								item.subUid = "feed/" + url;
							} else if (item != null) {
								if ("title".equals(tag)) {
									item.title = xpp.nextText();
								} else if ("description".equals(tag)) {
									item.content = xpp.nextText();
								} else if ("link".equals(tag)) {
									item.link = xpp.nextText();
									item.uid = String.valueOf(item.link.hashCode());
								}
							}
						}
						else if (eventType == XmlPullParser.END_TAG) {
							if ("item".equals(tag)) {
								item.subUid = cat;
								items.add(item);
								item = null;
							}
						}

						eventType = xpp.next();
					}

					handler.items(items, INSERT_STRATEGY_DEFAULT);
				} catch (Exception e) {
					AQUtility.report(e);
				}

			}

		});

	}	
	
	@Override
	public void handleItemIdList(IItemIdListHandler handler, long syncTime) throws IOException, ReaderException {
		// TODO Auto-generated method stub
	}

	@Override
	public boolean markAsRead(String[]  itemUids, String[]  subUIds) throws IOException, ReaderException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean markAsUnread(String[]  itemUids, String[]  subUids, boolean keepUnread) throws IOException, ReaderException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean markAllAsRead(String s, String t, long syncTime) throws IOException, ReaderException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean editItemTag(String[]  itemUids, String[]  subUids, String[]  addTags, String[]  removeTags) throws IOException, ReaderException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean editSubscription(String uid, String title, String url, String[] tag, int action, long syncTime) throws IOException, ReaderException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean renameTag(String tagUid, String oldLabel, String newLabel) throws IOException, ReaderException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean disableTag(String tagUid, String label) throws IOException, ReaderException {
		// TODO Auto-generated method stub
		return false;
	}
	

}
