#!/usr/bin/env bash

server="optimus.inf.ethz.ch"
user="tivan"
cmd="nc -l 8080"

# PARAMS USER SERVER CMD
# USAGE: MW_PID=$(get_cmd_pid "$user" "$server" "$cmd")
# RETURNS PID of the remote SSH CMD
function get_cmd_pid() {
    #MW_PID_FILE="MW.pid"
    PID=$(ssh "${1}"@"${2}" "${3}"' > /dev/null 2>&1 & echo $!')
    echo $PID
}


#MW_PID_FILE="MW.pid"

#ssh ${user}@${server} ${cmd}' > /dev/null 2>&1 & echo $! > '${MW_PID_FILE}

#MW_PID=$(ssh ${user}@${server} cat ${MW_PID_FILE})

#MW_PID=$(ssh ${user}@${server} ${cmd}' > /dev/null 2>&1 & echo $!')

MW_PID=$(get_cmd_pid "$user" "$server" "$cmd")



echo $MW_PID

ssh ${user}@${server} kill ${MW_PID}