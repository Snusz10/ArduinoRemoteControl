The ArduinoProject folder is a java project that is intended to be edited with the AndroidStudio IDE. You can make changes to it there and rebuild a new apk to install on the tablet if needed

The CoreController.apk file is a built version of the ArduinoProject source code. If no changes have been made to the project as of today (November 17, 2024; Version 1.0.0) then you can use this to install the android application on whatever device you would like

ESP32Final is a project meant to be open and modified using ArudinoIDE. You can make changes if necessary. Opening it up in the arduino ide is how you can flash an ESP32 chip with the code that it should be running.

The fritzing sketch requires the "Fritzing" Application to be opened and modified, although this isn't really necessary as it only shows the wiring for the ESP32 that has been copied and pasted into the "Fritzing Diagram" pdf file. This is also slightly out of date with no request from Core to have the documentation updated. Refer to the ESP32Code.ino source file to understand what pins should connect to the various components of the device.