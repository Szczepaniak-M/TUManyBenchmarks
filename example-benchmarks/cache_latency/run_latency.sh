#!/usr/bin/env bash

convert_to_bytes() {
    size=$1
    unit=$2
    instances=$3

    case $unit in
        KiB) echo $(echo "$size * 1024 / $instances / 1" | bc) ;;
        MiB) echo $(echo "$size * 1024 * 1024 / $instances / 1" | bc) ;;
        GiB) echo $(echo "$size * 1024 * 1024 * 1024 / $instances / 1" | bc) ;;
        *) echo $size ;;
    esac
}

interpolate() {
    local s=$1
    local e=$2
    local num_points=$3

    for ((i=0; i<num_points; i++)); do
        # Generate value with a higher density towards the edges
        x=$(echo "$i / ($num_points - 1)" | bc -l)  # Linear interpolation between 0 and 1

        # Sigmoid-like function using awk (adjustable slope factor 12 for sharper edges)
        adj_x=$(awk -v x="$x" 'BEGIN { print 1 / (1 + exp(-12 * (x - 0.5))) }')

        # Final value between start and end
        value=$(awk -v s="$s" -v e="$e" -v adj_x="$adj_x" 'BEGIN { print int(s + e * adj_x) }')

        echo "$value"
    done
}

lscpu_output=$(lscpu)

l1d=$(echo "$lscpu_output" | grep "L1d" | awk '{print $3, $4}')
l2=$(echo "$lscpu_output" | grep "L2" | awk '{print $3, $4}')
l3=$(echo "$lscpu_output" | grep "L3" | awk '{print $3, $4}')

l1d_instances=$(echo "$lscpu_output" | grep "L1d" | awk '{print $5}' | sed 's/[^0-9]//g')
l2_instances=$(echo "$lscpu_output" | grep "L2" | awk '{print $5}' | sed 's/[^0-9]//g')
l3_instances=$(echo "$lscpu_output" | grep "L3" | awk '{print $5}' | sed 's/[^0-9]//g')

l1d_bytes=$(convert_to_bytes $(echo $l1d) $l1d_instances)
l2_bytes=$(convert_to_bytes $(echo $l2) $l2_instances)
l3_bytes=$(convert_to_bytes $(echo $l3) $l3_instances)

l12_bytes=$(("$l1d_bytes + $l2_bytes"))
l123_bytes=$(("$l1d_bytes + $l2_bytes + $l3_bytes"))
double_cache=$(echo "$l123_bytes * 2" | bc)
triple_cache=$(echo "$l123_bytes * 3" | bc)
cache_sizes=(
  $(interpolate 0 $l1d_bytes 10)
  "$l1d_bytes"
  $(interpolate $l1d_bytes $l2_bytes 10)
  "$l12_bytes"
  $(interpolate $l12_bytes $l3_bytes 16)
  "$l123_bytes"
  $(interpolate $l123_bytes $double_cache 8)
  "$triple_cache"
)

for cache_size in "${cache_sizes[@]}";
do
  numactl --cpubind=0 --membind=0 ./latency $cache_size 200000000 >> results.csv
done
