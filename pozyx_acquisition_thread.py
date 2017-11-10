import serial
from time import time
from threading import Thread
from main import data_fields


# TODO: Hey fellow lauzhacker, Have you placed the 4 Pozyx anchors? You may want to measure their x,y,z positions.
# The Arduino will send you some data through the serial port.
# Among these data there are the coordinates (x,y,z) that the Arduino computes for you.
# If you want to use them, you will have to modify the anchors coordinates in the Arduino sketch:
# pozyx/Arduino/multiple_range.ino
# Finally you will have to upload the sketch to the Arduino.
#

"""
Thread class for acquiring Pozyx data. On linux you must enter the following command in the terminal to allow USB access
sudo chmod 777 /dev/ttyACM0
"""

class PozyxAcquisition(Thread):
    def __init__(self, usb_port, baudrate=115200, datawriter=None):
        Thread.__init__(self)
        self.setDaemon(True)
        self.datawriter = datawriter

        # The baudrate have to match the one used by the Arduino sketch
        self.baudrate = baudrate
        self.usb_port = usb_port

        self.device_id = 28486  # id of the Pozyx shield (the one which is plugged on the Arduino)
        print("Pozyx init()")

        # time window of boxfilter
        self.avg_millis = 250

    def run(self):
        print("Pozyx run()")
        # We connect to the USB port. This action will have the effect of reseting the Arduino
        # We could do it at init time, but we would have to handle the close case
        with serial.Serial(self.usb_port, baudrate=self.baudrate, timeout=1) as ser:
            # The first lines contain some info about the Arduino etc..
            # That is why we skip these lines until the ranging actually starts

            line = ""
            while "READY FOR TIME SYNC" not in line:
                line = ser.readline().decode()
                if len(line) > 0:
                    print(line[:-1])

            # --------------- TIME SYNCHRONIZATION ------------------
            # All this synchronization stuff is useful when working with an Arduino-Wifi. Otherwise it's kind of useless
            # However the Arduino
            Nb_pingpong = 100
            roundtrip = 0
            for k in range(Nb_pingpong):
                line = ""
                ping_t = time()
                ser.write(("0xHEXA INTE INTE SH" + '\n').encode())
                while "0xHEXA INTE INTE SH" not in line:
                    line = ser.readline().decode()
                roundtrip += time() - ping_t
            ser.write("END SYNC\n".encode())
            roundtrip *= 0.5 * 1000 / Nb_pingpong
            roundtrip = int(roundtrip)
            print("Estimated round trip time: " + str(roundtrip))

            line = ""
            while "TIME0=" not in line:
                line = ser.readline().decode()
                if len(line) > 0:
                    print(line[:-1])
            arduino_t1 = int(line.split("=")[1])
            t0 = int(time() * 1000) - arduino_t1 - roundtrip

            line = ""
            while "START Ranging:" not in line:
                line = ser.readline().decode()
                if len(line) > 0:
                    print(line[:-1])

            # Now that the ranging has started, we can start to write data in csv file
            tlast = 0
            sumindex = -1
            avg_timeshift = 0
            N = 100

            while True:
                line = ser.readline().decode()
                if len(line) > 0:
                    if 'ERROR' not in line:

                        anchor_id , ts, distance, rss, pox, poy, poz = line.split(" ")
                        pox = float(pox) / 1000.0
                        poy = float(poy) / 1000.0
                        poz = float(poz) / 1000.0
                        rss = float(rss)

                        t = time() * 1000

                        # Every 30 seconds, we resynchronize the arduino
                        if t - tlast > 30 * 1000:  # happens to be true every 30 seconds
                            print("Start resync")
                            tlast = t
                            sumindex = 0

                        # The resynchronization needs N iteration to compute the average timeshift.
                        # If N = 100, then resync will last about 2 seconds
                        if 0 <= sumindex < N:  # happens to be true N iterations in a row
                            avg_timeshift += int(t0 + int(ts) - int(t))
                            sumindex += 1

                            if sumindex == N:  # happens to be true at the Nth iteration
                                # We can average the timshift now and substract it to the t0
                                avg_timeshift /= 1.0 * N
                                t0 -= avg_timeshift
                                print("Finish resync, time shift was " + str(avg_timeshift) + "ms")
                                # we reset the variables for the next resynchronization
                                avg_timeshift = 0
                                sumindex = -1

                        timestamp = int(t0 + int(ts))

                        # anchor_id is an hexadecimal number. We convert it in decimal
                        anchor_id = int(anchor_id[2:], 16)

                        # convert distance from millimeters to meters
                        distance = float(distance) * 0.001

                        # After many measures, we noticed that the error can be linearly approximed from the
                        # measured distance.
                        estimated_err = -0.008033*distance + 0.175
                        estimated_var = 0.000147*distance + 0.0002835


                        dico = {
                            "timestamp": timestamp,
                            "device_id": self.device_id,
                            "anchor_id": anchor_id,
                            "distance": distance,
                            "rssi": rss,
                            "px": pox,
                            "py": poy,
                            "pz": poz,
                            "system_id": self.baudrate
                        }

                        # format data to a list corresponding to headers defined in main.data_fields
                        towrite = [dico[f] if f in list(dico.keys()) else 'NaN' for f in data_fields]

                        if self.datawriter is not None:
                            self.datawriter.writeline(towrite)


    def onDestroy(self):
        print("Pozyx onDestroy()")