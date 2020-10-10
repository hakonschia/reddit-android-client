
# ![logo](/images/logo.png) An unofficial Reddit client for Android

An unofficial Reddit client for Android. The application allows for users to log in with their Reddit account and perform actions such as voting, commenting, and retrieving customized front page posts. There is currently no mod support, and is not planned at this time.

Includes a Java wrapper for the Reddit API that provides basic functionality.


## Screenshots

<div>
  <img src="/images/scrolling-in-posts.png" alt="Scrolling posts" width="275"/>
  <img src="/images/profile.png" alt="User profile" width="275"/>
  <img src="/images/in-post.png" alt="In post" width="275"/>
</div>


## Installation

The downloadable [APK](apk/app-debug.apk) contains the application at commit `7f6436575137a358f5c785b7f9973adf24f8ca74`. To install the app via the APK download the APK and open it on your phone and follow the steps shown.

To retrieve the latest version clone the repository and install manually via Android Studio.


## Thank you to these libraries

* [Retrofit](https://github.com/square/retrofit) - Used to create the wrapper for the Reddit API
* [Picasso](https://github.com/square/picasso) - Used to download, cache, and process images
* [Slidr](https://github.com/r0adkll/Slidr) - To (very) easily create swipeable activities
* [zoomage](https://github.com/jsibbold/zoomage) - For zoomable ImageViews
* [Markwon](https://github.com/noties/Markwon) - For rendering of markdown text in TextViews
* [ExoPlayer](https://github.com/google/ExoPlayer) - Used for playing videos

As well as various code snippets acknowledged throughout the code, and Reddit for providing an open API to make this project possible.
