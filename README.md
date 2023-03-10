This is a fork of sendbird-uikit-android to change the admin message layout.

1- change files using the android studio
2- upload them to GIT https://github.com/trkfabi/sendbird-uikit-android/tree/main
3- go to git and create a new release with a new TAG (i.e.: 3.4.0.11)
4- go to https://jitpack.io/#trkfabi/sendbird-uikit-android and press "look up" and then "get" the latest tag so it gets compiled.
5- wait some time... go to the ti module and update the tag to the latest i.e.: 
repositories {
      google()
      mavenCentral()
    maven { url "https://jitpack.io" }
    maven { url "https://repo.sendbird.com/public/maven" }
}
dependencies {
    implementation 'com.github.trkfabi:sendbird-uikit-android:3.4.0.11'
}

6- ready to build

