# Kaiwa- Video Calling App

Kaiwa is a real-time video calling Android application built with Jetpack Compose and Agora SDK. It enables high-quality video calls between users with broadcaster and audience roles, making it perfect for live streaming and interactive video scenarios.

Please note: The app apk and the source code are included in this github but after directly installing the app the video conferencing wont work right away as agora tokens are generated based on the channel name and everyone who has to join the same channel name should 
should have the same token we can implement a javascript program that allowes users to create their channels and unique tokens which can make this app more production ready for now we are using agora test tokens that expire after a day.

If you want to download the app without opening the android studio [Here is the apk link](https://drive.google.com/file/d/12BYygsG8qkOItvSGmqUhhcClE0f3ImgZ/view?usp=sharing)

## App size
- Installed as apk - 257MB
- Installed from android studio - 95MB

--APK (250MB):

Contains native libraries (.so files) for ALL supported CPU architectures (armeabi-v7a, arm64-v8a, x86, x86_64)
Includes all resources and assets at full resolution
No optimization for different device configurations
In our case, Agora SDK's native libraries for all architectures contribute significantly to the size


--App Bundle (135MB):

Android App Bundle (AAB) is an upload format that includes all resources and code
Google Play Store generates optimized APKs based on device configuration
When installed from Play Store, users only get the code and resources specific to their device
Smaller because Play Store will create split APKs for different architectures


--Android Studio Install (95MB):

Android Studio knows your test device's exact architecture and configuration
Installs only the necessary native libraries for your specific device architecture
Excludes unused resources and architectures
Most efficient because it's targeted for a specific device

## Features

- Real-time Video Calls: High-quality, low-latency video communication
- Role-based Access: Broadcaster role with full audio/video controls and audience role for viewing only
- Interactive Controls:
  - Camera toggle (Broadcasters only)
  - Microphone toggle (Broadcasters only)
  - End call functionality (All users)
- Modern UI: Built with Jetpack Compose for a fluid, native Android experience
- Permission Handling: Runtime permission management for camera and microphone
- *Error Handling*: Comprehensive error handling and user feedback



## Tech Stack

**Kotlin**: Primary programming language

**Jetpack Compose**: UI framework

**Agora SDK**: Real-time communication

**Material Design 3**: UI components and styling

**Coroutines**: Asynchronous programming

**Android Permissions**: Runtime permission handling



## Prerequisites

- Android Studio
- JDK 11 or higher
- Android SDK version 24 or higher
- An Agora account and App ID
- Minimum Android target SDK: API 33 


## Setup & Installation

Clone the Repository
   
```bash
git clone https://github.com/Rhapsodictree/kaiwa.git

```
Configure Agora Credentials

- Create a file named config.kt in app/src/main/kotlin+java/com/example/kaiwa/config/
- Add your Agora credentials:
      
      package com.example.kaiwa.config
      const val APP_ID = "your-app-id"
      const val token = "your-token" 

    
      
  Build the Project

   - Open the project in Android Studio
   - Sync Gradle files
   - Build the project
   
 ##  Run the App
 - Connect to an Android device or start an emulator
 -  Run the app using Android Studio
 - Grant necessary permissions when prompted
 - Enter a channel name and select your role (Broadcaster/Audience)
 - Start video calling!!


    
## Functionality

### Broadcaster Role:
- Can toggle camera on/off
- Can toggle microphone on/off
- Can end the call
- Video stream is visible to all participants

### Audience Role:
- Can view broadcasters' streams
- Can end the call
- No audio/video controls (view only)


## Permissions

The app requires the following permissions:
- **CAMERA** - For video streaming
- **RECORD_AUDIO** - For audio streaming
- **INTERNET** - For network communication

## Configurations

### Build Configuration

```gradle
android {
    namespace = "com.example.kaiwa"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.kaiwa"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }
```
### Key Dependencies

```gradle
dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.ui)
    // ... other dependencies
}
```
##  Troubleshooting

Common issues and solutions:

Permission Denied
   - Ensure all required permissions are granted in device settings
   - Reinstall the app if permissions are stuck

Connection Issues
   - Verify internet connection
   - Check Agora App ID configuration
   - Ensure token (if used) is valid and not expired

Video Not Showing
   - Check camera permissions
   - Verify device has a compatible camera
   - Ensure you're in Broadcaster role if trying to stream



## Acknowledgements

- [Agora.io](https://www.agora.io/) for their excellent RTC SDK
- [Jetpack Compose](https://developer.android.com/jetpack/compose) documentation
- [Android Developers Community](https://developer.android.com/community)
- [Claude](https://claude.ai/new) for error logging

## Authors

- Nischay Kashyap [@Rhapsodictree](https://github.com/Rhapsodictree)
- Sagarika Barman [@barmansagarika](https://github.com/barmansagarika)
