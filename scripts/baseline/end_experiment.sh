#!/usr/bin/env bash

user="tivan"

client1="tivanforaslvms1.westeurope.cloudapp.azure.com"
client2="tivanforaslvms2.westeurope.cloudapp.azure.com"
client3="tivanforaslvms3.westeurope.cloudapp.azure.com"

server1="tivanforaslvms6.westeurope.cloudapp.azure.com"
server2="tivanforaslvms7.westeurope.cloudapp.azure.com"

DSTAT_CMD="pkill -f dstat"
MEMCACHED_CMD="pkill -f memcached"



ssh -f ${user}@${server1} "sh -c '${DSTAT_CMD}; ${MEMCACHED_CMD} > /dev/null 2>&1 &'"
ssh -f ${user}@${server2} "sh -c '${DSTAT_CMD}; ${MEMCACHED_CMD} > /dev/null 2>&1 &'"

ssh -f ${user}@${client1} "sh -c '${DSTAT_CMD}; ${MEMCACHED_CMD} > /dev/null 2>&1 &'"
ssh -f ${user}@${client2} "sh -c '${DSTAT_CMD}; ${MEMCACHED_CMD} > /dev/null 2>&1 &'"
ssh -f ${user}@${client3} "sh -c '${DSTAT_CMD}; ${MEMCACHED_CMD} > /dev/null 2>&1 &'"