
# ![logo](/images/logo.png) An unofficial Reddit client for Android

An unofficial Reddit client for Android. The application allows for users to log in with their Reddit account and perform actions such as voting, commenting, and retrieving customized front page posts, without any ads.


## Screenshots

<div>
  <img src="/images/everything.gif" alt="Everything" width="275"/>
  <img src="/images/profile.gif" alt="User profile" width="275"/>
  <img src="/images/replying.gif" alt="Replying" width="275"/>
</div>


## Installation

The downloadable [APK](apk/app-release.apk) contains the application at commit `39d635231f8d73d4c8b838b71623038555f441ce` (#1810). To install the app download the APK, open it on your phone, and follow the steps shown. If the install fails you might have to temporarily [disable Google Play Protect](https://support.google.com/googleplay/answer/2812853) (remember to enable it again after the install).

`minSdkVersion` is 23 (Android 6.0).

See also: [build](#build)


## Features
- **Appearance**
  - Light and dark mode (true black)
  - Translated to English and Norwegian (bokmål)

- **Private browsing** 
  - While logged in, [enable private browsing](images/enable-private-browsing.png) to temporarily act as an anonymous, non-logged in user. [Disable at any time](images/disable-private-browsing.png) when attempting to do an action that requires a logged in user

- **Multiple accounts** 
  - Log in with [multiple accounts](images/multiple-accounts.png) and switch whenever you want to
  - Mark account as NSFW to override various NSFW settings, such as how images/thumbnails are blurred

- **Links**
  - [Show preview of links](images/show-link-previews.png) in comments
    - Optionally: show entire link (or only 1 line)
    - Optionally: don't show preview when the link is identical to the text
  - [Enlarge links](images/enlarge-links.png) to make them easier to click (up to 2.5x the size)
  - [Long press to show URL of a link](images/peek-url.png)

- **Posts**
  - [Peek text posts](images/peek-text-posts.gif) while scrolling in list of posts by long pressing on the post
  - [Control the size](images/control-size-of-post.png) of post content when opened, based on a percentage of the screen size
  - [Expand post](images/expand-post-content.gif) content at any time when scrolling through the comments
  - [Disable post from being collapsed](images/disable-post-collapse.gif) when scrolling in comments, with an independent size compared to when it can be collapsed.
    - This can be set as the default
  - [Filter posts](images/filter-posts.png) from subreddits from appearing in front page/popular/all

- **Comments**
  - [Navigate](images/navigate-top-level-comments.gif) between top level comments
    - Optionally with an animation, with a customizable threshold for how many comments to at most smoothly navigate between
    - Long press to go to first/last
  - Show [all](images/sidebars-all.png) sidebars, or [only](images/sidebars-only-one.png) one
    - [Fully customizable colors](images/comment-sidebar-color-dialog.gif) - [Preview](images/comment-sidebar-color-preview.png)
  - [Highlight comments](images/highlight-new-comments.png) added since the last time a post was viewed (can be toggled)

- **Inbox**
  - Check inbox for new messages every 15, 30, or 60 minutes (or never)
  - Optionally:
    - [Show badge](images/show-inbox-badge.gif) in the bottom bar
    - Show notifications

- **Misc**
  - [Slide away](images/slide-away.gif) opened posts, subreddits, user, images and so on
  - [Disable awards](images/show-awards.png) on posts and comments

- **Third party integration** (All these options can be [toggled indepedently](images/settings-load-third-party.png))
  - Imgur albums/galleries [are shown as](images/imgur-album-as-gallery.png) other Reddit galleries
  - Gfycat and Imgur videos are [loaded directly](images/gfycat-imgur-videos.png) from the source to always provide audio when possible (and show the size of the video), as the video URLs
  provided by Reddit [do not include any audio](images/gfycat-imgur-videos-not-loaded-directly.png).

- **Mod support**
  - Sticky posts and comments
  - Distinguish posts and comments as a moderator
  - View reports on posts and comments, with option to mark reports as ignored
  - Mark posts as NSFW/spoiler
  - Lock posts/comments


## Build

The APK is built with Android Studio Arctic Fox (2020.3.1). Other versions might be incompatible.

Create the file `secrets.properties` under [/app](app) and assign the following values:

To build release versions of the application, create [signing keys](https://developer.android.com/studio/publish/app-signing#sign-apk) and add the following values to `secrets.properties`:
```
FILE_PATH = <Path to the .jks file>
STORE_PASSWORD = <The keystore password>
KEY_ALIAS = <The alias of the key>
KEY_PASSWORD = <The key password>
```

Note: The signing values cannot be omitted. If you are not building a release version assign empty strings to these values (ie. `FILE_PATH = ""`)


Optionally, create an [Imgur app](https://api.imgur.com/oauth2/addclient) and pass the client ID to make requests towards the Imgur API. Some communication is done with Imgur, but these calls are optional and the app will be functional if this is omitted. Only userless/anonymous endpoints are used.
```
IMGUR_CLIENT_ID = <The Imgur client ID>
```

### Crashlytics

The application uses [Firebase Crashlytics](https://firebase.google.com/docs/crashlytics/get-started?platform=android) for crash reporting. Set up your Firebase projects and add the `google-services.json` file to [/app/app](/app/app). This can be omitted, but all Firebase references must be removed manually from the code (from Gradle files and [SettingsFragment](/app/app/src/main/java/com/example/hakonsreader/fragments/SettingsFragment.kt)).


## Acknowledgements

Thank you to Reddit for making this project possible by providing an open API.


### Libraries used

* [Retrofit](https://github.com/square/retrofit) - Used to create the wrapper for the Reddit API
  * [Gson](https://github.com/google/gson) - For API deserialization and other JSON needs
* [Glide](https://github.com/bumptech/glide) - Image processing, downloading, and caching
* [PhotoView](https://github.com/chrisbanes/PhotoView) - For zoomable ImageViews
* [RoundedImageView](https://github.com/vinc3m1/RoundedImageView) - To create circular ImageViews
* [Slidr](https://github.com/r0adkll/Slidr) - To easily create swipeable activities
  * With further support from this fork: https://github.com/kenilt/Slidr
* [Markwon](https://github.com/noties/Markwon) - Markdown text rendering in TextViews
* [ExoPlayer](https://github.com/google/ExoPlayer) - Used for playing videos
* [android-youtube-player](https://github.com/PierfrancescoSoffritti/android-youtube-player) - Used for playing YouTube videos
* [Ticker](https://github.com/robinhood/ticker) - A ticker style TextView that smoothly animates changes (by sliding characters up/down)
* [Material Popup Menu](https://github.com/zawadz88/MaterialPopupMenu) - For popup menus
* [Android Material Color Picker Dialog](https://github.com/Pes8/android-material-color-picker-dialog) - For picking colors
* [LeakCanary](https://github.com/square/leakcanary) - For identifying memory leaks

As well as various code snippets acknowledged throughout the code.