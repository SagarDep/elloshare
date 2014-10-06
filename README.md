elloshare
=========

Want to download and install ElloShare?  Get an APK [here](http://www.weatherlight.com/elloshare).

ElloShare is a photo sharing app for Android.  It integrates with Gallery to make it easy to post images from your phone
into Ello.

Implementation is currently VERY rough.  It actually uses Crosswalk to sign in through a browser session to collect the
necessary tokens to authenticate, upload, and post.  If you want to build this yourself, you need to download the Crosswalk
project separately and follow the instructions to [include it as a library project](https://crosswalk-project.org/#documentation/embedding_crosswalk).

I suspect that this can all be done with UrlConnection objects or the default Android webview, but I had a hard time confirming that so far.

Please fork and modify.  Ello is about sharing art, and for many of us, that's mobile pictures.

None of this would have been possible without the [Ello research](https://gist.github.com/conatus/cc665f917d5558c123bc) done by github user [conatus](https://github.com/conatus).


