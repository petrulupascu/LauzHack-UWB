# LauzHack Vidinoti Example

This application fetch data from the differents localization systems Pozyx and/or TANGO, and output them with a common format.

### Installation
This application has been coded in python 3.5
If you intend to use the Pozyx system, you will need to install the additional package pyserial

Commands for linux
```bash
sudo apt install python3-pip
pip3 install pyserial
```

### Running the application
Go to the main script and set variable __use_pozyx__ and __use_tango__ to __True__ or __False__ depending on which system you want to use
Then simply use the command:
`python3 main.py`

If you use the Pozyx on linux, you will have to grant access to serial port with a command similar to this one:
```bash
sudo chmod 777 /dev/ttyACM0
```

### Data Output
Each acquisition systems provide different kind of measurements. The data are logged into a csv file (default ./data/data.csv).

The very first line correspond to the header, and all following line correspond to a single mesurement between 1 device and 1 anchor.
The columns are separated by commas (,) and missing values are completed by NaN.

## Pozyx

Pozyx is a new technology (2015) developed by a Start-Up using UWB for indoor and outdoor localization. The frequency range spans from 3.5 to 6.5 GHz. It consists of a set of anchors and one tag. The tag basically send a short signals to anchors and wait for the answer. Then it measures the Time Of Flight from which it compute the distance with a precision reaching the 10cm (According to the Pozyx web site) and at a distance up to 200m in line of sight.

More information on their website: https://www.pozyx.io/Documentation

### Pozyx Inventory
 - 4 Pozyx anchors (locators)
 - 4 cables USB type A to micro USB
 - 4 AC DC adapter DC5V 2.0A
 - 1 Pozyx shield for Arduino (tag)
 - 1 Arduino Uno R3
 - 1 cable USB type A to UBS type B

### Our Pozyx Application
The Pozyx shield (the tag) is plugged on the Arduino. The C# sketch on the Arduino sends the measures to the computer. It returns the distance between the tag and the Pozyx locators.

It is also possible to directly get the (x,y,z) coordinates computed by the Arduino. If you want to use these corrdinates, you will have to modify the anchors coordinates in the Arduino sketch: __./pozyx/Arduino/multiple_range.ino__
And then to upload the sketch to the Arduino.

## TANGO
The TANGO device can scan and learn and localize itself in an area.
https://developers.google.com/tango/

### TANGO Inventory
 - 1 TANGO device (Lenovo PHAB2)
 - 1 Cable USB type A to micro USB
 - 1 Adapter

### Our TANGO Application
We provide an application where the device will sends the localization data to your computer.

##### Getting Started with exploration
1) Choose your area an think about a coordinates systems. (choose origin and axis orientation)
1) Make sure that both of your computer and TANGO can connect to the same WiFi network.
1) On TANGO, start the application __U-Tango 2.1__ and go to __Explore__
1) Place the TANGO at the origin of your system, the camera looking in the Y direction
1) Press __Explore off__ to start learning the area. Then have a walk scanning your environment.
1) When your done, save your ADF file.

##### Getting started with Localization
1) Connect both TANGO and your computer to the same WiFi
1) On TANGO, start the application __U-Tango 2.1__ and go to __Locate__
1) Charge your ADF file
1) Press on the IP address to set it to the IP of your computer.
1) On the computer, start the main script `python3 main.py`
1) On TANGO, press Locate and start scanning the area. After a few seconds the TANGO will have recognized its position and will send your coordinates to the computer.


### Troubleshoot: When sockets don't work

If code is stucked at line "json_str, addr = sock.recvfrom(1024)" it means that nothing is coming out of the socket:
1) Enable either Wi-Fi or Ethernet but not both at the same time
2) Is the internet connection OK?
3) Verify that the IP used for the TANGO socket does match your actual IP
4) Is this computer connected to the same wifi network than the TANGO device?
5) Try using another WiFI.

If you get the following: "OSError: [Errno 98] Address already in use" at line "sock.bind((self.UDP_IP, self.UDP_PORT))"
1) The script is already running somewhere. Close all other running scripts.
2) FOR PYCHARM USERS: A script has been disconnected from PyCharm but it is still running.
    You need to kill it from terminal (linux):

      - Find python PID: `$ pidof python3.5`
      - Verify it correspond to the process you want to kill (second element of the following output)  `$ ps aux | grep python3.5`
      - Kill the process  `$ kill -9 $(pidof python3.5)`

If you get: "socket.gaierror: [Errno -2] Name or service not known" at line "socket.gethostbyname(socket.gethostname())"
1) Try disable and re-enable the Ethernet connection
2) FOR PYCHARM USERS: Try restart PyCharm

If you get: OSError: [Errno 99] Cannot assign requested address
1) Verify that the IP used for the TANGO socket does match your actual IP
2) Otherwise that's no luck, you have to reboot.