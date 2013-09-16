API
---
With the GoogleReaderClone extenion you can see how to implement an extension. To use the API you need to import reader-api-r1.jar into the projects. Documentation will come soon.

### Getting Started
The News+ API is pretty easy to get started with:

1. Add the API JAR (reader-api-r1.jar) to your Android project.
2. Create a new service that extends the ReaderExtension class.
3. Add the corresponding <service> tag to your AndroidManifest.xml file and add the required <intent-filter> and <meta-data> elements.

Once you have both News+ and your custom extension installed, you should be able to see your extension in the News+ extension list (press the title in actionbar).


### Registering extensions
An extension is simply a service that the News+ process binds to. Subclasses of this base ReaderExtension class should thus be declared as <service> components in the application's AndroidManifest.xml file.
The main News+ app discovers available extensions using Android's Intent mechanism. Ensure that your service definition includes an <intent-filter> with an action of ACTION_EXTENSION. Also make sure to require the PERMISSION_READ_EXTENSION_DATA permission so that only News+ can bind to your service and request updates. Lastly, there are a few <meta-data> elements that you should add to your service definition:

* protocolVersion (required): should be 1.
* description (required): should be a one- or two-sentence description of the extension, as a string.
* loginActivity (required): should be the qualified component name for a login activity in the extension's package that News+ can start when the user choose the extension

### Subclassing ReaderExtension
Subclasses must implement following methods:

**public abstract void handleReaderList(ITagListHandler tagHandler, ISubscriptionListHandler subHandler, long syncTime) throws IOException, ReaderException**
* Called to retrieve feeds, tags or folders from the service. It will initialize the feeds and folder structure for the displaying them in the app.
 * @param tagHandler 	Handles tag and folder entries from the service.
 * @param subHandler 	Handles feed entries from the service.
 * @param syncTime 		Time of synchronization

**public abstract void handleItemList(IItemListHandler handler, long syncTime) throws IOException, ReaderException**
* Called to retrieve items from the service. Sync parameters can be retrieved from IItemListHandler
 * @param handler 	Handles item entries from the service.
 * @param syncTime 	Time of synchronization

**public abstract void handleItemIdList(IItemIdListHandler handler, long syncTime) throws IOException, ReaderException**
* Is useful for 2-way synchronization. Retrieving items ids from the server to know the changes on the server.
 * @param handler 	Handles item id entries from the service.
 * @param syncTime 		Time of synchronization

**public abstract boolean markAsRead(String[] itemUids, String[] subUids) throws IOException, ReaderException**
* Mark items as read.
 * @param itemUids 	Item ids which has to be marked as read
 * @param subUids 	Some services need to have corresponding subscription ids
 * @return boolean 	true if success else false

**public abstract boolean markAsUnread(String[] itemUids, String[] subUids, boolean keepUnread) throws IOException, ReaderException**
* Mark items as unread.
 * @param itemUids 		Item ids which has to be marked as unread
 * @param subUids 		Some services need to have corresponding subscription ids
 * @param keepUnread 	Set keep unread status
 * @return boolean 	true if success else false

**public abstract boolean markAllAsRead(String stream, String title, String[] excludedStreams, long syncTime) throws IOException, ReaderException**
* Mark all items as read for a specific stream.
 * @param stream 		Stream(subscriptions, tags or folders) that has to be marked as read. If stream == null then mark all as read
 * @param label 		Some services need to have stream label
 * @param excludedStreams Streams which are excluded from sync
 * @param syncTime 		Time of synchronization
 * @return boolean 	true if success else false

**public abstract boolean editItemTag(String[] itemUids, String[] subUids, String[] addTags, String[] removeTags) throws IOException, ReaderException**
* Modify tags for items.
 * @param itemUids 		Item ids
 * @param subUids 		Corresponding subscription ids
 * @param addTags 		Tags to be added to items
 * @param removeTags 	Tags to be removed from items
 * @return boolean 	true if success else false

**public abstract boolean editSubscription(String uid, String title, String url, String[] tags, int action, long syncTime) throws IOException, ReaderException**
* Modify subscriptions.
 * @param uid 			Subscription id which has to be modified
 * @param title 		Subscription title
 * @param url 			Subscription url
 * @param tags 			Tags to be added/removed from subscription
 * @param action 		{SUBSCRIPTION_ACTION_EDIT, SUBSCRIPTION_ACTION_SUBSCRIBE, SUBSCRIPTION_ACTION_UNSUBSCRIBE, SUBSCRIPTION_ACTION_ADD_LABEL, SUBSCRIPTION_ACTION_REMOVE_LABEL}
 * @param syncTime 		Time of synchronization
 * @return boolean 	true if success else false

** public abstract boolean renameTag(String uid, String oldLabel, String newLabel) throws IOException, ReaderException **
* Rename a tag/folder
 *
 * @param uid 			Tag id
 * @param oldLabel 		Old label
 * @param newLabel 		New Label
 * @return boolean 	true if success else false

**	public abstract boolean disableTag(String uid, String label) throws IOException, ReaderException **
* Remove a tag/folder
 * @param uid 			Tag id
 * @param label 		Tag label
 * @return boolean 	true if success else false
