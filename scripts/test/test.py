#!/usr/bin/env python3

from paramiko import SSHClient, AutoAddPolicy
import time

ssh = SSHClient()
ssh.set_missing_host_key_policy(AutoAddPolicy())
ssh.connect('optimus.inf.ethz.ch', username='tivan', key_filename='/home/ivan/.ssh/id_rsa.pub', port=22)
sleeptime = 0.001
outdata, errdata = '', ''
ssh_transp = ssh.get_transport()
chan = ssh_transp.open_session()
# chan.settimeout(3 * 60 * 60)
chan.setblocking(0)
chan.exec_command('nc -l 7777')


while True:  # monitoring process
    # Reading from output streams
    while chan.recv_ready():
        outdata += chan.recv(1000).decode('utf-8')
    while chan.recv_stderr_ready():
        errdata += chan.recv_stderr(1000).decode('utf-8')
    if chan.exit_status_ready():  # If completed
        break
    time.sleep(sleeptime)

retcode = chan.recv_exit_status()
ssh_transp.close()

print(outdata)
print(errdata)