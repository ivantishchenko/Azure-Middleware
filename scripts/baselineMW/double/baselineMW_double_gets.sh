#!/usr/bin/env bash

parent_dir=$(dirname $PWD)
source ${parent_dir}/baseline_mw.sh

# repeat W times
for w in "${workers[@]}";
do
    echo "Number of workers = ${w}"
    # launch MW with w threads
    for c in "${clients[@]}";
    do
        for rep in `seq 1 3`;
        do
            cmd1="${cmdpart_GET} --server=${middleware1} --port=${mw_port} --test-time=${time} --clients=${c} --threads=${threads_double} --out-file=${LOG_FILE_DIR_DOUBLE_GET_CLIENT}/${w}/baselineMW_${c}_${rep}_1.log"
            cmd2="${cmdpart_GET} --server=${middleware2} --port=${mw_port} --test-time=${time} --clients=${c} --threads=${threads_double} --out-file=${LOG_FILE_DIR_DOUBLE_GET_CLIENT}/${w}/baselineMW_${c}_${rep}_2.log"

            cmd_mw1="${CMD_PART_MW} -t ${w} > ${LOG_FILE_DIR_DOUBLE_GET_MW}/${w}/baselineMW_${c}_${rep}_1.log"
            cmd_mw2="${CMD_PART_MW} -t ${w} > ${LOG_FILE_DIR_DOUBLE_GET_MW}/${w}/baselineMW_${c}_${rep}_2.log"

            MW_PID1=$(get_cmd_pid "$user" "$middleware1" "$cmd_mw1")
            MW_PID2=$(get_cmd_pid "$user" "$middleware2" "$cmd_mw2")

            sleep 2

            ssh ${user}@${client1} $cmd1 &
            ssh ${user}@${client1} $cmd2 &

            sleep $((time + 2))

            ssh ${user}@${middleware1} kill -15 ${MW_PID1}
            ssh ${user}@${middleware2} kill -15 ${MW_PID2}

            sleep 2
            rm -rf "log"

        done
    done
done

echo "Done executing"