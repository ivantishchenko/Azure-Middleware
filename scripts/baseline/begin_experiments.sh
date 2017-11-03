#!/usr/bin/env bash

user="tivan"

client1="tivanforaslvms1.westeurope.cloudapp.azure.com"
client2="tivanforaslvms2.westeurope.cloudapp.azure.com"
client3="tivanforaslvms3.westeurope.cloudapp.azure.com"

server1="tivanforaslvms6.westeurope.cloudapp.azure.com"
server2="tivanforaslvms7.westeurope.cloudapp.azure.com"

port=9090
time=2

LOG_FILE_DIR1=~/logfiles_baseline_1_server_SET
LOG_FILE_DIR2=~/logfiles_baseline_1_server_GET
LOG_FILE_DIR3=~/logfiles_baseline_2_servers_SET
LOG_FILE_DIR4=~/logfiles_baseline_2_servers_GET

MKDIR_CMD="mkdir ${LOG_FILE_DIR1} ${LOG_FILE_DIR2} ${LOG_FILE_DIR3} ${LOG_FILE_DIR4}"
DSTAT_CMD="dstat --time --cpu --mem --net --output ~/report.csv"

# CREATE LOGS DIR AND START DSTATS
ssh -f ${user}@${server1} "sh -c '${MKDIR_CMD} > /dev/null 2>&1 &'"
ssh -f ${user}@${server2} "sh -c '${MKDIR_CMD} > /dev/null 2>&1 &'"

ssh -f ${user}@${client1} "sh -c '${MKDIR_CMD} > /dev/null 2>&1 &'"
ssh -f ${user}@${client2} "sh -c '${MKDIR_CMD} > /dev/null 2>&1 &'"
ssh -f ${user}@${client3} "sh -c '${MKDIR_CMD} > /dev/null 2>&1 &'"



ssh -f ${user}@${server1} "sh -c '${DSTAT_CMD} > /dev/null 2>&1 &'"
ssh -f ${user}@${server2} "sh -c '${DSTAT_CMD} > /dev/null 2>&1 &'"

ssh -f ${user}@${client1} "sh -c '${DSTAT_CMD} > /dev/null 2>&1 &'"
ssh -f ${user}@${client2} "sh -c '${DSTAT_CMD} > /dev/null 2>&1 &'"
ssh -f ${user}@${client3} "sh -c '${DSTAT_CMD} > /dev/null 2>&1 &'"