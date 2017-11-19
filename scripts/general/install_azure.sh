#!/usr/bin/env bash

cd ~ &&
sudo apt-get update &&
sudo apt-get install memcached git unzip ant dstat &&
wget https://github.com/ivantishchenko/memtier_benchmark/archive/master.zip &&
unzip master.zip &&
cd memtier_benchmark-master &&
sudo apt-get install build-essential autoconf automake libpcre3-dev libevent-dev pkg-config zlib1g-dev &&
autoreconf -ivf &&
./configure &&
make &&
sudo service memcached stop &&
echo 'export PATH="~/memtier_benchmark-master:$PATH"' >> ~/.bashrc

# JAVA 8
#sudo add-apt-repository ppa:webupd8team/java
#sudo apt update; sudo apt install oracle-java8-installer
#sudo apt install oracle-java8-set-default
#javac -version