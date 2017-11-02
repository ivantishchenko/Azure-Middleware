#!/bin/bash

# Write only workload
cmdpart="memtier_benchmark --protocol=memcache_text --ratio=1:0 --expiry-range=9999-10000 --key-maximum=10000 --data-size=1024 --hide-histogram"

server="localhost"
port=9090
time=60

#Check if there is an argument to the script
if [[ $# > 0 ]]; then server="$1"; fi
#Check if there is a 2nd argument to the script
if [[ $# > 1 ]]; then port="$2"; fi
# check if there is 3d
if [[ $# > 2 ]]; then time="$3"; fi

LOG_FILE_DIR=~/logfiles_baseline_1_server_SET

if [ ! -d $LOG_FILE_DIR ]; then
    echo "Creating logfiles directory"
    mkdir -p $LOG_FILE_DIR
fi

#TODO: SSH to a server machine and launch memcached
#ssh tivan@SERVERMACINE "memcached -p 9090 -t 1 &"

threads=2
#clients=(1 `seq 4 4 32`)
# 1 4 33
#clients=(`seq 1 1 32`)
powers=(`seq 0 1 5`)

for c in "${powers[@]}";
do
	for rep in `seq 1 3`;
	do

        power=$((2**$c))
        echo $power
		#for different client machines
		cmd1="${cmdpart} --server=${server} --port=${port} --test-time=${time} --clients=${power} --threads=${threads} &> ${LOG_FILE_DIR}/baseline_${power}_${rep}_1.log &"
		cmd2="${cmdpart} --server=${server} --port=${port} --test-time=${time} --clients=${power} --threads=${threads} &> ${LOG_FILE_DIR}/baseline_${power}_${rep}_2.log &"
		cmd3="${cmdpart} --server=${server} --port=${port} --test-time=${time} --clients=${power} --threads=${threads} &> ${LOG_FILE_DIR}/baseline_${power}_${rep}_3.log &"

        #TODO: put SSH instead of eval
		eval $cmd1
		eval $cmd2
		eval $cmd3

		sleep $((time + 2))
	done
done

echo "Done executing"