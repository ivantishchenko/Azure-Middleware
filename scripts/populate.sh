#!/usr/bin/env bash


for value in {1..9}
    do
        echo -e "set my${value} 0 60 5\r\nhello\r"
    done | nc localhost 8080

