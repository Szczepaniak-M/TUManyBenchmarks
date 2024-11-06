#!/usr/bin/env bash

sleep 5
num_threads=$(lscpu | grep "^CPU(s):" | awk '{print $2}')
num_threads_plus_two=$(("$num_threads" + 2))
threads=$((num_threads_plus_two < 50 ? num_threads_plus_two : 50))
iperf -c node-2 -t 602 -P $threads -i 1 -f g >> results.txt
