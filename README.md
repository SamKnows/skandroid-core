# skandroid-core
` fcc_skcore` branch

## Introduction
This is the GitHub repository that contains the source-code for the SamKnows Speed test app 
for Android. The SamKnows speed test app is a tool for accurately measuring mobile broadband. 
This repository contains the common code for the app, related to the core functionality and 
common UI elements.

All client specific builds of the SamKnows speed test app use skandroid-core as a submodule 
code for their core functionality. The custom code for client specific builds can be found 
in their own repositories. Please note that some client specific (custom code) repositories 
may be private as per client requirements.

SamKnows has made this code public in line with its commitment of open data and a transparent 
technical methodology.

You can find more infomation on SamKnows at www.samknows.com

## Modernization
Project was modernized (i.e. converted to work w/recent Android Studio instead of Eclipse)
in Sep 2018

### Known issues following modernization

#### Facebook integration broken
It appears that posting to facebook no longer works (even with facebook app installed
and logged in on device); this is not unexpected as the facebook SDK bundled with this
app is several years old, and facebook regularly deprecates old SDKs.
