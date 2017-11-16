#!/usr/bin/env bash

parent_dir=$(dirname $PWD)
source ${parent_dir}/baseline_mw.sh


function create_out_dirs_clients() {
    for i in "${workers[@]}";
    do
        MKDIR_CMD="mkdir -p ${LOG_FILE_DIR_SINGLE_SET_CLIENT}/${i} ${LOG_FILE_DIR_SINGLE_GET_CLIENT}/${i} ${LOG_FILE_DIR_DOUBLE_SET_CLIENT}/${i} ${LOG_FILE_DIR_DOUBLE_GET_CLIENT}/${i}"
        eval ${MKDIR_CMD}
        eval ${MKDIR_CMD}
    done
}

function create_out_dirs_mw() {
    for i in "${workers[@]}";
    do
        MKDIR_CMD="mkdir -p ${LOG_FILE_DIR_SINGLE_SET_MW}/${i} ${LOG_FILE_DIR_SINGLE_GET_MW}/${i} ${LOG_FILE_DIR_DOUBLE_SET_MW}/${i} ${LOG_FILE_DIR_DOUBLE_GET_MW}/${i}"
        eval ${MKDIR_CMD}
        eval ${MKDIR_CMD}
    done
}

create_out_dirs_clients
create_out_dirs_mw

middleware1="127.0.0.1"
middleware2="127.0.0.1"
mw_port1="8080"
mw_port2="8081"
server="127.0.0.1"
server_port="9090"
time=5

server_cmd="memcached -p ${server_port} -t 1"
CMD_PART_MW1="java -jar /home/ivan/asl-fall17-project/dist/middleware-tivan.jar -l 127.0.0.1 -p ${mw_port1} -s false -m ${server}:${server_port}"
CMD_PART_MW2="java -jar /home/ivan/asl-fall17-project/dist/middleware-tivan.jar -l 127.0.0.1 -p ${mw_port2} -s false -m ${server}:${server_port}"

cmdpart_SET="memtier_benchmark --protocol=memcache_text --ratio=1:0 --expiry-range=9999-10000 --key-maximum=10000 --data-size=1024 --hide-histogram"
cmdpart_GET="memtier_benchmark --protocol=memcache_text --ratio=0:1 --expiry-range=9999-10000 --key-maximum=10000 --data-size=1024 --hide-histogram"

eval $server_cmd &

# repeat W times
for w in "${workers[@]}";
do
    echo "Number of workers = ${w}"
    # launch MW with w threads
    for c in "${clients[@]}";
    do
        for rep in `seq 1 3`;
        do
            cmd1="${cmdpart_SET} --server=${middleware1} --port=${mw_port1} --test-time=${time} --clients=${c} --threads=${threads_double} --out-file=${LOG_FILE_DIR_DOUBLE_SET_CLIENT}/${w}/baselineMW_${c}_${rep}_1.log"
            cmd2="${cmdpart_SET} --server=${middleware2} --port=${mw_port2} --test-time=${time} --clients=${c} --threads=${threads_double} --out-file=${LOG_FILE_DIR_DOUBLE_SET_CLIENT}/${w}/baselineMW_${c}_${rep}_2.log"

            cmd_mw1="${CMD_PART_MW1} -t ${w} > ${LOG_FILE_DIR_DOUBLE_SET_MW}/${w}/baselineMW_${c}_${rep}_1.log &"
            cmd_mw2="${CMD_PART_MW2} -t ${w} > ${LOG_FILE_DIR_DOUBLE_SET_MW}/${w}/baselineMW_${c}_${rep}_2.log &"

            eval $cmd_mw1
            MW_PID1=$!
            echo $MW_PID1

            eval $cmd_mw2
            MW_PID2=$!
            echo $MW_PID2

            sleep 2

            eval $cmd1 &
            eval $cmd2 &

            sleep $((time + 2))

            kill -15 ${MW_PID1}
            kill -15 ${MW_PID2}

            sleep 2
            ssh ${user}@${middleware1} $RM_CMD
            ssh ${user}@${middleware2} $RM_CMD

        done
    done
done

echo "Done executing"