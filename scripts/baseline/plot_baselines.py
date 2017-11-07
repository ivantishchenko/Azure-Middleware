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

    def __init__(self, rep=3, path="", machines_num=3, threads_num=2, run_time=60, range_clients=[1,33,4]):
        self.REP_NUMBER = rep
        self.LOGFILES_PATH = path
        self.MACHINES_NUMBER = machines_num
        self.THREAD_PER_CLIENT = threads_num
        self.RUN_TIME = run_time
        self.CLIENTS_RANGE_BEG = range_clients[0]
        self.CLIENTS_RANGE_END = range_clients[1]
        self.CLIENTS_RANGE_STEP = range_clients[2]

    def set_params(self, rep, path, machines_num, threads_num, run_time, range_clients):
        self.REP_NUMBER = rep
        self.LOGFILES_PATH = path
        self.MACHINES_NUMBER = machines_num
        self.THREAD_PER_CLIENT = threads_num
        self.RUN_TIME = run_time
        self.CLIENTS_RANGE_BEG = range_clients[0]
        self.CLIENTS_RANGE_END = range_clients[1]
        self.CLIENTS_RANGE_STEP = range_clients[2]

    def extractParams(self, logfile):
        file = open(logfile, 'r')
        for line in file:
            if line.startswith("Totals"):
                avg_throughput = line.split()[1]
                avg_response_time = line.split()[4]

        file.close()
        return float(avg_throughput), float(avg_response_time)

    def plot_baseline_breakdown(self, filename1, filename2):
        NUM_TICKS = len(list(range(self.CLIENTS_RANGE_BEG, self.CLIENTS_RANGE_END + 1, self.CLIENTS_RANGE_STEP)))

        # Build
        T_total = []
        R_total = []
        T_STD = []
        R_STD = []

        for virtual_client in range(self.CLIENTS_RANGE_BEG, self.CLIENTS_RANGE_END + 1, self.CLIENTS_RANGE_STEP):
            final_throughput = [0] * self.MACHINES_NUMBER
            final_response_time = [0] * self.MACHINES_NUMBER

            aggregate_throughput_vals = [[0] * self.REP_NUMBER] * self.MACHINES_NUMBER
            avg_response_time_vals = [[0] * self.REP_NUMBER] * self.MACHINES_NUMBER

            for repetition in range(1, self.REP_NUMBER + 1):

                for machine in range(1, self.MACHINES_NUMBER + 1):
                    logfile_name = self.LOGFILES_PATH + "/baseline_{}_{}_{}.log".format(virtual_client, repetition, machine)
                    #print(logfile_name)
                    throughput, response = self.extractParams(logfile_name)
                    # Agregates
                    # SUM thriuputs
                    final_throughput[machine - 1] += throughput
                    # AVG response times
                    final_response_time[machine - 1] += response

                    aggregate_throughput_vals[machine-1][repetition-1] = throughput
                    avg_response_time_vals[machine-1][repetition-1] = response

                    #avg_response_time /= MACHINES_NUMBER
                    # at this point we have aggregates from all machines

            new_T = []
            new_R = []
            new_STD_T = []
            new_STD_R = []

            for i in range(self.MACHINES_NUMBER):
                final_throughput[i] /= self.REP_NUMBER
                final_response_time[i] /= self.REP_NUMBER


                T = final_throughput[i]
                new_T.append(T)

                R = final_response_time[i]
                new_R.append(R)

                STD_T = stdev(aggregate_throughput_vals[i])
                new_STD_T.append(STD_T)

                STD_R = stdev(avg_response_time_vals[i])
                new_STD_R.append(STD_R)

            T_total.append(new_T)
            R_total.append(new_R)
            T_STD.append(new_STD_T)
            R_STD.append(new_STD_R)

        # Build

        # for msec
        #R_total = [x / 1000 for x in R_total]
        #R_STD = [x / 1000 for x in R_STD]

        clients = [x * self.THREAD_PER_CLIENT * self.MACHINES_NUMBER for x in range(self.CLIENTS_RANGE_BEG, self.CLIENTS_RANGE_END + 1, self.CLIENTS_RANGE_STEP)]
        ticks = [x * self.THREAD_PER_CLIENT * self.MACHINES_NUMBER for x in range(self.CLIENTS_RANGE_BEG, self.CLIENTS_RANGE_END + 1, self.CLIENTS_RANGE_STEP)]

        #print(len(clients))
        #print(T_total)
        #print(T_STD)

        # throughput
        plt.figure(1)

        colors = ['b', 'm', 'y']
        markers = ['-x', '-^', '-o']

        for i in range(self.MACHINES_NUMBER):
            OUT_T = []
            OUT_STD = []
            for j in range(NUM_TICKS):
                OUT_T.append(T_total[j][i])
                OUT_STD.append(T_STD[j][i])

            plt.errorbar(clients, OUT_T, yerr=OUT_STD, fmt=markers[i % self.MACHINES_NUMBER], ecolor='r', color=colors[i % self.MACHINES_NUMBER], label="Client machine " + str(i+1))

        plt.legend()
        plt.xticks(ticks)
        plt.ylim(ymin=0)
        plt.xlim(xmin=0)
        plt.grid()
        plt.xlabel('Clients')
        plt.ylabel('Throughput')
        #plt.show()

        plt.savefig(filename1)
        plt.gcf().clear()

        # response time
        plt.figure(2)

        for i in range(self.MACHINES_NUMBER):
            OUT_R = []
            OUT_STD = []
            for j in range(NUM_TICKS):
                OUT_R.append(R_total[j][i])
                OUT_STD.append(R_STD[j][i])

            plt.errorbar(clients, OUT_R, yerr=OUT_STD, fmt=markers[i % self.MACHINES_NUMBER], ecolor='r', color=colors[i % self.MACHINES_NUMBER], label="Client machine " + str(i+1))

        plt.legend()
        plt.xticks(ticks)
        plt.ylim(ymin=0)
        plt.xlim(xmin=0)
        plt.grid()
        plt.xlabel('Clients')
        plt.ylabel('Response time')
        #plt.show()
        plt.savefig(filename2)
        plt.gcf().clear()

    def plot_baseline_aggregate(self, filename1, filename2):
        # Build
        T_total = []
        R_total = []
        T_STD = []
        R_STD = []

        for virtual_client in range(self.CLIENTS_RANGE_BEG, self.CLIENTS_RANGE_END + 1, self.CLIENTS_RANGE_STEP):
            final_throughput = 0
            final_response_time = 0
            aggregate_throughput_vals = []
            avg_response_time_vals = []

            for repetition in range(1, self.REP_NUMBER + 1):
                aggregate_throughput = 0
                avg_response_time = 0

                for machine in range(1, self.MACHINES_NUMBER + 1):
                    logfile_name = self.LOGFILES_PATH + "/baseline_{}_{}_{}.log".format(virtual_client, repetition, machine)
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

        clients = [x * self.THREAD_PER_CLIENT * self.MACHINES_NUMBER for x in range(self.CLIENTS_RANGE_BEG, self.CLIENTS_RANGE_END + 1, self.CLIENTS_RANGE_STEP)]
        ticks = [x * self.THREAD_PER_CLIENT * self.MACHINES_NUMBER for x in range(self.CLIENTS_RANGE_BEG, self.CLIENTS_RANGE_END + 1, self.CLIENTS_RANGE_STEP)]

        #print(clients)
        #print(T_total)
        #print(T_STD)

        # throughput
        plt.figure(1)
        plt.errorbar(clients, T_total, yerr=T_STD, fmt='-o', ecolor='r')
        plt.xticks(ticks)
        plt.ylim(ymin=0)
        plt.xlim(xmin=0)
        plt.grid()
        plt.xlabel('Clients')
        plt.ylabel('Throughput')
        #plt.show()
        plt.savefig(filename1)
        plt.gcf().clear()

        # response time
        plt.figure(2)
        plt.errorbar(clients, R_total, yerr=R_STD, fmt='-o', ecolor='r')
        plt.xticks(ticks)
        plt.ylim(ymin=0)
        plt.xlim(xmin=0)
        plt.grid()
        plt.xlabel('Clients')
        plt.ylabel('Response time')
        #plt.show()
        plt.savefig(filename2)
        plt.gcf().clear()


# PLOTTING

path_GET1 = "/home/ivan/asl-fall17-project/experiments/logfiles/baseline/logfiles_baseline_1_server_GET"
path_SET1 = "/home/ivan/asl-fall17-project/experiments/logfiles/baseline/logfiles_baseline_1_server_SET"
path_GET2 = "/home/ivan/asl-fall17-project/experiments/logfiles/baseline/logfiles_baseline_2_servers_GET"
path_SET2 = "/home/ivan/asl-fall17-project/experiments/logfiles/baseline/logfiles_baseline_2_servers_SET"

plotter = ExperimentPlotter()
plotter.set_params(3, path_SET1, 3, 2, 60, [1, 33, 4])
plotter.plot_baseline_aggregate("baseline_set1_agr_T.png","baseline_set1_agr_R.png")
plotter.plot_baseline_breakdown("baseline_set1_sep_T.png","baseline_set1_sep_R.png")

plotter.set_params(3, path_GET1, 3, 2, 60, [1, 33, 4])
plotter.plot_baseline_aggregate("baseline_get1_agr_T.png","baseline_get1_agr_R.png")
plotter.plot_baseline_breakdown("baseline_get1_sep_T.png","baseline_get1_sep_R.png")

plotter.set_params(3, path_SET2, 2, 1, 60, [1, 33, 4])
plotter.plot_baseline_aggregate("baseline_set2_agr_T.png","baseline_set2_agr_R.png")
plotter.plot_baseline_breakdown("baseline_set2_sep_T.png","baseline_set2_sep_R.png")

plotter.set_params(3, path_GET2, 2, 1, 60, [1, 33, 4])
plotter.plot_baseline_aggregate("baseline_get2_agr_T.png","baseline_get1_agr_R.png")
plotter.plot_baseline_breakdown("baseline_get2_sep_T.png","baseline_get2_sep_R.png")