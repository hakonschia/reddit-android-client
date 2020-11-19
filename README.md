
# ![logo](/images/logo.png) An unofficial Reddit client for Android

An unofficial Reddit client for Android. The application allows for users to log in with their Reddit account and perform actions such as voting, commenting, and retrieving customized front page posts. Mod support is limited.

Includes an Android wrapper for the Reddit API (for installed OAuth applications only) that provides basic functionality, as well as basic persistence with [Room](https://developer.android.com/training/data-storage/room).


## Screenshots

<div>
  <img src="/images/subreddit.png" alt="Subreddit" width="275"/>
  <img src="/images/scrolling-in-posts.png" alt="Scrolling posts" width="275"/>
  <img src="/images/in-post.gif" alt="In post" width="275"/>
  <img src="/images/profile.png" alt="User profile" width="275"/>
  <img src="/images/replying.png" alt="User profile" width="275"/>
  <img src="/images/search-for-subreddit.png" alt="User profile" width="275"/>
</div>


## Installation

The downloadable [APK](apk/app-release.apk) contains the application at commit `6f67ae52d8f747a1ec6c8d3ec6a76254e9172353` (#826). To install the app via the APK download the APK and open it on your phone and follow the steps shown.

To retrieve the latest version clone the repository and install manually via Android Studio (this will be a debug version).


## Acknowledgements

Thank you to Reddit for making this project possible by providing an open API.


### Libraries used

* [Retrofit](https://github.com/square/retrofit) - Used to create the wrapper for the Reddit API
  * [Gson](https://github.com/google/gson) - For API serialization and other JSON needs
* [Picasso](https://github.com/square/picasso) - Image processing, downloading, and caching
* [PhotoView](https://github.com/chrisbanes/PhotoView) - For zoomable ImageViews
* [CircleImageView](https://github.com/hdodenhof/CircleImageView) - To create circular ImageViews
* [Slidr](https://github.com/r0adkll/Slidr) - To (very) easily create swipeable activities
* [Markwon](https://github.com/noties/Markwon) - Markdown text rendering in TextViews
* [ExoPlayer](https://github.com/google/ExoPlayer) - Used for playing videos
* [Ticker](https://github.com/robinhood/ticker) - A ticker style TextView that smoothly animates changes (by sliding characters up/down)
* [ProcessPhoenix](https://github.com/JakeWharton/ProcessPhoenix) - Used to restart the application

As well as various code snippets acknowledged throughout the code.