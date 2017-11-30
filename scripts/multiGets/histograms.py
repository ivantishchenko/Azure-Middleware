import matplotlib.pyplot as plt
import numpy as np

class Histogram:

    KEYS_NUM = 6
    MEMTIER_NUM = 6
    MW_NUMBER = 2
    LOGFILES_PATH = "/home/ivan/asl-fall17-project/experiments/logfiles/multiGets/logfiles_multiGET_nonshard_MW"
    REP_NUMBER = 3
    INSIDE_MW = True

    HISTOGRAM_LEN = 40

    def extractParams(self, logfile):

        response_time_list = []
        response_number_list = []

        file = open(logfile, 'r')
        for line in file:
            if line.startswith("Histogram response times"):
                next(file)
                for line in file:
                    response_time = line.split()[0]
                    response_number = line.split()[3]

                    response_time_list.append(response_time)
                    response_number_list.append(response_number)

        file.close()

        return response_time_list, response_number_list

    def plot_histogram(self):
        # Build

        OUT_TIMES = []
        OUT_NUMBERS = []

        if self.INSIDE_MW:
            self.MACHINES_NUMBER = self.MW_NUMBER
        else:
            self.MACHINES_NUMBER = self.MEMTIER_NUM

        for i in range(self.HISTOGRAM_LEN):

            final_response_number = 0
            avg_response_number_vals = []
            for repetition in range(1, self.REP_NUMBER + 1):
                avg_response_number = 0
                for machine in range(1, self.MACHINES_NUMBER + 1):

                    logfile_name = self.LOGFILES_PATH + "/multi_get_{}_{}_{}.log".format(self.KEYS_NUM, repetition, machine)

                    response_time, response_number = self.extractParams(logfile_name)
                    #print("Configuration: " + "keys = {} repetition = {} machine = {}".format(self.KEYS_NUM, repetition, machine))

                    #print(response_time[i])
                    #print(response_number[i])

                    avg_response_number += int(response_number[i])

                avg_response_number /= self.MACHINES_NUMBER
                final_response_number += avg_response_number

            final_response_number /= self.REP_NUMBER
            OUT_NUMBERS.append(final_response_number)
            OUT_TIMES.append(response_time[i])

        OUT_HISTO = []
        for i in range(self.HISTOGRAM_LEN):
            element = (OUT_TIMES[i], OUT_NUMBERS[i])
            print(element)
            OUT_HISTO.append(element)

        plt.bar(OUT_TIMES, height=OUT_NUMBERS)
        plt.show()

h = Histogram()
h.plot_histogram()

