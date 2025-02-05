#!/usr/bin/env bash

function create_out_dirs_clients() {
    MKDIR_CMD="mkdir -p ${LOG_FILE_DIR_Client}"
    ssh ${user}@${client1} ${MKDIR_CMD}
    ssh ${user}@${client2} ${MKDIR_CMD}
    ssh ${user}@${client3} ${MKDIR_CMD}
}

function create_out_dirs_mw() {
    MKDIR_CMD="mkdir -p ${LOG_FILE_DIR_MW}"
    ssh ${user}@${middleware1} ${MKDIR_CMD}
    ssh ${user}@${middleware2} ${MKDIR_CMD}
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
client3="tivanforaslvms3.westeurope.cloudapp.azure.com"

middleware1="tivanforaslvms4.westeurope.cloudapp.azure.com"
middleware2="tivanforaslvms5.westeurope.cloudapp.azure.com"


middleware1_privat="10.0.0.8"
middleware2_privat="10.0.0.7"

server1="tivanforaslvms6.westeurope.cloudapp.azure.com"
server2="tivanforaslvms7.westeurope.cloudapp.azure.com"
server3="tivanforaslvms8.westeurope.cloudapp.azure.com"

server_privat1="10.0.0.9"
server_privat2="10.0.0.11"
server_privat3="10.0.0.5"

mw_port=8080
server_port=9090
time=60

threads_single=2
threads_double=1

clients=(`seq 1 4 33`)
MULTI_GET_KEYS=(1 3 6 9)

# Write only workload
cmdpart_GET="memtier_benchmark-master/memtier_benchmark --protocol=memcache_text --expiry-range=9999-10000 --key-maximum=10000 --data-size=1024"

RM_CMD="rm -rf log/"

CMD_PART_MW1="java -jar /home/tivan/asl-fall17-project/dist/middleware-tivan.jar -l ${middleware1_privat} -p ${mw_port} -s true -m ${server_privat1}:${server_port} ${server_privat2}:${server_port} ${server_privat3}:${server_port}"
CMD_PART_MW2="java -jar /home/tivan/asl-fall17-project/dist/middleware-tivan.jar -l ${middleware2_privat} -p ${mw_port} -s true -m ${server_privat1}:${server_port} ${server_privat2}:${server_port} ${server_privat3}:${server_port}"

server_cmd="memcached -p ${server_port} -t 1"

LOG_FILE_DIR_MW="logfiles_multiGET_shard_MW"
LOG_FILE_DIR_Client="logfiles_multiGET_shard_Client"


#create_out_dirs_mw
#create_out_dirs_clients

# launch MEMCACHED
ssh -f ${user}@${server1} "sh -c '${server_cmd} > /dev/null 2>&1 &'"
ssh -f ${user}@${server2} "sh -c '${server_cmd} > /dev/null 2>&1 &'"
ssh -f ${user}@${server3} "sh -c '${server_cmd} > /dev/null 2>&1 &'"


POPULATE_CMD1="memtier_benchmark-master/memtier_benchmark --protocol=memcache_text --ratio=1:0 --expiry-range=9999-10000 --key-maximum=10000 --data-size=1024 --server=${server_privat1} --port=${server_port} --test-time=30 --clients=2 --threads=2"
POPULATE_CMD2="memtier_benchmark-master/memtier_benchmark --protocol=memcache_text --ratio=1:0 --expiry-range=9999-10000 --key-maximum=10000 --data-size=1024 --server=${server_privat2} --port=${server_port} --test-time=30 --clients=2 --threads=2"
POPULATE_CMD3="memtier_benchmark-master/memtier_benchmark --protocol=memcache_text --ratio=1:0 --expiry-range=9999-10000 --key-maximum=10000 --data-size=1024 --server=${server_privat3} --port=${server_port} --test-time=30 --clients=2 --threads=2"

echo "Populate servers"
ssh ${user}@${client1} $POPULATE_CMD1 &
ssh ${user}@${client2} $POPULATE_CMD2 &
ssh ${user}@${client3} $POPULATE_CMD3 &

wait

VC_NUM=2

# repeat W times
for w in "${MULTI_GET_KEYS[@]}";
do
    echo "Number of keys = ${w}"
    # launch MW with w threads
        for rep in `seq 1 3`;
        do
            cmd1="${cmdpart_GET} --ratio=1:${w} --multi-key-get=${w} --server=${middleware1_privat} --port=${mw_port} --test-time=${time} --clients=${VC_NUM} --threads=${threads_double} --out-file=${LOG_FILE_DIR_Client}/multi_get_${w}_${rep}_1.log"
            cmd2="${cmdpart_GET} --ratio=1:${w} --multi-key-get=${w} --server=${middleware2_privat} --port=${mw_port} --test-time=${time} --clients=${VC_NUM} --threads=${threads_double} --out-file=${LOG_FILE_DIR_Client}/multi_get_${w}_${rep}_2.log"
            cmd3="${cmdpart_GET} --ratio=1:${w} --multi-key-get=${w} --server=${middleware1_privat} --port=${mw_port} --test-time=${time} --clients=${VC_NUM} --threads=${threads_double} --out-file=${LOG_FILE_DIR_Client}/multi_get_${w}_${rep}_3.log"
            cmd4="${cmdpart_GET} --ratio=1:${w} --multi-key-get=${w} --server=${middleware2_privat} --port=${mw_port} --test-time=${time} --clients=${VC_NUM} --threads=${threads_double} --out-file=${LOG_FILE_DIR_Client}/multi_get_${w}_${rep}_4.log"
            cmd5="${cmdpart_GET} --ratio=1:${w} --multi-key-get=${w} --server=${middleware1_privat} --port=${mw_port} --test-time=${time} --clients=${VC_NUM} --threads=${threads_double} --out-file=${LOG_FILE_DIR_Client}/multi_get_${w}_${rep}_5.log"
            cmd6="${cmdpart_GET} --ratio=1:${w} --multi-key-get=${w} --server=${middleware2_privat} --port=${mw_port} --test-time=${time} --clients=${VC_NUM} --threads=${threads_double} --out-file=${LOG_FILE_DIR_Client}/multi_get_${w}_${rep}_6.log"

            cmd_mw1="${CMD_PART_MW1} -t 64 > ${LOG_FILE_DIR_MW}/multi_get_${w}_${rep}_1.log &"
            cmd_mw2="${CMD_PART_MW2} -t 64 > ${LOG_FILE_DIR_MW}/multi_get_${w}_${rep}_2.log &"

            echo "Executing middleware part"

            ssh ${user}@${middleware1} $cmd_mw1
            sleep 2
            ssh ${user}@${middleware2} $cmd_mw2
            sleep 2

            echo "Executing client part"
            ssh ${user}@${client1} $cmd1 &
            ssh ${user}@${client1} $cmd2 &

            ssh ${user}@${client2} $cmd3 &
            ssh ${user}@${client2} $cmd4 &

            ssh ${user}@${client3} $cmd5 &
            ssh ${user}@${client3} $cmd6 &


            wait
            echo "Killing MW"

            kill_CMD="pkill --signal 15 -f 'java -jar'"
            ssh ${user}@${middleware1} $kill_CMD
            ssh ${user}@${middleware2} $kill_CMD

            sleep 2
            ssh ${user}@${middleware1} $RM_CMD
            ssh ${user}@${middleware2} $RM_CMD

        done
done

echo "Done executing"
