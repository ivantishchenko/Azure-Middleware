import re
import sys
from operator import add
from matplotlib import pyplot as plt
from statistics import stdev

REP_NUMBER = 3
LOGFILES_PATH = sys.argv[1]
MACHINES_NUMBER = 2
THREAD_PER_CLIENT = 1

# CHANGE
RUN_TIME = 60
# See the same in bash script
# clients=`seq 1 4 33`
CLIENTS_RANGE_BEG = 1
CLIENTS_RANGE_END = 33
CLIENTS_RANGE_STEP = 4


def extractParams(logfile):
    file = open(logfile, 'r')

    for line in file:
        if line.startswith("Totals"):
            avg_throughput = line.split()[1]
            avg_response_time = line.split()[4]

    file.close()

    return float(avg_throughput), float(avg_response_time)


NUM_TICKS = len(list(range(CLIENTS_RANGE_BEG, CLIENTS_RANGE_END + 1, CLIENTS_RANGE_STEP)))

# Build
T_total = []
R_total = []
T_STD = []
R_STD = []

for virtual_client in range(CLIENTS_RANGE_BEG, CLIENTS_RANGE_END + 1, CLIENTS_RANGE_STEP):
    final_throughput = [0] * MACHINES_NUMBER
    final_response_time = [0] * MACHINES_NUMBER

    aggregate_throughput_vals = [[0] * REP_NUMBER] * MACHINES_NUMBER
    avg_response_time_vals = [[0] * REP_NUMBER] * MACHINES_NUMBER

    for repetition in range(1, REP_NUMBER + 1):

        for machine in range(1, MACHINES_NUMBER + 1):
            logfile_name = LOGFILES_PATH + "/baseline_{}_{}_{}.log".format(virtual_client, repetition, machine)
            #print(logfile_name)
            throughput, response = extractParams(logfile_name)
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

    for i in range(MACHINES_NUMBER):
        final_throughput[i] /= REP_NUMBER
        final_response_time[i] /= REP_NUMBER


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

clients = [x * THREAD_PER_CLIENT * MACHINES_NUMBER for x in range(CLIENTS_RANGE_BEG, CLIENTS_RANGE_END + 1, CLIENTS_RANGE_STEP)]
ticks = [x * THREAD_PER_CLIENT * MACHINES_NUMBER for x in range(CLIENTS_RANGE_BEG, CLIENTS_RANGE_END + 1, CLIENTS_RANGE_STEP)]

print(len(clients))
print(T_total)
print(T_STD)

# throughput
plt.figure(1)

colors = ['g', 'b', 'y']
for i in range(MACHINES_NUMBER):
    OUT_T = []
    OUT_STD = []
    for j in range(NUM_TICKS):
        OUT_T.append(T_total[j][i])
        OUT_STD.append(T_STD[j][i])

    plt.errorbar(clients, OUT_T, yerr=OUT_STD, fmt='-o', ecolor='r', color=colors[i % MACHINES_NUMBER], label="Memtier instance " + str(i+1))

plt.legend()
plt.xticks(ticks)
plt.ylim(ymin=0)
plt.xlim(xmin=0)
plt.grid()
plt.xlabel('Clients')
plt.ylabel('Throughput')
plt.show()

# response time
plt.figure(2)

colors = ['g', 'b', 'y']
for i in range(MACHINES_NUMBER):
    OUT_R = []
    OUT_STD = []
    for j in range(NUM_TICKS):
        OUT_R.append(R_total[j][i])
        OUT_STD.append(R_STD[j][i])

    plt.errorbar(clients, OUT_R, yerr=OUT_STD, fmt='-o', ecolor='r', color=colors[i % MACHINES_NUMBER], label="Memtier instance " + str(i+1))

plt.legend()
plt.xticks(ticks)
plt.ylim(ymin=0)
plt.xlim(xmin=0)
plt.grid()
plt.xlabel('Clients')
plt.ylabel('Response time')
plt.show()