#!/usr/bin/env bash

user="tivan"

client1="tivanforaslvms1.westeurope.cloudapp.azure.com"
client2="tivanforaslvms2.westeurope.cloudapp.azure.com"
client3="tivanforaslvms3.westeurope.cloudapp.azure.com"

server1="tivanforaslvms6.westeurope.cloudapp.azure.com"
server2="tivanforaslvms7.westeurope.cloudapp.azure.com"

port=9090
time=2

LOG_FILE_DIR1=logfiles_baseline_1_server_SET
LOG_FILE_DIR2=logfiles_baseline_1_server_GET
LOG_FILE_DIR3=logfiles_baseline_2_servers_SET
LOG_FILE_DIR4=logfiles_baseline_2_servers_GET

MKDIR_CMD="mkdir ${LOG_FILE_DIR1} ${LOG_FILE_DIR2} ${LOG_FILE_DIR3} ${LOG_FILE_DIR4}"
DSTAT_CMD="dstat --time --cpu --mem --net --output ~/report.csv"

# CREATE LOGS DIR AND START DSTATS
ssh ${user}@${server1} ${MKDIR_CMD}
ssh ${user}@${server2} ${MKDIR_CMD}

ssh ${user}@${client1} ${MKDIR_CMD}
ssh ${user}@${client2} ${MKDIR_CMD}
ssh ${user}@${client3} ${MKDIR_CMD}


#ssh -f ${user}@${server1} "sh -c '${DSTAT_CMD} > /dev/null 2>&1 &'"
#ssh -f ${user}@${server2} "sh -c '${DSTAT_CMD} > /dev/null 2>&1 &'"

#ssh -f ${user}@${client1} "sh -c '${DSTAT_CMD} > /dev/null 2>&1 &'"
#ssh -f ${user}@${client2} "sh -c '${DSTAT_CMD} > /dev/null 2>&1 &'"
#ssh -f ${user}@${client3} "sh -c '${DSTAT_CMD} > /dev/null 2>&1 &'"


#PING_CMD1="ping ${server1} -c 10 >> ping_server1.log"
#PING_CMD2="ping ${server2} -c 10 >> ping_server2.log"

#ssh -f ${user}@${client1} "sh -c '${PING_CMD1} > /dev/null 2>&1 &'"
#ssh -f ${user}@${client2} "sh -c '${PING_CMD1} > /dev/null 2>&1 &'"
#ssh -f ${user}@${client3} "sh -c '${PING_CMD1} > /dev/null 2>&1 &'"

#sleep 3

#ssh -f ${user}@${client1} "sh -c '${PING_CMD2} > /dev/null 2>&1 &'"
#ssh -f ${user}@${client2} "sh -c '${PING_CMD2} > /dev/null 2>&1 &'"
#ssh -f ${user}@${client3} "sh -c '${PING_CMD2} > /dev/null 2>&1 &'"

#
#
ssh ${user}@${server1} screen -S DSTAT -d -m "dstat --time --cpu --mem --net --output ~/report.csv"
ssh ${user}@${server2} screen -S DSTAT -d -m "dstat --time --cpu --mem --net --output ~/report.csv"

ssh ${user}@${client1} screen -S DSTAT -d -m "dstat --time --cpu --mem --net --output ~/report.csv"
ssh ${user}@${client2} screen -S DSTAT -d -m "dstat --time --cpu --mem --net --output ~/report.csv"
ssh ${user}@${client3} screen -S DSTAT -d -m "dstat --time --cpu --mem --net --output ~/report.csv"