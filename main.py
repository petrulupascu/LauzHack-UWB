import os

from data_writer import DataWriter
from pozyx_acquisition_thread import PozyxAcquisition
from tango_acquisition_thread import TangoAcquisition
import ubiment_parameters as UBI


# CHOOSE THE SYSTEMS YOU WANT TO USE.
pozyx = False
tango = False

file_path = os.path.join(os.path.dirname(__file__), 'data/data.csv')

# The logs are written in a csv file using the following headers
data_fields = [
    "timestamp",        # time in milliseconds in epoch time
    "device_id",        # the device we want to locate (previously named tag_id)
    "system_id",        # TANGO has system id 7585, and Pozyx has 115200
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


# TODO when sockets dont work:
# If code is stucked at line "json_str, addr = sock.recvfrom(1024)"
#   1) Enable either Wi-Fi or Ethernet but not both at the same time
#   2) Is the internet connection OK?
#   3) Verify IP and port
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


threads_list = []

# initialize the threads

# The datawriter will log the received measures in a csv file
datawriter = DataWriter(file_path, header=UBI.data_fields)

if pozyx:
    threads_list.append(PozyxAcquisition(usb_port=local_config.POZYX_USB_PORT, datawriter=datawriter,
                                         baudrate=UBI.POZYX_BAUDRATE))
if tango:
    threads_list.append(TangoAcquisitionNoFP(UDP_IP=local_config.UDP_IP, UDP_PORT=UBI.TANGO_PORT,
                                             datawriter=datawriter))

# start the threads
try:
    # Start threads
    for thread in threads_list:
        thread.start()

    # Attend que les threads se terminent
    for thread in threads_list:
        thread.join()

except (KeyboardInterrupt, SystemExit):
    datawriter.onDestroy()
    raise
