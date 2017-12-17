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

    def plot_baseline_aggregate(self, filename1, filename2):

        T_workers = []
        R_workers = []
        T_STD_workers = []
        R_STD_workers = []

        if self.INSIDE_MW:
            MACHINES_RANGE = self.MW_NUMBER
            print("Plotting on the MW")
        else:
            MACHINES_RANGE = self.MACHINES_NUMBER
            print("Plotting on the Clients")

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
                        logfile_name = self.LOGFILES_PATH + "/" + str(worker) + "/baselineMW_{}_{}_{}.log".format(virtual_client, repetition, machine)
                        #print(logfile_name)

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


        colors = ['b', 'm', 'y', 'g']
        markers = ['-x', '-^', '-o', '-d']
        legends = []
        legends_name = []
        for i in range(len(self.WORKERS_RANGE)):
            T = T_workers[i]
            T_STD = T_STD_workers[i]

            clients = [x * self.THREAD_PER_CLIENT * self.MACHINES_NUMBER for x in range(self.CLIENTS_RANGE_BEG, self.CLIENTS_RANGE_END + 1, self.CLIENTS_RANGE_STEP)]
            ticks = [x * self.THREAD_PER_CLIENT * self.MACHINES_NUMBER for x in range(self.CLIENTS_RANGE_BEG, self.CLIENTS_RANGE_END + 1, self.CLIENTS_RANGE_STEP)]

            # throughput
            print(T)
            plt.figure(1)
            print("WORKERS # {} MAX QueueLen {}".format(self.WORKERS_RANGE[i], max(T)))
            print("")

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


            # response time
            plt.figure(2)
            print(R)
            print("WORKERS # {} MAX Service time {}".format(self.WORKERS_RANGE[i], max(R)))
            print("")

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


# FINAL commands

# path = "/home/ivan/asl-fall17-project/experiments/logfiles/baselineMiddleware/logfiles_baselineMW_single_GET_MW"
# plotter = ExperimentPlotter()
# plotter.INSIDE_MW = True
# plotter.set_params(3, path, 1, 2, 1, [1, 33, 4])
# plotter.plot_baseline_aggregate("queueLen_single_baselineMW_get_agr.png","serveTime_single_baselineMW_get_agr.png")


# path = "/home/ivan/asl-fall17-project/experiments/logfiles/baselineMiddleware/logfiles_baselineMW_double_GET_MW"
# plotter = ExperimentPlotter()
# plotter.INSIDE_MW = True
# plotter.set_params(3, path, 2, 1, 2, [1, 33, 4])
# plotter.plot_baseline_aggregate("queueLen_double_baselineMW_get_agr.png","serveTime_double_baselineMW_get_agr.png")

# DOUBLE ADD

# path = "/home/ivan/asl-fall17-project/experiments/logfiles/baselineMiddleware/logfiles_baselineMW_double_GET_client_ADD"
# plotter = ExperimentPlotter()
# plotter.INSIDE_MW = False
# plotter.set_params(3, path, 4, 1, 2, [1, 33, 4])
# plotter.WORKERS_RANGE=[64]
# plotter.plot_baseline_aggregate("ADD_double_baselineMWClient_get_agr_T.png","ADD_double_baselineMWClient_get_agr_R.png")
#
# path = "/home/ivan/asl-fall17-project/experiments/logfiles/baselineMiddleware/logfiles_baselineMW_double_GET_MW_ADD"
# plotter = ExperimentPlotter()
# plotter.INSIDE_MW = True
# plotter.set_params(3, path, 4, 1, 2, [1, 33, 4])
# plotter.WORKERS_RANGE=[64]
# plotter.plot_baseline_aggregate("ADD_double_baselineMW_get_agr_T.png","ADD_double_baselineMW_get_agr_R.png")

# REDO GET DOUBLE
#
# path = "/home/ivan/asl-fall17-project/experiments/logfiles/baselineMiddleware/redoGET/logfiles_baselineMW_double_GET_client"
# plotter = ExperimentPlotter()
# plotter.INSIDE_MW = False
# plotter.WORKERS_RANGE = [64]
# plotter.set_params(3, path, 2, 1, 2, [1, 33, 4])
# plotter.plot_baseline_aggregate("redo_double_baselineMWClient_get_agr_T.png","redo_double_baselineMWClient_get_agr_R.png")
#
# path = "/home/ivan/asl-fall17-project/experiments/logfiles/baselineMiddleware/redoGET/logfiles_baselineMW_double_GET_MW"
# plotter = ExperimentPlotter()
# plotter.INSIDE_MW = True
# plotter.WORKERS_RANGE = [64]
# plotter.set_params(3, path, 2, 1, 2, [1, 33, 4])
# plotter.plot_baseline_aggregate("redo_double_baselineMW_get_agr_T.png","redo_double_baselineMW_get_agr_R.png")

# FINAL

path = "/home/ivan/asl-fall17-project/experiments/logfiles/baselineMiddleware/redoFinal/logfiles_baselineMW_single_GET_MW"
plotter = ExperimentPlotter()
plotter.INSIDE_MW = True
plotter.set_params(3, path, 1, 2, 1, [1, 33, 4])
plotter.plot_baseline_aggregate("final_queueLen_single_baselineMW_get_agr.png","final_serveTime_single_baselineMW_get_agr.png")


path = "/home/ivan/asl-fall17-project/experiments/logfiles/baselineMiddleware/redoFinal/logfiles_baselineMW_double_GET_MW"
plotter = ExperimentPlotter()
plotter.INSIDE_MW = True
plotter.set_params(3, path, 2, 1, 2, [1, 33, 4])
plotter.plot_baseline_aggregate("final_queueLen_double_baselineMW_get_agr.png","final_serveTime_double_baselineMW_get_agr.png")
