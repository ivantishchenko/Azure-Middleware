import re
import sys
from operator import add
from matplotlib import pyplot as plt
from statistics import stdev
import numpy as np

class PercentilePlot:

    REP_NUMBER = 3
    LOGFILES_PATH = ""
    MACHINES_NUMBER = 3
    THREAD_PER_CLIENT = 2

    # CHANGE
    RUN_TIME = 60
    # See the same in bash script
    # clients=`seq 1 4 33`
    CLIENTS_RANGE_BEG = 1
    CLIENTS_RANGE_END = 33
    CLIENTS_RANGE_STEP = 4

    PERCENT_RANGE = [25, 50, 75, 90, 99]
    KEY_RANGE = [1, 3, 6, 9]

    def __init__(self, rep=3, path="", machines_num=3, threads_num=2):
        self.REP_NUMBER = rep
        self.LOGFILES_PATH = path
        self.MACHINES_NUMBER = machines_num
        self.THREAD_PER_CLIENT = threads_num

    def set_params(self, rep, path, machines_num, threads_num):
        self.REP_NUMBER = rep
        self.LOGFILES_PATH = path
        self.MACHINES_NUMBER = machines_num
        self.THREAD_PER_CLIENT = threads_num


    def extractPercentile(self, logfile, percentile):
        file = open(logfile, 'r')

        # EXTRACT <Percentile, Time>
        GET_cdf = []
        SET_cdf = []

        for line in file:
            if line.startswith("Type     <= msec         Percent"):
                for line in file:
                    if line.startswith("SET"):
                        entry = (line.split()[2], line.split()[1])
                        SET_cdf.append(entry)
                    if line.startswith("GET"):
                        entry = (line.split()[2], line.split()[1])
                        GET_cdf.append(entry)
        file.close()

        x = [float(each_element[0]) for each_element in SET_cdf]
        y = [float(each_element[1]) for each_element in SET_cdf]
        SET_percentile = np.interp(percentile, x, y)

        x = [float(each_element[0]) for each_element in GET_cdf]
        y = [float(each_element[1]) for each_element in GET_cdf]
        GET_percentile = np.interp(percentile, x, y)

        print(logfile)
        print(SET_percentile, GET_percentile)
        return SET_percentile, GET_percentile

    def extractParams(self, logfile):
        file = open(logfile, 'r')
        for line in file:
            if line.startswith("Totals"):
                avg_throughput = line.split()[1]
                avg_response_time = line.split()[4]

        file.close()
        return float(avg_throughput), float(avg_response_time)



    def plot_percentile(self,filename2):
        R_all = []
        R_STD_all = []

        for percentile in self.PERCENT_RANGE:
            # Build
            R_total = []

            R_STD = []

            for multi_get_key in self.KEY_RANGE:
                final_response_time = 0
                avg_response_time_vals = []

                for repetition in range(1, self.REP_NUMBER + 1):
                    avg_response_time = 0

                    for machine in range(1, self.MACHINES_NUMBER + 1):
                        logfile_name = self.LOGFILES_PATH + "/multi_get_{}_{}_{}.log".format(multi_get_key, repetition, machine)
                        #print(logfile_name)
                        SET_percentile, GET_percentile = self.extractPercentile(logfile_name, percentile)
                        response = (SET_percentile + GET_percentile) / 2

                        avg_response_time += response

                    avg_response_time /= self.MACHINES_NUMBER
                    # at this point we have aggregates from all machines
                    final_response_time += avg_response_time

                    avg_response_time_vals.append(avg_response_time)

                final_response_time /= self.REP_NUMBER
                R_total.append(final_response_time)
                R_STD.append(stdev(avg_response_time_vals))

            R_all.append(R_total)
            R_STD_all.append(R_STD)

        # PLOT


        colors = ['b', 'm', 'y', 'g', 'c']
        markers = ['-x', '-^', '-o', '-d', '-*']
        legends = []
        legends_name = []

        clients = self.KEY_RANGE
        ticks = list(range(10))

        for i in range(len(self.PERCENT_RANGE)):
            R = R_all[i]
            STD = R_STD_all[i]
            plt.figure(2)
            p=plt.errorbar(clients, R, yerr=STD, fmt=markers[i], ecolor='r', color=colors[i], capsize=2, elinewidth=0.5, alpha=0.8)
            legends.append(p)
            legends_name.append("Percentile = " + str(self.PERCENT_RANGE[i]))


        plt.title("Multigets percentiles")

        plt.xticks(ticks)
        plt.ylim(ymin=0)
        plt.xlim(xmin=0)
        plt.grid()
        plt.xlabel('Multi get size (keys)')
        plt.ylabel('Response time (msec)')
        #plt.show()
        plt.legend(legends, legends_name, loc='upper right')
        plt.savefig(filename2)
        plt.gcf().clear()

# PLOTTING

path = "/home/ivan/asl-fall17-project/experiments/logfiles/multiGets/logfiles_multiGET_shard_Client"

plotter = PercentilePlot()
plotter.set_params(3, path, 6, 1)
plotter.plot_percentile("shard_multi_get_R_percentile.png")


path = "/home/ivan/asl-fall17-project/experiments/logfiles/multiGets/logfiles_multiGET_nonshard_Client"

plotter = PercentilePlot()
plotter.set_params(3, path, 6, 1)
plotter.plot_percentile("nonshard_multi_get_R_percentile.png")
