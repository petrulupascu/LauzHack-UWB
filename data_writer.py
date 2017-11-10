from __future__ import print_function
import sys
from threading import RLock
import csv


def eprint(*args, **kwargs):
    print(*args, file=sys.stderr, **kwargs)


class DataWriter():
    locker = RLock()

    def __init__(self, targetfile, header, verbose=False, verbose_interval=10):
        self.csvfile = open(targetfile, 'w')
        self.csvwriter = csv.writer(self.csvfile, delimiter=',', quotechar='"', quoting=csv.QUOTE_MINIMAL)
        self.verbose = verbose
        self.verbose_interval = verbose_interval
        self.count = 0
        self.header = header

        self.csvwriter.writerow(header)

    def writeline(self, dictionary):
        """
        write the data from dictionary to the log file
        :param dictionary: Entries of the dictionary must be contained in the self.header array
        :return: None 
        """
        # we lock this part because we don't want two scripts writing in the same time
        with self.locker:
            # convert the dictionary into an array of values accordingly to the header.
            # Missing values are replaced by 'NaN'
            line = [dictionary[f] if f in list(dictionary.keys()) else 'NaN' for f in self.header]
            if self.verbose and self.count % self.verbose_interval == 0:
                print("Data writer is about to write: ", line)
            self.csvwriter.writerow(line)
            self.csvfile.flush()
            self.count += 1

    def onDestroy(self):
        print("Data writer onDestroy()")
        self.csvfile.close()
