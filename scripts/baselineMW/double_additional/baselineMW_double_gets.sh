#!/usr/bin/env bash

parent_dir=$(dirname $PWD)
source ${parent_dir}/baseline_mw.sh


POPULATE_CMD1="memtier_benchmark-master/memtier_benchmark --protocol=memcache_text --ratio=1:0 --expiry-range=9999-10000 --key-maximum=10000 --data-size=1024 --server=${server_privat} --port=${server_port} --test-time=30 --clients=2 --threads=2"

#echo "Populate servers"
#ssh ${user}@${client1} $POPULATE_CMD1 &

#wait

MAX_VC=32
MAX_WT=64

# repeat W times
#for w in "${workers[@]}";
#do
    echo "Number of workers = ${MAX_WT}"
    # launch MW with w threads
    for c in "${clients[@]}";
    do
        for rep in `seq 1 3`;
        do
            cmd1="${cmdpart_GET} --server=${middleware1_privat} --port=${mw_port} --test-time=${time} --clients=${c} --threads=${threads_double} --out-file=${LOG_FILE_DIR_DOUBLE_GET_CLIENT_ADD}/${MAX_WT}/baselineMW_${c}_${rep}_1.log"
            cmd2="${cmdpart_GET} --server=${middleware2_privat} --port=${mw_port} --test-time=${time} --clients=${c} --threads=${threads_double} --out-file=${LOG_FILE_DIR_DOUBLE_GET_CLIENT_ADD}/${MAX_WT}/baselineMW_${c}_${rep}_2.log"
            cmd3="${cmdpart_GET} --server=${middleware1_privat} --port=${mw_port} --test-time=${time} --clients=${c} --threads=${threads_double} --out-file=${LOG_FILE_DIR_DOUBLE_GET_CLIENT_ADD}/${MAX_WT}/baselineMW_${c}_${rep}_3.log"
            cmd4="${cmdpart_GET} --server=${middleware2_privat} --port=${mw_port} --test-time=${time} --clients=${c} --threads=${threads_double} --out-file=${LOG_FILE_DIR_DOUBLE_GET_CLIENT_ADD}/${MAX_WT}/baselineMW_${c}_${rep}_4.log"

            cmd_mw1="${CMD_PART_MW1} -t ${MAX_WT} > ${LOG_FILE_DIR_DOUBLE_GET_MW_ADD}/${MAX_WT}/baselineMW_${c}_${rep}_1.log &"
            cmd_mw2="${CMD_PART_MW2} -t ${MAX_WT} > ${LOG_FILE_DIR_DOUBLE_GET_MW_ADD}/${MAX_WT}/baselineMW_${c}_${rep}_2.log &"

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
#done

echo "Done executing"