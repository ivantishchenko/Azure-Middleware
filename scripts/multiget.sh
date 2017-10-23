#!/usr/bin/env bash

var=""
for i in {1..26}
    do
        var=${var}" ""my"${i}
    done


var="get ${var} \r"
echo -e $var | nc localhost 8080 -w 5