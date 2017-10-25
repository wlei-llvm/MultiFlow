MultiFlow
=====================
multi-source binding taint analysis 


### Summary
-  Multiflow can determine whether multiple sets of information flows can occur in one execution(Android CallBack Partition) or not. It's an extension to [FlowDroid](https://github.com/secure-software-engineering/soot-infoflow-android), since FlowDroid give all information flows independently, it can't distinguish their correlation.

### Building form Source 
- It's easy to build the source code if you know how to build FlowDroid , which you can refer to [FlowDroid wiki](https://github.com/secure-software-engineering/soot-infoflow-android/wiki)
- Then, you can do the following step:
1. put the code (multifinflow/\*) into the same directory as (infoflow/*)  
2. patch the customized FlowDroid to (infoflow/*) 
3. build and run

##### Customized FlowDroid
You should patch our customized FlowDroid to the original FlowDroid, in which we add some configurations and needs for multiple data flow facts manipulated
- comparing the original version, revisions happen in the following files :
```
infoflow/InfoflowConfiguration.java
infoflow/InfoflowManager.java
infoflow/android/SetupApplication.java
infoflow/android/TestApps/Test.java
infoflow/android/source/AccessPathBasedSourceSinkManager.java
infoflow/android/source/AndroidSourceSinkManager.java
infoflow/data/Abstraction.java
infoflow/problems/TaintPropagationResults.java
infoflow/problems/rules/SinkPropagationRule.java
infoflow/solver/fastSolver/FastSolverLinkedNode.java
infoflow/solver/fastSolver/IFDSSolver.java
infoflow/solver/fastSolver/InfoflowSolver.java
```


### Configuration
- As listed below, our tool includes the executable files(*.jar) after building. Also ,you need to add the configuration files for sources/sinks/taint wrappers/callbacks just like FlowDroid. 
- The only different configuration file is “CustomSourceAndSinks.txt”, which we use to customize the multiple sources to avoid unnecessary analysis. Please put all of them in the same directory.
```
*.jar
SourcesAndSinks.txt
AndroidCallbacks.txt
EasyTaintWrapperSource.txt
CustomSourcesAndSinks.txt
```
### Command to Run
For example :
```
java -Xmx32g -cp soot-multi-0.1.jar soot.jimple.infoflow.android.TestApps.Test /home/xxx/test/test.apk /home/xxx/software/android-sdk-linux/platforms 
--multimode CUSTOM --nocallbackssource
```
- Like FlowDroid.  The first parameter is the APK file, the second parameter is the Android SDK directory. 
Also, you should add the additional options : “--multimode CUSTOM/ALLCOMB/ALLCOMBCALLBACK --nocallbackssource”. 
- We support these running multimode: [CUSTOM, ALLCOMB, ALLCOMBCALLBACK] to configure the sources .
>1. “--multimode CUSTOM” means to use “CustomSourceAndSinks.txt” for custom analysis. 
>2. “--multimode ALLCOMB” will run all pairwise combinations of the sources in “SourcesAndSinks.txt” and end in a common function. 
>3. “--multimode ALLCOMBCALLBACK” will run all pairwise combinations of the sources in “SourcesAndSinks.txt”, and end in a callback. 
- “--nocallbackssource” is for performance reasons, since we are not ready to analyze callbacks sources.

##### Sources and Sinks Configuration
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
### Improving performance
all the optimization options of FlowDroid can also be used in MultiFlow. They are :
--aliasflowins
--aplength n
--nostatic
--nocallbacks
--pathalgo
--nopath