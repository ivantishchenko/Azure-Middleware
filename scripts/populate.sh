#!/usr/bin/env bash

for value in {1..26}
    do
        echo -e "set my${value} 0 60 5\r\nhello\r" | nc localhost 8080 -w 1
    done