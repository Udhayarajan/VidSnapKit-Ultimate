# VidSnapKit
VidSnapKit is a Kotlin Multi-Platform library that allows users to download videos from Instagram (Reels, Stories, Carousel Posts), Facebook, DailyMotion, LinkedIn, ShareChat, Vimeo and Likee. The library is built with support for Android and JVM, and can be easily included in other Java/Kotlin projects or Android apps.

## Installation⚙️
Add the following dependency to your app's build.gradle file:


    implementation 'io.github.udhayarajan:VidSnapKit:<release_version>'

You can see LATEST_VERSION from release of this current respository.

NOTE: You no need to add `v` and `beta` tag in implementaion line. Just add numerical version alone (eg: v2.0.1-beta as implementation `'io.github.udhayarajan:VidSnapKit:2.0.1'`)

Sync your project with Gradle.


## Usage⚒️
To download a video, call the downloadVideo function with the URL of the video and the platform it is from (Instagram, Facebook, DailyMotion, LinkedIn, ShareChat, or Vimeo) and cookies if applicable.


    val extractor = Extractor.findExtractor("https://www.instagram.com/p/B_5qXN6j5sd")
    extractor?.let {
                    cookies?.let { cookie ->
                        it.cookies = cookie
                    }
                    it.start { res ->
                        //Get data from here
                    }
                }
The function will return a Extractor object representing the extracted video.

## Note⚠️
- Make sure you have the necessary permissions to download the video.
- The library is intended for personal use only and should not be used for copyright infringement.
- The library is for educational and testing purposes and is not intended for commercial use.

## Contributing🤝
If you want to contribute to this project, feel free to open a pull request or create an issue.

## Support🔁
If you have any issues or questions, please open an issue.

## License📝
This project is licensed under the Apache 2.0 License.

## Author🖋️
This project is created by [Udhayarajan M](https://linktr.ee/udhayarajan_m)

## Cloud version☁️
This project is available as cloud API version vist [Rapid API](https://rapidapi.com/mudhayarajan2013/api/vidsnap) to get it. **IT IS FREE CURRENTLY**

## Resources📲
A sample app using this library can be found at this URL: https://github.com/Udhayarajan/VidSnapKit/tree/master/sample%20app

## Additional things➕
- You can check the sample app for more details on how to use this library
- Cookies needed to be passed if required.

## Sponsorship☕
[!["Buy Me A Coffee"](https://img.buymeacoffee.com/button-api/?text=Buy%20me%20a%20coffee&emoji=&slug=udhayarajan&button_colour=5F7FFF&font_colour=ffffff&font_family=Cookie&outline_colour=000000&coffee_colour=FFDD00)](https://www.buymeacoffee.com/udhayarajan)


