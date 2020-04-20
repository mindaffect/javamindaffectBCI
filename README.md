mindaffectBCI
=============
This repository contains the [java](java.org) SDK code for the Brain Computer Interface (BCI) developed by the company [Mindaffect](https://mindaffect.nl).

File Structure
--------------
This repository is organized roughly as follows:

 - `messagelib ` - contains the main utopia-HUB code, which is the central message server from the mindaffectBCI decoder, and the java client library for the same hub.   
 - matrixSpellerGDX - this contains a java presentation example written using [libGDX](https://libgdx.badlogicgames.com/).  LibGDX is a cross platform high performance games engine for Java, which allows the development of BCI presentation systems for windows, macOS, linux, Android and iOS.
 - fakepresentation - this contains a fake presentation system, useful for testing the development of decoder or output modules
 - fakerecognition - this contains a fake recognication (aka. decoder) module, very useful for development of presentation or output system when you do not want to use an actual EEG system and human brain. (e.g. in the case when you either don't have the hardware, or don't want the hassel of putting on the cap and calibrating the system.)

Installing mindaffectBCI
------------------------

That's easy, just download this repository, and built the sub-project you are interested in.   All sub-project directories contain [Apache ANT](https://ant.apache.org/) and [Android-Studio](https://developer.android.com/studio) build files.  Thus, you should be able to build the projects either by running: `ant` in the project directory, or hitting build in android-studio.


Testing the mindaffectBCI SDK
-----------------------------

This SDK provides the functionality needed to add Brain Controls to your own applications.  However, it *does not* provide the actual brain measuring hardware (i.e. EEG) or the brain-signal decoding algorithms. 

In order to allow you to develop and test your Brain Controlled applications without connecting to a real mindaffect Decoder, we provide a so called "fake recogniser".  This fake recogniser simulates the operation of the true mindaffect decoder to allow easy development and debugging.  Before starting with the example output and presentation modules.  You can download the fakerecogniser from our [github page](https://github.com/mindaffect/javamindaffectBCI/tree/master/bin), or build it from the `fakerecogniser` directory.

You should start this fake recogniser by running, either ::
```
  bin/startFakeRecogniser.bat
```  
if running on windows, or  ::
```
  bin/startFakeRecogniser.sh
```
if running on linux/macOS

If successfull, running these scripts should open a terminal window which shows the messages recieved/sent from your example application.

Note: The fakerecogniser is written in [java](https://www.java.com), so you will need a JVM with version >8 for it to run.  If needed download from [here](https://www.java.com/ES/download/)