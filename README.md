# VidSnapKit-Ultimate
Same as VidSnapKit but multi-platform project with JVM jar an android aar support. Social media post downloading library wriiten in Kotlin for Android and JVM machines. Compatible with Java projects too.

Add below lines to your gradle file.

    dependencies {
        ...
        implementation 'io.github.udhayarajan:VidSnapKit:<LATEST_VERSION>'
    }
    
You can see `LATEST_VERSION` from release of this current respository.

NOTE: You no need to add beta `v` and `beta` tag in implementaion line. Just add numerical version alone (eg: `v2.0.1-beta` as  `implementation 'io.github.udhayarajan:VidSnapKit:2.0.1`)

Also make sure to include `mavenCentral()` repository


    repositories {
        mavenCentral()
    }


## Supported:
 - Instagram(Reels, Stories, Carousel Post, Image Post)
 - Facebook
 - DailyMotion
 - LinkedIn
 - ShareChat
 - Vimeo
