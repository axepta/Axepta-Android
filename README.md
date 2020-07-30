# Axepta-Android  

[ ![Download](https://api.bintray.com/packages/exozetag/maven/axepta-android-sdk/images/download.svg) ](https://bintray.com/exozetag/maven/axepta-android-sdk/_latestVersion) [![API](https://img.shields.io/badge/API-16%2B-brightgreen.svg?style=flat)](https://android-arsenal.com/api?level=16)  [![Gradle Version](https://img.shields.io/badge/gradle-6.5.1-green.svg)](https://docs.gradle.org/current/release-notes.html)

## Introduction

Android SDK for [Axepta](https://www.axepta.com/).


## Add the SDK to Your Project

    implementation 'com.axepta.sdk:lib:<VERSION>'

## How to build

    gradlew installDebug

#### Build Requirements

- JDK7, JDK8
- Android Build Tools 29.0.3
- Android SDK 29
- Android min SDK API 17

## How to use

Please refer to the PDF file in this repo [How_To_Implement_in_Android.pdf](docs/How_To_Implement_In_Android_Project.pdf) for further details on how to use the SDK

To make this example app working two basic actions need to be done.
- You need to create a merchant account at Axepta and provide your credentials in the app
- You need to provide payment parameters for the according payment methods. For more information on mandatory and optional parameters see [Axepta](https://docs.axepta.bnpparibas/display/DOCBNP/English+documentation)


## License

Please refer to this repo's [license file](LICENSE).
