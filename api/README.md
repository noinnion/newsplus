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


