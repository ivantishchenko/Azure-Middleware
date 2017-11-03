#!/bin/bash

#export export PATH="~/memtier_benchmark-master:$PATH"

# Write only workload
cmdpart="memtier_benchmark --protocol=memcache_text --ratio=0:1 --expiry-range=9999-10000 --key-maximum=10000 --data-size=1024 --hide-histogram"

user="tivan"
server="tivanforaslvms6.westeurope.cloudapp.azure.com"

client1="tivanforaslvms1.westeurope.cloudapp.azure.com"
client2="tivanforaslvms2.westeurope.cloudapp.azure.com"
client3="tivanforaslvms3.westeurope.cloudapp.azure.com"

port=9090
time=2

#Check if there is an argument to the script
if [[ $# > 0 ]]; then server="$1"; fi
#Check if there is a 2nd argument to the script
if [[ $# > 1 ]]; then port="$2"; fi
# check if there is 3d
if [[ $# > 2 ]]; then time="$3"; fi


$server_cmd= "memcached -p ${port} -t 1"
ssh -f ${user}@${server} "sh -c '${server_cmd} > /dev/null 2>&1 &'"

LOG_FILE_DIR=~/logfiles_baseline_1_server_GET

threads=2
powers=(`seq 0 1 5`)

for c in "${powers[@]}";
do
	for rep in `seq 1 3`;
	do

        power=$((2**$c))
        echo $power
		#for different client machines
		cmd1="${cmdpart} --server=${server} --port=${port} --test-time=${time} --clients=${power} --threads=${threads} --out-file=${LOG_FILE_DIR}/baseline_${power}_${rep}_1.log"
		cmd2="${cmdpart} --server=${server} --port=${port} --test-time=${time} --clients=${power} --threads=${threads} --out-file=${LOG_FILE_DIR}/baseline_${power}_${rep}_2.log"
		cmd3="${cmdpart} --server=${server} --port=${port} --test-time=${time} --clients=${power} --threads=${threads} --out-file=${LOG_FILE_DIR}/baseline_${power}_${rep}_3.log"

		#ssh tivan@tivanforaslvms1.westeurope.cloudapp.azure.com $cmd1 &
		#ssh tivan@tivanforaslvms2.westeurope.cloudapp.azure.com $cmd2 &
		#ssh tivan@tivanforaslvms3.westeurope.cloudapp.azure.com $cmd3 &

        ssh -f ${user}@${client1} "sh -c '${cmd1} > /dev/null 2>&1 &'"
        ssh -f ${user}@${client2} "sh -c '${cmd2} > /dev/null 2>&1 &'"
        ssh -f ${user}@${client3} "sh -c '${cmd3} > /dev/null 2>&1 &'"

		sleep $((time + 2))
	done
done

echo "Done executing"