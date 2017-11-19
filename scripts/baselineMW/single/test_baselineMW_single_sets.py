#ASL Script to Run Experiment 3.1


#Bring Up MWare
#In a loop , invoke memtier on client machines  & COllect the logs
#Kill MWare & Collect Logs

import sys, paramiko
import time
import select

'''
Utility Functions
'''

#Execute Commands & Return the PID of the current Shell
def execute(channel, command):
    command = 'echo $$; exec ' + command
    stdin, stdout, stderr = channel.exec_command(command)
    pid = int(stdout.readline())
    return pid, stdin, stdout, stderr

#return SSH connection handle, input is hostname string
def connect_machine(host):
    ssh = paramiko.SSHClient()
    ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    ssh.connect(host, username='tivan', password='', port=22)
    print('Connected to ' + host)
    return ssh

def create_memtierCmd(server, port, clients, ttime, threads, ratio):
    memtierCmd  =  "./memtier_benchmark-master/memtier_benchmark --data-size=1024 --server="+server+"  --port="+str(port)+" --clients="+str(clients)+" --test-time="+str(ttime)+" --threads="+str(threads)+" --ratio="+ratio+" --expiry-range=9999-10000 --key-maximum=10000 --hide-histogram --protocol=memcache_text 2>&1"
    return memtierCmd

def exp31():


    #vclients = [1, 2, 4, 8, 16, 32]
    #mwareThreads = [1, 8, 16, 32, 64]

    LOG_DIR="log3.1/"

    vclients = [1, 5, 9, 13, 17, 21, 25, 29, 33]
    mwareThreads = [8, 16, 32, 64]

    testTime = 5 #For Each Experiment
    ratios = ["1:0", "0:1"] #WriteOnly ==> Read-Only

    mw_machine1 = 'tivanforaslvms4.westeurope.cloudapp.azure.com'
    mw_machine1_ip = "10.0.0.8"

    server = "tivanforaslvms6.westeurope.cloudapp.azure.com"
    server_ip = "10.0.0.9"

    client1 = 'tivanforaslvms1.westeurope.cloudapp.azure.com'

    runMware = "java -jar ~/asl-fall17-project/dist/middleware-tivan.jar -l " + mw_machine1_ip + " -p 8080 -s false"
    servers = " -m " + server_ip + ":9090"
    # mware_threads =" -t 1 "

    #Connect to MWare
    mw = connect_machine(mw_machine1)
    #Connect to Client
    cl1 = connect_machine(client1)

    for worker  in mwareThreads:
        for client in vclients:
            for repetition in range(3):
                runMwareFullCmd = runMware+servers+" -t " + str(worker)
                #Run Mware
                pid, stdin, stdout, stderr = execute(mw, runMwareFullCmd)
                print("Ran : " + runMwareFullCmd)
                time.sleep(2)  #Give Time To Initialize

                #Run memtier
                memtierCmd = create_memtierCmd(server=mw_machine1_ip, port=8080, clients=client, ttime=testTime, threads=2, ratio="1:0")

                print(memtierCmd)
                p, stin, stout, sterr = execute(cl1, memtierCmd)

                #Collect Memtier Output
                client_out = stout.readlines()

                client_file = open(LOG_DIR + str(worker) + "/" + "exp3.1_set_client_" + str(client)+ "_" + str(repetition+1) + "_1" + ".log", 'w')
                for cLine in client_out:
                    print(cLine)
                    client_file.write(cLine)
                client_file.close()

                print("Killing MWare")
                #Kill MWare
                mw.exec_command("kill " + str(pid))
                #Collect Output
                mware_out = stdout.readlines()
                mware_file = open(LOG_DIR + str(worker) + "/" + "exp3.1_set_mw_" + str(client)+ "_" + str(repetition+1) + "_1" + ".log", 'w')
                for mLine in mware_out:
                    print(mLine)
                    mware_file.write(mLine)

                #Close Files
                mware_file.close()
                client_file.close()


    print("***Done !!!***")


exp31()