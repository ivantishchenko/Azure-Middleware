import re
import sys
from operator import add
from matplotlib import pyplot as plt
from statistics import stdev

class Data2k:

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

    MW_NUMBER = 2
    WORKERS_RANGE = [8, 16, 32, 64]

    INSIDE_MW = True

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

    def extractParamsMW(self, logfile):
        file = open(logfile, 'r')
        for line in file:
            if line.startswith("FINAL STATS"):
                for line in file:
                    if line.startswith("Average throughput"):
                        avg_throughput = line.split()[4]
                    if line.startswith("Average response time"):
                        avg_response_time = line.split()[5]

        file.close()

        #print(logfile)
        return float(avg_throughput), float(avg_response_time)

    def extractParams(self, logfile):
        file = open(logfile, 'r')
        for line in file:
            if line.startswith("Totals"):
                avg_throughput = line.split()[1]
                avg_response_time = line.split()[4]

        file.close()

        #print(logfile)
        return float(avg_throughput), float(avg_response_time)

    def get_data(self, params):

        if self.MACHINES_NUMBER != 1:
            MACHINES_RANGE = self.MACHINES_NUMBER * self.THREAD_PER_CLIENT
        else:
            MACHINES_RANGE = self.MACHINES_NUMBER

        NUM_MC = params[0]
        NUM_MW = params[1]
        NUM_WT = params[2]

        for repetition in range(1, self.REP_NUMBER + 1):
            aggregate_throughput = 0
            avg_response_time = 0

            for machine in range(1, MACHINES_RANGE + 1):
                logfile_name = self.LOGFILES_PATH + "/2k_{}_{}_{}_{}_{}.log".format(NUM_MC, NUM_MW, NUM_WT, repetition, machine)

                throughput, response = self.extractParams(logfile_name)

                aggregate_throughput += throughput
                avg_response_time += response

            avg_response_time /= MACHINES_RANGE

            print("T = {} R = {} Config MC = {} MW = {} WT = {} REP = {}.log".format(aggregate_throughput, avg_response_time, NUM_MC, NUM_MW, NUM_WT, repetition))
        print("")



path = "/home/ivan/asl-fall17-project/experiments/logfiles/2k/logfiles_2k_MIX_Client"
plotter = Data2k()
plotter.set_params(3, path, 6, 1)
plotter.get_data([2,1,8])
plotter.get_data([2,1,32])
plotter.get_data([2,2,8])
plotter.get_data([2,2,32])
plotter.get_data([3,1,8])
plotter.get_data([3,1,32])
plotter.get_data([3,2,8])
plotter.get_data([3,2,32])