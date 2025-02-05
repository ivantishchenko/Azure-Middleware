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
mw_port="8080"
server="127.0.0.1"
server_port="9090"
time=30

server_cmd="memcached -p ${server_port} -t 1"
CMD_PART_MW="java -jar /home/ivan/asl-fall17-project/dist/middleware-tivan.jar -l 127.0.0.1 -p ${mw_port} -s false -m ${server}:${server_port}"
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
            cmd1="${cmdpart_GET} --server=${middleware1} --port=${mw_port} --test-time=${time} --clients=${c} --threads=${threads_single} --out-file=${LOG_FILE_DIR_SINGLE_GET_CLIENT}/${w}/baselineMW_${c}_${rep}_1.log"
            cmd_mw="${CMD_PART_MW} -t ${w} > ${LOG_FILE_DIR_SINGLE_GET_MW}/${w}/baselineMW_${c}_${rep}_1.log &"

            eval $cmd_mw
            MW_PID=$!

            echo $MW_PID
            sleep 2

            echo "Memtier = ${w}"
            eval $cmd1 &
            sleep $((time + 2))

            kill -15 ${MW_PID}
            sleep 2
            rm -rf "log"
        done
    done
done

echo "Done executing"