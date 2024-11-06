#!/usr/bin/env bash

echo "threads,bw,sum" > results.csv
threads=(1 2 4 6 8 12 16 18 20 22 24 28 32 36 40 44 48) 
# fill 70% of RAM available per NUMA node with uint64_t
size=$(free -b | grep Mem: | awk '{print $2}')
numa_nodes=$(lscpu | grep "NUMA node(s):" | awk '{print $3}')
input_size=$(echo "$size / $numa_nodes * 0.7 / 8" | bc)
huge_pages=$(echo "$input_size * 8 / 1024 / 1024 / 2 + 1" | bc)
sudo bash -c "echo $huge_pages > /sys/devices/system/node/node0/hugepages/hugepages-2048kB/nr_hugepages"

for t_count in "${threads[@]}";
do
  numactl --cpubind=0 --membind=0 ./membw $input_size $t_count 10 >> results.csv
done
