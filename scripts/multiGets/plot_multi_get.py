import re
import sys
from operator import add
from matplotlib import pyplot as plt
from statistics import stdev

class ExperimentPlotter:

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

    def extractParams(self, logfile):
        file = open(logfile, 'r')
        for line in file:
            if line.startswith("Totals"):
                avg_throughput = line.split()[1]
                avg_response_time = line.split()[4]

        file.close()
        return float(avg_throughput), float(avg_response_time)


    def plot_baseline_aggregate(self, filename1, filename2):
        # Build
        T_total = []
        R_total = []
        T_STD = []
        R_STD = []

        for multi_get_key in self.KEY_RANGE:
            final_throughput = 0
            final_response_time = 0
            aggregate_throughput_vals = []
            avg_response_time_vals = []

            for repetition in range(1, self.REP_NUMBER + 1):
                aggregate_throughput = 0
                avg_response_time = 0

                for machine in range(1, self.MACHINES_NUMBER + 1):
                    logfile_name = self.LOGFILES_PATH + "/multi_get_{}_{}_{}.log".format(multi_get_key, repetition, machine)
                    #print(logfile_name)
                    throughput, response = self.extractParams(logfile_name)
                    # Agregates
                    # SUM thriuputs
                    aggregate_throughput += throughput
                    # AVG response times
                    avg_response_time += response

                avg_response_time /= self.MACHINES_NUMBER
                # at this point we have aggregates from all machines
                final_throughput += aggregate_throughput
                final_response_time += avg_response_time

                aggregate_throughput_vals.append(aggregate_throughput)
                avg_response_time_vals.append(avg_response_time)

            final_throughput /= self.REP_NUMBER
            final_response_time /= self.REP_NUMBER

            T_total.append(final_throughput)
            R_total.append(final_response_time)
            T_STD.append(stdev(aggregate_throughput_vals))
            R_STD.append(stdev(avg_response_time_vals))

        # Build

        # for msec
        #R_total = [x / 1000 for x in R_total]
        #R_STD = [x / 1000 for x in R_STD]

        clients = self.KEY_RANGE
        ticks = list(range(10))

        #print(clients)
        #print(T_total)
        #print(T_STD)

        throughput
        plt.figure(1)
        plt.title("Multigets")
        plt.errorbar(clients, T_total, yerr=T_STD, fmt='-o', ecolor='r', color='blue', capsize=2, elinewidth=0.5, alpha=0.8)
        plt.xticks(ticks)
        plt.ylim(ymin=0)
        plt.xlim(xmin=0)
        plt.grid()
        plt.xlabel('Multi get size (keys)')
        plt.ylabel('Throughput (ops/sec)')
        #plt.show()
        plt.savefig(filename1)
        plt.gcf().clear()

        # response time
        plt.figure(2)
        plt.title("Multigets")
        plt.errorbar(clients, R_total, yerr=R_STD, fmt='-o', ecolor='r', color='blue', capsize=2, elinewidth=0.5, alpha=0.8)
        plt.xticks(ticks)
        plt.ylim(ymin=0)
        plt.xlim(xmin=0)
        plt.grid()
        plt.xlabel('Multi get size (keys)')
        plt.ylabel('Response time (msec)')
        #plt.show()
        plt.savefig(filename2)
        plt.gcf().clear()


# PLOTTING

path = "/home/ivan/asl-fall17-project/experiments/logfiles/multiGets/logfiles_multiGET_shard_Client"

plotter = ExperimentPlotter()
plotter.set_params(3, path, 6, 1)
plotter.plot_baseline_aggregate("shard_multi_get_T.png","shard_multi_get_R.png")

path = "/home/ivan/asl-fall17-project/experiments/logfiles/multiGets/logfiles_multiGET_nonshard_Client"

plotter = ExperimentPlotter()
plotter.set_params(3, path, 6, 1)
plotter.plot_baseline_aggregate("nonshard_multi_get_T.png","nonshard_multi_get_R.png")
