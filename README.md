MultiFlow
=====================
 Extend multi-sources analysis to [FlowDroid](https://github.com/secure-software-engineering/soot-infoflow-android)


### How To Run
#### configuration
As listed below, our tool includes the executable files(*.jar) and the configuration files for sources/sinks/taint wrappers/callbacks just like FlowDroid. The only different configuration file is “CustomSourceAndSinks.txt”, which we use to customize the multiple sources to avoid unnecessary analysis. Please put all of them in the same directory.
```
*.jar
SourcesAndSinks.txt
AndroidCallbacks.txt
EasyTaintWrapperSource.txt
CustomSourcesAndSinks.txt
```
### Command 
It’s also the same as FlowDroid. The first parameter is the APK file, and the second parameter is the Android SDK directory. Also, you should add the additional options : “--multimode CUSTOM/ALLCOMB --nocallbackssource”. We support two running multimode: [CUSTOM, ALLCOMB] to configure the sources . “--multimode CUSTOM” means to use “CustomSourceAndSinks.txt” for custom analysis. “--multimode ALLCOMB” will run all pairwise combinations of the sources in “SourcesAndSinks.txt”.  “--nocallbackssource” is for performance reasons, since we are not ready to analyze callbacks sources.
For example :
```
java -Xmx32g -cp soot-multi-0.1.jar soot.jimple.infoflow.android.TestApps.Test /home/xxx/test/test.apk /home/xxx/software/android-sdk-linux/platforms 
--multimode CUSTOM --nocallbackssource
```
Sources and Sinks Configuration
In mode “CUSTOM”, our tool use custom sources patterns but share common sinks. So, you need write common sinks in “SourcesAndSinks.txt”, and write custom sources patterns in “CustomSourcesAndSinks.txt”. The format of sources and sinks in each line is the same as SourcesAndSinks.txt provided by SUSI. We use “%%%” to separate the different custom sources patterns in “CustomSourcesAndSinks.txt” . 
In mode “ALLCOMB”, you just need write all the sources and sinks in“SourcesAndSinks.txt”.
Example of “CustomSourcesAndSinks.txt”:
```
<android.telephony.TelephonyManager: java.lang.String getDeviceId()> android.permission.READ_PHONE_STATE -> _SOURCE_
<android.telephony.TelephonyManager: java.lang.String getSubscriberId()> android.permission.READ_PHONE_STATE -> _SOURCE_
<android.telephony.TelephonyManager: java.lang.String getSimSerialNumber()> android.permission.READ_PHONE_STATE -> _SOURCE_
<android.telephony.TelephonyManager: java.lang.String getLine1Number()> android.permission.READ_PHONE_STATE -> _SOURCE_
%%%
<android.location.Location: double getLatitude()> -> _SOURCE_
<android.location.Location: double getLongitude()> -> _SOURCE_
```

