# LauzHack Vidinoti Example

This application fetch data from the differents localization systems Pozyx and/or TANGO, and output them with a common format.

## Installation on Linux
```
sudo apt install python3-pip
pip3 install numpy pyserial matplotlib ntplib
pip3 install pylocus
sudo apt-get install python-tk
```

## Output
Each acquisition systems provide different kind of measurements.
Currently we use csv files.

Each line corresponds to a single mesurement between 1 device and 1 anchor.
The columns are sparated by commas (,)

Missing values are completed by NaN


## When running on linux
Need to execute the following line to allow access to pozyx port:
    sudo chmod 777 /dev/ttyACM0