import re
import sys
from operator import add
from matplotlib import pyplot as plt
from statistics import stdev
import operator

class ExperimentPlotter:

    REP_NUMBER = 3
    LOGFILES_PATH = ""
    MACHINES_NUMBER = 3
    THREAD_PER_CLIENT = 2

    THINK_TIME = 0.00128
    THINK_TIME = 0.0
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

    def __init__(self, rep=3, path="", machines_num=3, threads_num=2, mw_num=2, range_clients=[1,33,4]):
        self.REP_NUMBER = rep
        self.LOGFILES_PATH = path
        self.MACHINES_NUMBER = machines_num
        self.THREAD_PER_CLIENT = threads_num
        self.CLIENTS_RANGE_BEG = range_clients[0]
        self.CLIENTS_RANGE_END = range_clients[1]
        self.CLIENTS_RANGE_STEP = range_clients[2]
        self.MW_NUMBER = mw_num

    def set_params(self, rep, path, machines_num, threads_num, mw_num, range_clients):
        self.REP_NUMBER = rep
        self.LOGFILES_PATH = path
        self.MACHINES_NUMBER = machines_num
        self.THREAD_PER_CLIENT = threads_num
        self.CLIENTS_RANGE_BEG = range_clients[0]
        self.CLIENTS_RANGE_END = range_clients[1]
        self.CLIENTS_RANGE_STEP = range_clients[2]
        self.MW_NUMBER = mw_num

    def extractParamsMW(self, logfile):
        file = open(logfile, 'r')
        for line in file:
            if line.startswith("FINAL STATS"):
                for line in file:
                    if line.startswith("Average queue length"):
                        queue_len = line.split()[4]
                    if line.startswith("Average service time"):
                        serve_time = line.split()[5]
                        #print(avg_response_time)

        file.close()

        #print(logfile)
        return float(queue_len), float(serve_time)

    def extractParams(self, logfile):
        file = open(logfile, 'r')
        for line in file:
            if line.startswith("Totals"):
                avg_throughput = line.split()[1]
                avg_response_time = line.split()[4]

        file.close()

        #print(logfile)
        return float(avg_throughput), float(avg_response_time)

    def getConfigThroughput(self, WT_num, VC_num, MACHINE_num):
        config_throughput = 0
        for machine in range(1, MACHINE_num + 1):
            rep_thoughput = 0
            for repetition in range(1, self.REP_NUMBER + 1):
                logfile_name = self.LOGFILES_PATH + "/" + str(WT_num) + "/throughputWrites_{}_{}_{}.log".format(VC_num, repetition, machine)
                # print(logfile_name)
                if self.INSIDE_MW:
                    T, _ = self.extractParamsMW(logfile_name)
                    rep_thoughput += T
                else:
                    T, _ = self.extractParams(logfile_name)
                    rep_thoughput += T

            rep_thoughput /= self.REP_NUMBER
            config_throughput += rep_thoughput
        return config_throughput


    def getConfigThroughputInter(self, WT_num, VC_num, MACHINE_num):
        config_R = 0
        for machine in range(1, MACHINE_num + 1):
            rep_R = 0
            for repetition in range(1, self.REP_NUMBER + 1):
                logfile_name = self.LOGFILES_PATH + "/" + str(WT_num) + "/throughputWrites_{}_{}_{}.log".format(VC_num, repetition, machine)
                # print(logfile_name)
                if self.INSIDE_MW:
                    _, R = self.extractParamsMW(logfile_name)
                    rep_R += R
                else:
                    _, R = self.extractParams(logfile_name)
                    rep_R += R
            rep_R /= self.REP_NUMBER
            config_R += rep_R

        config_R /= MACHINE_num
        config_R /= 1000

        NumClients = VC_num * self.THREAD_PER_CLIENT * self.MACHINES_NUMBER
        X = NumClients / (config_R + self.THINK_TIME)
        return X

    def getConfigTimeQueue(self, WT_num, VC_num, MACHINE_num):
        time_config = 0
        for machine in range(1, MACHINE_num + 1):
            time_rep = 0
            for repetition in range(1, self.REP_NUMBER + 1):
                logfile_name = self.LOGFILES_PATH + "/" + str(WT_num) + "/throughputWrites_{}_{}_{}.log".format(VC_num, repetition, machine)
                # print(logfile_name)
                file = open(logfile_name, 'r')
                for line in file:
                    if line.startswith("FINAL STATS"):
                        for line in file:
                            if line.startswith("Average wait time in queue"):
                                queue_time = float(line.split()[7])
                file.close()
                time_rep += queue_time

            time_rep /= self.REP_NUMBER
            time_config += time_rep

        time_config /= MACHINE_num
        return time_config


    def getConfigQueueLen(self, WT_num, VC_num, MACHINE_num):
        len_config = 0
        for machine in range(1, MACHINE_num + 1):
            len_rep = 0
            for repetition in range(1, self.REP_NUMBER + 1):
                logfile_name = self.LOGFILES_PATH + "/" + str(WT_num) + "/throughputWrites_{}_{}_{}.log".format(VC_num, repetition, machine)
                # print(logfile_name)
                file = open(logfile_name, 'r')
                for line in file:
                    if line.startswith("FINAL STATS"):
                        for line in file:
                            if line.startswith("Average queue length"):
                                len_queue = float(line.split()[4])
                file.close()
                len_rep += len_queue

            len_rep /= self.REP_NUMBER
            len_config += len_rep

        len_config /= MACHINE_num
        return len_config

    def getConfigServiceTime(self, WT_num, VC_num, MACHINE_num):
        time_config = 0
        for machine in range(1, MACHINE_num + 1):
            time_rep = 0
            for repetition in range(1, self.REP_NUMBER + 1):
                logfile_name = self.LOGFILES_PATH + "/" + str(WT_num) + "/throughputWrites_{}_{}_{}.log".format(VC_num, repetition, machine)
                # print(logfile_name)
                file = open(logfile_name, 'r')
                for line in file:
                    if line.startswith("FINAL STATS"):
                        for line in file:
                            if line.startswith("Average service time"):
                                serve_time = float(line.split()[5])
                file.close()
                time_rep += serve_time

            time_rep /= self.REP_NUMBER
            time_config += time_rep

        time_config /= MACHINE_num
        return time_config


    def plot_baseline_aggregate(self, filename1, filename2):

        T_workers = []
        R_workers = []
        T_STD_workers = []
        R_STD_workers = []

        if self.INSIDE_MW:
            MACHINES_RANGE = self.MW_NUMBER
            print("On MW")
        else:
            MACHINES_RANGE = self.MACHINES_NUMBER
            print("On clients")

        for worker in self.WORKERS_RANGE:
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

                    for machine in range(1, MACHINES_RANGE + 1):
                        logfile_name = self.LOGFILES_PATH + "/" + str(worker) + "/throughputWrites_{}_{}_{}.log".format(virtual_client, repetition, machine)
                        # print(logfile_name)

                        if self.INSIDE_MW:
                            throughput, response = self.extractParamsMW(logfile_name)
                        else:
                            throughput, response = self.extractParams(logfile_name)

                        # Agregates
                        # SUM thriuputs
                        aggregate_throughput += throughput
                        # AVG response times
                        avg_response_time += response

                    avg_response_time /= MACHINES_RANGE
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

            # PLOTTING

            T_workers.append(T_total)
            R_workers.append(R_total)
            T_STD_workers.append(T_STD)
            R_STD_workers.append(R_STD)



        peaks = list(range(self.CLIENTS_RANGE_BEG, self.CLIENTS_RANGE_END + 1, self.CLIENTS_RANGE_STEP))
        colors = ['b', 'm', 'y', 'g']
        markers = ['-x', '-^', '-o', '-d']
        legends = []
        legends_name = []
        for i in range(len(self.WORKERS_RANGE)):
            T = T_workers[i]
            T_STD = T_STD_workers[i]

            # Measurments for the table
            print("all T = {} ".format(T))
            index, value = max(enumerate(T), key=operator.itemgetter(1))
            maxThroughputVC = peaks[index]
            print("WORKERS # {} MAX Throughput {} at VC = {}".format(self.WORKERS_RANGE[i], value, maxThroughputVC))



            if self.INSIDE_MW:
                config_T = self.getConfigThroughput(self.WORKERS_RANGE[i], maxThroughputVC, MACHINES_RANGE)
                print("AVG T = {}".format(config_T))

                config_T_inter = self.getConfigThroughputInter(self.WORKERS_RANGE[i], maxThroughputVC, MACHINES_RANGE)
                print("AVG INTER T = {}".format(config_T_inter))

                queue_time = self.getConfigTimeQueue(self.WORKERS_RANGE[i], maxThroughputVC, MACHINES_RANGE)
                print("AVG QUEUE WAIT TIME T = {}".format(queue_time))

                queue_len = self.getConfigQueueLen(self.WORKERS_RANGE[i], maxThroughputVC, MACHINES_RANGE)
                print("AVG QUEUE LEN = {}".format(queue_len))

                server_time = self.getConfigServiceTime(self.WORKERS_RANGE[i], maxThroughputVC, MACHINES_RANGE)
                print("AVG SERVE TIME = {}".format(server_time))
            else:
                config_T = self.getConfigThroughput(self.WORKERS_RANGE[i], maxThroughputVC, MACHINES_RANGE)
                print("AVG T = {}".format(config_T))

            print("")
            # Table end


            clients = [x * self.THREAD_PER_CLIENT * self.MACHINES_NUMBER for x in range(self.CLIENTS_RANGE_BEG, self.CLIENTS_RANGE_END + 1, self.CLIENTS_RANGE_STEP)]
            ticks = [x * self.THREAD_PER_CLIENT * self.MACHINES_NUMBER for x in range(self.CLIENTS_RANGE_BEG, self.CLIENTS_RANGE_END + 1, self.CLIENTS_RANGE_STEP)]

            # throughput
            plt.figure(1)
            #plt.title("Throughput graph")
            p = plt.errorbar(clients, T, yerr=T_STD, fmt=markers[i], ecolor='r', color=colors[i])
            legends.append(p)
            legends_name.append("Worker threads # " + str(self.WORKERS_RANGE[i]))


        plt.xticks(ticks)
        plt.ylim(ymin=0)
        plt.ylim(ymin=0)
        plt.xlim(xmin=0)
        plt.grid()
        plt.xlabel('NumClients')
        plt.ylabel('Number of jobs')
        #plt.show()
        plt.legend(legends, legends_name)
        plt.savefig(filename1)
        plt.gcf().clear()


        legends = []
        legends_name = []
        for i in range(len(self.WORKERS_RANGE)):
            R = R_workers[i]
            R_STD = R_STD_workers[i]


            # Measurments for the table
            print("all R = {} ".format(R))
            index, value = max(enumerate(R), key=operator.itemgetter(1))
            maxResp = peaks[index]
            print("WORKERS # {} MAX Response time {} at VC = {}".format(self.WORKERS_RANGE[i], value, maxResp))

            clients = [x * self.THREAD_PER_CLIENT * self.MACHINES_NUMBER for x in range(self.CLIENTS_RANGE_BEG, self.CLIENTS_RANGE_END + 1, self.CLIENTS_RANGE_STEP)]
            ticks = [x * self.THREAD_PER_CLIENT * self.MACHINES_NUMBER for x in range(self.CLIENTS_RANGE_BEG, self.CLIENTS_RANGE_END + 1, self.CLIENTS_RANGE_STEP)]

            # response time
            plt.figure(2)

            p = plt.errorbar(clients, R, yerr=R_STD, fmt=markers[i], ecolor='r', color=colors[i])
            legends.append(p)
            legends_name.append("Worker threads # " + str(self.WORKERS_RANGE[i]))


        plt.xticks(ticks)
        plt.ylim(ymin=0)
        plt.xlim(xmin=0)
        plt.grid()
        plt.xlabel('NumClients')
        plt.ylabel('Service time (msec)')
        #plt.show()
        plt.legend(legends, legends_name)
        plt.savefig(filename2)
        plt.gcf().clear()



path = "/home/ivan/asl-fall17-project/experiments/logfiles/throughputWrites/prev/logfiles_throughputWrites_MW"
plotter = ExperimentPlotter()
plotter.INSIDE_MW = True
plotter.set_params(3, path, 6, 1, 2, [1, 33, 4])
plotter.plot_baseline_aggregate("writes_queue_len.png", "writes_serve_time_.png")

# path = "/home/ivan/asl-fall17-project/experiments/logfiles/throughputWrites/logfiles_throughputWrites_Client"
# plotter = ExperimentPlotter()
# plotter.INSIDE_MW = False
# plotter.set_params(3, path, 6, 1, 2, [1, 33, 4])
# plotter.plot_baseline_aggregate("throughput_writesClient_T.png", "throughput_writesClient_R.png")