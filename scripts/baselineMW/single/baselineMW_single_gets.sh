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
            cmd1="${cmdpart_GET} --server=${middleware1} --port=${mw_port} --test-time=${time} --clients=${c} --threads=${threads_single} --out-file=${LOG_FILE_DIR_SINGLE_GET_CLIENT}/${w}/baselineMW_${c}_${rep}_1.log"
            cmd_mw="${CMD_PART_MW} -t ${w} > ${LOG_FILE_DIR_SINGLE_GET_MW}/${w}/baselineMW_${c}_${rep}_1.log"

            MW_PID=$(get_cmd_pid "$user" "$middleware1" "$cmd_mw")

            sleep 3

            ssh ${user}@${client1} $cmd1
            #sleep $((time + 2))

            ssh ${user}@${middleware1} kill ${MW_PID}
        done
    done
done

echo "Done executing"