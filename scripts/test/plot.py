import re
import sys
from operator import add
from matplotlib import pyplot as plt
from statistics import stdev

REP_NUMBER = 3
LOGFILES_PATH = sys.argv[1]
MACHINES_NUMBER = 3
THREAD_PER_CLIENT = 2

# CHANGE
RUN_TIME = 60
# See the same in bash script
# clients=`seq 1 4 33`
CLIENTS_RANGE_BEG = 1
CLIENTS_RANGE_END = 33
CLIENTS_RANGE_STEP = 4

# gets colums with thriuputs and response times
'''
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

    T = sum(throughput_col) / len(throughput_col)
    R = sum(response_time_col) / len(response_time_col)

    return float(T), float(R)
'''

def extractParams(logfile):
    file = open(logfile, 'r')

    for line in file:
        if line.startswith("Totals"):
            avg_throughput = line.split()[1]
            avg_response_time = line.split()[4]

    file.close()

    return float(avg_throughput), float(avg_response_time)

# Build
T_total = []
R_total = []
T_STD = []
R_STD = []

for virtual_client in range(CLIENTS_RANGE_BEG, CLIENTS_RANGE_END + 1, CLIENTS_RANGE_STEP):
    final_throughput = 0
    final_response_time = 0
    aggregate_throughput_vals = []
    avg_response_time_vals = []

    for repetition in range(1, REP_NUMBER + 1):
        aggregate_throughput = 0
        avg_response_time = 0

        for machine in range(1, MACHINES_NUMBER + 1):
            logfile_name = LOGFILES_PATH + "/baseline_{}_{}_{}.log".format(virtual_client, repetition, machine)
            #print(logfile_name)
            throughput, response = extractParams(logfile_name)
            # Agregates
            # SUM thriuputs
            aggregate_throughput += throughput
            # AVG response times
            avg_response_time += response

        avg_response_time /= MACHINES_NUMBER
        # at this point we have aggregates from all machines
        final_throughput += aggregate_throughput
        final_response_time += avg_response_time

        aggregate_throughput_vals.append(aggregate_throughput)
        avg_response_time_vals.append(avg_response_time)

    final_throughput /= REP_NUMBER
    final_response_time /= REP_NUMBER

    T_total.append(final_throughput)
    R_total.append(final_response_time)
    T_STD.append(stdev(aggregate_throughput_vals))
    R_STD.append(stdev(avg_response_time_vals))

# Build

# for msec
#R_total = [x / 1000 for x in R_total]
#R_STD = [x / 1000 for x in R_STD]

clients = [x * THREAD_PER_CLIENT * MACHINES_NUMBER for x in range(CLIENTS_RANGE_BEG, CLIENTS_RANGE_END + 1, CLIENTS_RANGE_STEP)]
ticks = [x * THREAD_PER_CLIENT * MACHINES_NUMBER for x in range(CLIENTS_RANGE_BEG, CLIENTS_RANGE_END + 1, CLIENTS_RANGE_STEP)]

print(clients)
print(T_total)
print(T_STD)

# throughput
plt.figure(1)
plt.errorbar(clients, T_total, yerr=T_STD, fmt='-o', ecolor='r')
plt.xticks(ticks)
plt.ylim(ymin=0)
plt.xlim(xmin=0)
plt.grid()
plt.xlabel('Clients')
plt.ylabel('Throughput')
plt.show()

# response time
plt.figure(2)
plt.errorbar(clients, R_total, yerr=R_STD, fmt='-o', ecolor='r')
plt.xticks(ticks)
plt.ylim(ymin=0)
plt.xlim(xmin=0)
plt.grid()
plt.xlabel('Clients')
plt.ylabel('Response time')
plt.show()

#print(final_throughput)
#print(final_response_time)

#TODO: try to do box plot for all clients
#T_col, R_col = extractParams(logfiles_path + "/baseline_28_1_1.log")
#print(T_col)
#print(R_col)