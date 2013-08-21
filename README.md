News+
-----------
Since Google has decided to shutdown Google Reader. We are trying hard to find alternative services. There are a lot of great services that many of you would like to use. That's why we have decided to support them as well. I want to introduce you a new app which is called News+. News+ is developed based on gReader great functional features. It also has an Extension system so that services could be integrated into News+. The extension api will be published and the documentation will be updated soon. You can find it under Github https://github.com/noinnion/newsplus. We hope that many of you could create great Extension for News+. 

Right now the app is still in Beta status. We will try to improve it with the time. There are still many bugs ;)

### Usage

You need to install News+ first to use the extensions. You can find the extensions on the Dropdown in the action bar.

News+ APK: Download and install it on you Android device.
https://github.com/noinnion/newsplus/raw/master/apk/NewsPlus_beta.apk

### Extensions

* Google Reader Clone (api clone): [Download](https://github.com/noinnion/newsplus/raw/master/apk/GoogleReaderClone_beta.apk)
* InoReader: [Download](https://github.com/noinnion/newsplus/raw/master/apk/GoogleReaderClone_beta.apk)
* Bazqux: [Download](https://github.com/noinnion/newsplus/raw/master/apk/BazquxExtension_beta.apk)
* TT-Rss: [Download](https://github.com/noinnion/newsplus/raw/master/apk/TtRssExtension_beta.apk) (NOTE: api_newsplus: https://github.com/hrk/tt-rss-newsplus-plugin/)
* NewsBlur: https://github.com/asafge/NewsBlurPlus
* CommaFeed: https://github.com/Athou/commafeed-newsplus
* SubReader: [Download](http://subreader.com/static/files/SubReaderExtension.apk)

We are trying to publish more :)

### API
For the Example extension and Google Reader extension there are 2 projects on Github. With the examples you can see how to implement an extension. (for Google Reader you need to set up Client_id + Client_secret in GoogleReaderAPI.java). To use the API you need to import reader-api-r1.0.jar into the projects. Documentation will come soon.

### Getting Started
The News+ API is pretty easy to get started with:

1. Add the API JAR to your Android project.
2. Create a new service that extends the ReaderExtension class.
3. Add the corresponding <service> tag to your AndroidManifest.xml file and add the required <intent-filter> and <meta-data> elements.

Once you have both News+ and your custom extension installed, you should be able to see your extension in the News+ extension list (press the title in actionbar).
