#!/usr/bin/env bash

parent_dir=$(dirname $PWD)
source ${parent_dir}/baseline_mw.sh

# launch MEMCACHED
ssh -f ${user}@${server} "sh -c '${server_cmd} > /dev/null 2>&1 &'"

# repeat W times
for w in "${workers[@]}";
do
    echo "Number of workers = ${w}"
    # launch MW with w threads
    for c in "${clients[@]}";
    do
        for rep in `seq 1 3`;
        do
            cmd1="${cmdpart_SET} --server=${middleware1_privat} --port=${mw_port} --test-time=${time} --clients=${c} --threads=${threads_double} --out-file=${LOG_FILE_DIR_DOUBLE_SET_CLIENT}/${w}/baselineMW_${c}_${rep}_1.log"
            cmd2="${cmdpart_SET} --server=${middleware2_privat} --port=${mw_port} --test-time=${time} --clients=${c} --threads=${threads_double} --out-file=${LOG_FILE_DIR_DOUBLE_SET_CLIENT}/${w}/baselineMW_${c}_${rep}_2.log"

            cmd_mw1="${CMD_PART_MW1} -t ${w} > ${LOG_FILE_DIR_DOUBLE_SET_MW}/${w}/baselineMW_${c}_${rep}_1.log &"
            cmd_mw2="${CMD_PART_MW2} -t ${w} > ${LOG_FILE_DIR_DOUBLE_SET_MW}/${w}/baselineMW_${c}_${rep}_2.log &"

            echo "Executing middleware part"

            ssh ${user}@${middleware1} $cmd_mw1
            ssh ${user}@${middleware2} $cmd_mw2

            sleep 2

            echo "Executing client part"
            ssh ${user}@${client1} $cmd1 &
            ssh ${user}@${client1} $cmd2 &

            sleep $((time + 2))
            echo "Killing MW"

            kill_CMD="pkill --signal 15 -f 'java -jar'"
            ssh ${user}@${middleware1} $kill_CMD
            ssh ${user}@${middleware2} $kill_CMD

            sleep 2
            ssh ${user}@${middleware1} $RM_CMD
            ssh ${user}@${middleware2} $RM_CMD

        done
    done
done

echo "Done executing"