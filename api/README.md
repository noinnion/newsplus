API
---
With the GoogleReaderClone extenion you can see how to implement an extension. To use the API you need to import reader-api-r1.jar into the projects. Documentation will come soon.

### Getting Started
The News+ API is pretty easy to get started with:

1. Add the API JAR (reader-api-r1.jar) to your Android project.
2. Create a new service that extends the ReaderExtension class.
3. Add the corresponding <service> tag to your AndroidManifest.xml file and add the required <intent-filter> and <meta-data> elements.

Once you have both News+ and your custom extension installed, you should be able to see your extension in the News+ extension list (press the title in actionbar).
