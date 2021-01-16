
# ![logo](/images/logo.png) An unofficial Reddit client for Android

An unofficial Reddit client for Android. The application allows for users to log in with their Reddit account and perform actions such as voting, commenting, and retrieving customized front page posts. Mod support is limited.

Includes an Android wrapper for the Reddit API (for installed OAuth applications only) that leverages Kotlin coroutines, as well as providing basic persistence with [Room](https://developer.android.com/training/data-storage/room).


## Screenshots

<div>
  <img src="/images/subreddit.png" alt="Subreddit" width="275"/>
  <img src="/images/scrolling-in-posts.png" alt="Scrolling posts" width="275"/>
  <img src="/images/in-post.gif" alt="In post" width="275"/>
  <img src="/images/profile.png" alt="User profile" width="275"/>
  <img src="/images/replying.gif" alt="User profile" width="275"/>
  <img src="/images/search-for-subreddit.png" alt="User profile" width="275"/>
</div>


## Installation

The downloadable [APK](apk/app-release.apk) contains the application at commit `b92bcbd12aae24a809b8a45ed043bd152216c6e1` (#1114). To install the app via the APK download the APK, open it on your phone, and follow the steps shown.

### Build yourself

To build the application you have to create your own [Reddit application](https://www.reddit.com/prefs/apps) and sign up for API access.

Create the file `secrets.properties` under [/app](app) and assign the following values:
```
REDDIT_CLIENT_ID = <The randomly generated client ID of the application>
REDDIT_CALLBACK_URL = <The custom defined callback URL of the application>
REDDIT_USER_AGENT = <The User-Agent to use for API calls towards Reddit as defined here: https://github.com/reddit-archive/reddit/wiki/API#rules>
```

Optionally, create an [Imgur app](https://api.imgur.com/oauth2/addclient) and pass the client ID to make requests towards the Imgur API. Some communication is done with Imgur, but these calls are optional and the app will be functional if this is omitted. Only userless/anonymous endpoints are used for Imgur.
```
IMGUR_CLIENT_ID = <The Imgur client ID>
```

To build release versions of the application, create [signing keys](https://developer.android.com/studio/publish/app-signing#sign-apk) and add the following values to `secrets.properties`:
```
FILE_PATH = <Path to the .jks file>
STORE_PASSWORD = <The keystore password>
KEY_ALIAS = <The alias of the key>
KEY_PASSWORD = <The key password>
```


## Features
- **Posts**
  - [Control the size](images/control-size-of-post.png) of post content when opened, based on a percentage of the screen size.
  - [Expand post](images/expand-post-content.gif) content at any time when scrolling through the comments.
  - [Disable post from being collapsed](images/disable-post-collapse.gif) when scrolling in comments, with an independent size compared to when it can be collapsed.
    - This can be set as the default for all posts
  - [Filter posts](images/filter-posts.png) from subreddits from appearing in front page/popular/all

- **Comments**
  - [Navigate](images/navigate-top-level-comments.gif) between top level comments
    - Optionally with an animation, with a customizable threshold for how many comments to at most smoothly navigate between.
    - Long press to go to first/last
  - Show [all](images/sidebars-all.png) sidebars, or [only](images/sidebars-only-one.png) one
  - [Enlarge links](images/enlarge-links.png) to make them easier to click (up to 2.5x the size)
  - [Show preview of links](images/show-link-previews.png)
    - Optionally: show entire link (or only 2 lines)
    - Optionally: don't show preview when the link is identical to the text
  - [Highlight comments](images/highlight-new-comments.png) added since the last time a post was viewed (can be toggled).

- **Private browsing** - While logged in, [enable private browsing](images/enable-private-browsing.png) to temporarily act as an anonymous, non-logged in user. [Disable at any time](images/disable-private-browsing.png) when attempting to do an action that requires you to log in.

- **Mod support**
  - Sticky posts and comments
  - Distinguish posts and comments as a moderator
  - View reports on posts and comments, with option to mark reports as ignored


## Acknowledgements

Thank you to Reddit for making this project possible by providing an open API.


### Libraries used

* [Retrofit](https://github.com/square/retrofit) - Used to create the wrapper for the Reddit API
  * [Gson](https://github.com/google/gson) - For API deserialization and other JSON needs
* [Picasso](https://github.com/square/picasso) - Image processing, downloading, and caching
* [PhotoView](https://github.com/chrisbanes/PhotoView) - For zoomable ImageViews
* [CircleImageView](https://github.com/hdodenhof/CircleImageView) - To create circular ImageViews
* [Slidr](https://github.com/r0adkll/Slidr) - To easily create swipeable activities
* [Markwon](https://github.com/noties/Markwon) - Markdown text rendering in TextViews
* [ExoPlayer](https://github.com/google/ExoPlayer) - Used for playing videos
* [android-youtube-player](https://github.com/PierfrancescoSoffritti/android-youtube-player) - Used for playing YouTube videos
* [Ticker](https://github.com/robinhood/ticker) - A ticker style TextView that smoothly animates changes (by sliding characters up/down)
* [ProcessPhoenix](https://github.com/JakeWharton/ProcessPhoenix) - Used to restart the application
* [Stetho](https://github.com/facebook/stetho) - For network debugging

As well as various code snippets acknowledged throughout the code.