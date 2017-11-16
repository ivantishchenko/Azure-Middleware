#!/usr/bin/env bash

function create_out_dirs_clients() {
    for i in "${workers[@]}";
    do
        MKDIR_CMD="mkdir -p ${LOG_FILE_DIR_SINGLE_SET_CLIENT}/${i} ${LOG_FILE_DIR_SINGLE_GET_CLIENT}/${i} ${LOG_FILE_DIR_DOUBLE_SET_CLIENT}/${i} ${LOG_FILE_DIR_DOUBLE_GET_CLIENT}/${i}"
        ssh ${user}@${client1} ${MKDIR_CMD}
        ssh ${user}@${client2} ${MKDIR_CMD}
    done
}

function create_out_dirs_mw() {
    for i in "${workers[@]}";
    do
        MKDIR_CMD="mkdir -p ${LOG_FILE_DIR_SINGLE_SET_MW}/${i} ${LOG_FILE_DIR_SINGLE_GET_MW}/${i} ${LOG_FILE_DIR_DOUBLE_SET_MW}/${i} ${LOG_FILE_DIR_DOUBLE_GET_MW}/${i}"
        ssh ${user}@${middleware1} ${MKDIR_CMD}
        ssh ${user}@${middleware2} ${MKDIR_CMD}
    done
}

# PARAMS USER SERVER CMD
# USAGE: MW_PID=$(get_cmd_pid "$user" "$server" "$cmd")
# RETURNS PID of the remote SSH CMD
function get_cmd_pid() {
    #MW_PID_FILE="MW.pid"
    PID=$(ssh "${1}"@"${2}" "${3}"' > /dev/null 2>&1 & echo $!')
    echo $PID
}

# CONSTANTS

user="tivan"

client1="tivanforaslvms1.westeurope.cloudapp.azure.com"
client2="tivanforaslvms2.westeurope.cloudapp.azure.com"

middleware1="tivanforaslvms4.westeurope.cloudapp.azure.com"
middleware2="tivanforaslvms5.westeurope.cloudapp.azure.com"

server="tivanforaslvms6.westeurope.cloudapp.azure.com"

mw_port=8080
server_port=9090
time=60

threads_single=2
threads_double=1

clients=(`seq 1 4 33`)
workers=(8 16 32 64)

# Write only workload
cmdpart_SET="memtier_benchmark-master/memtier_benchmark --protocol=memcache_text --ratio=1:0 --expiry-range=9999-10000 --key-maximum=10000 --data-size=1024 --hide-histogram"
cmdpart_GET="memtier_benchmark-master/memtier_benchmark --protocol=memcache_text --ratio=0:1 --expiry-range=9999-10000 --key-maximum=10000 --data-size=1024 --hide-histogram"

RM_CMD="rm -rf log/"
CMD_PART_MW="java -jar middleware-tivan.jar -l 127.0.0.1 -p ${mw_port} -s false -m ${server}:${server_port}"
server_cmd="memcached -p ${server_port} -t 1"

LOG_FILE_DIR_SINGLE_SET_CLIENT="logfiles_baselineMW_single_SET_client"
LOG_FILE_DIR_SINGLE_GET_CLIENT="logfiles_baselineMW_single_GET_client"
LOG_FILE_DIR_DOUBLE_SET_CLIENT="logfiles_baselineMW_double_SET_client"
LOG_FILE_DIR_DOUBLE_GET_CLIENT="logfiles_baselineMW_double_GET_client"

LOG_FILE_DIR_SINGLE_SET_MW="logfiles_baselineMW_single_SET_MW"
LOG_FILE_DIR_SINGLE_GET_MW="logfiles_baselineMW_single_GET_MW"
LOG_FILE_DIR_DOUBLE_SET_MW="logfiles_baselineMW_double_SET_MW"
LOG_FILE_DIR_DOUBLE_GET_MW="logfiles_baselineMW_double_GET_MW"

