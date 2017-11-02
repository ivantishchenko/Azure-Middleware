import re
import sys
from operator import add
from matplotlib import pyplot as plt

REP_NUMBER = 3
LOGFILES_PATH = sys.argv[1]
MACHINES_NUMBER = 3
RUN_TIME = 60
THREAD_PER_CLIENT = 2

# See the same in bash script
# clients=`seq 1 4 33`
CLIENTS_RANGE_BEG = 1
CLIENTS_RANGE_END =  33
CLIENTS_RANGE_STEP = 4

# gets colums with thriuputs and response times
def extractParams(logfile):
    file = open(logfile, 'r')

    throughput_col = []
    response_time_col = []

    for line in file:
        # detect matches
        match_throughput = re.search("\d+ \(avg:.* ops/sec", line)
        match_response_time = re.search("\d+\.\d+ \(avg:.* msec", line)

        # cut the value and add to list
        if match_throughput is not None:
            throughput_val = match_throughput.group().split(' ')[0]
            throughput_col.append(float(throughput_val))

        if match_response_time is not None:
            response_time_val = match_response_time.group().split(' ')[0]
            response_time_col.append(float(response_time_val))

    file.close()

    return throughput_col, response_time_col


# Build
T_total = []
R_total = []
for virtual_client in range(CLIENTS_RANGE_BEG, CLIENTS_RANGE_END + 1, CLIENTS_RANGE_STEP):
    final_throughput = [0 for x in range(RUN_TIME)]
    final_response_time = [0 for x in range(RUN_TIME)]

    for repetition in range(1, REP_NUMBER + 1):
        aggregate_throughput = [0 for x in range(RUN_TIME)]
        avg_response_time = [0 for x in range(RUN_TIME)]

        for machine in range(1, MACHINES_NUMBER + 1):
            logfile_name = LOGFILES_PATH + "/baseline_{}_{}_{}.log".format(virtual_client, repetition, machine)
            #print(logfile_name)
            T_col, R_col = extractParams(logfile_name)
            # Agregates
            # SUM thriuputs
            aggregate_throughput = list(map(add, T_col, aggregate_throughput))
            # AVG response times
            avg_response_time = list(map(add, R_col, avg_response_time))

        avg_response_time = [x / MACHINES_NUMBER for x in avg_response_time]
        # at this point we have aggregates from all machines
        final_throughput = list(map(add, aggregate_throughput, final_throughput))
        final_response_time = list(map(add, avg_response_time, final_response_time))

    final_throughput = [x / REP_NUMBER for x in final_throughput]
    final_response_time = [x / REP_NUMBER for x in final_response_time]
    T_total.append(final_throughput)
    R_total.append(final_response_time)


# Build

x_axis = [ x for x in range(CLIENTS_RANGE_BEG, CLIENTS_RANGE_END + 1, CLIENTS_RANGE_STEP)]

plt.figure()
plt.boxplot(T_total, 0, '', positions=x_axis)


plt.show()

#print(final_throughput)
#print(final_response_time)

#TODO: try to do box plot for all clients
#T_col, R_col = extractParams(logfiles_path + "/baseline_28_1_1.log")
#print(T_col)
#print(R_col)
