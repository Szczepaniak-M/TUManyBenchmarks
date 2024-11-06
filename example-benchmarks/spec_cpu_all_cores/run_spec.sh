#!/bin/bash

arch=$(lscpu | grep "Architecture" | awk '{print $2}')
cores=$(( $(lscpu | grep "^CPU(s):" | awk '{print $2}') ))

cd /home/ubuntu/specbench
source shrc
if [[ "$arch" == "x86_64" ]]; then
    runcpu --config=config_x86 --iterations=2 --copies=$cores --reportable intrate >> log.txt
elif [[ "$arch" == "aarch64" ]]; then
    runcpu --config=config_graviton --iterations=2 --copies=$cores --reportable intrate >> log.txt
fi
