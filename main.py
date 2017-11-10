import os
import socket

# Imports classes from other python files
from data_writer import DataWriter
from tango_acquisition_thread import TangoAcquisition
from pozyx_acquisition_thread import PozyxAcquisition
import ubiment_parameters as UBI

# TODO: Hey fellow lauzhacker, make sure you allow serial access before using the Pozyx system:
# TODO: $ sudo chmod 777 /dev/ttyACM0
# CHOOSE THE SYSTEMS YOU WANT TO USE.
use_pozyx = False
use_tango = False

if not use_pozyx and not use_tango:
    print("WARNING: You have to enable at least 1 positioning system.")

# the file where you will output the data
file_path = os.path.join(os.path.dirname(__file__), 'data/data.csv')
# this get automatically your ip address. However you may want to verify with the command $ ifconfig
local_ip = socket.gethostbyname(socket.gethostname())
# serial port where you plugged the pozyx-arduino
usb_port = '/dev/ttyACM0'


# The logs are written in a csv file using the following headers
data_fields = [
    "timestamp",        # time in milliseconds in epoch time
    "device_id",        # the device we want to locate
    "system_id",        # TANGO has system id 7585, and Pozyx has 115200 (also used for socket port and baudrate)
    "anchor_id",        # anchor used to take the current measure
    "px",               # [px, py, pz] is the position of the device in world coordinate
    "py",
    "pz",
    "theta_x",          # [theta_x, theta_y, theta_z] corresponds to the orientation of the device (radian)
    "theta_y",
    "theta_z",
    "distance",         # distance between anchor and device
    "rssi",             # received signal strength
]


# Initialize the datawriter which will log the received measures in a csv file
datawriter = DataWriter(file_path, header=data_fields, verbose=True, verbose_interval=1)

# ----------- initialize the threads -------------------
threads_list = []
if use_pozyx:
    threads_list.append(PozyxAcquisition(usb_port=usb_port, datawriter=datawriter))
if use_tango:
    threads_list.append(TangoAcquisition(local_ip=local_ip, datawriter=datawriter))

# start the threads
try:
    # Start threads
    for thread in threads_list:
        thread.start()

    # wait for thread to finish
    for thread in threads_list:
        thread.join()

except (KeyboardInterrupt, SystemExit):
    datawriter.onDestroy()
    raise


# TODO TROUBLESHOOT when sockets dont work:
# If code is stucked at line "json_str, addr = sock.recvfrom(1024)"
#   1) Enable either Wi-Fi or Ethernet but not both at the same time
#   2) Is the internet connection OK?
#   3) Verify IP and port. (use ifconfig in terminal)
#   4) Is this computer connected to the same wifi network than the TANGO device?
#
# "OSError: [Errno 98] Address already in use" at line "sock.bind((self.UDP_IP, self.UDP_PORT))"
#   1) The script is already running somewhere. Close all other running scripts.
#   2) FOR PYCHARM USERS: A script has been disconnected from PyCharm but it is still running.
#       You need to kill it from terminal (linux):
#       Find python PID:
#           $ pidof python3.5
#       Verify it correspond to the process you want to kill (second element of the following output)
#           $ ps aux | grep python3.5
#       Kill the process
#           $ kill -9 $(pidof python3.5)
#
# "socket.gaierror: [Errno -2] Name or service not known" at line "socket.gethostbyname(socket.gethostname())"
#   1) Try disable and re-enable the Ethernet connection
#   2) FOR PYCHARM USERS: Try restart PyCharm
#
# OSError: [Errno 99] Cannot assign requested address
#   1) Verify IP and port in local_config.py
#   2) Otherwise that's no luck, you have to reboot.