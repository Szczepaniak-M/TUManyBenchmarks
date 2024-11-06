import re
import sys
import json
from collections import defaultdict

def parse_iperf(file_path):
    connection_bandwidths = defaultdict(lambda: defaultdict(float))
    sum_bandwidth = defaultdict(float)
    min_bandwidth = defaultdict(float)
    max_bandwidth = defaultdict(float)
    avg_bandwidth = defaultdict(float)
    intervals = set()

    connection_pattern = re.compile(r'\[\s*(\d+)\]\s+([\d.]+)-([\d.]+)\s+sec\s+([\d.]+)\s+GBytes\s+([\d.]+)\s+Gbits/sec')
    sum_pattern = re.compile(r'\[SUM\]\s+([\d.]+)-([\d.]+)\s+sec\s+([\d.]+)\s+GBytes\s+([\d.]+)\s+Gbits/sec')

    with open(file_path, 'r') as f:
        for line in f:
            connection_match = connection_pattern.search(line)
            if connection_match:
                connection_id = int(connection_match.group(1))
                start_time = float(connection_match.group(2))
                bandwidth = float(connection_match.group(5))

                connection_bandwidths[connection_id][start_time] = bandwidth
                intervals.add(start_time)

            sum_match = sum_pattern.search(line)
            if sum_match:
                start_time = float(sum_match.group(1))
                bandwidth_sum = float(sum_match.group(4))

                sum_bandwidth[start_time] = bandwidth_sum
                intervals.add(start_time)
    if not sum_bandwidth:
        sum_bandwidth = connection_bandwidths[1]
    intervals = sorted(intervals)[1:-1]
    for i in intervals:
        interval = []
        for j in range(1, len(connection_bandwidths) + 1):
            interval.append(connection_bandwidths[j][i])
        min_bandwidth[i] = min(interval)
        max_bandwidth[i] = max(interval)
        avg_bandwidth[i] = sum(interval) / len(interval)

    data = {
        'connection_min': [min_bandwidth.get(i, 0) for i in intervals],
        'connection_max': [max_bandwidth.get(i, 0) for i in intervals],
        'connection_avg': [avg_bandwidth.get(i, 0) for i in intervals],
        'sum_bandwidth': [sum_bandwidth.get(i, 0) for i in intervals]
    }

    return data

if __name__ == "__main__":
    if len(sys.argv) != 2:
        print("Usage: python3 format_output.py <input_file_1>")
        sys.exit(1)

    file_path = sys.argv[1]
    data = parse_iperf(file_path)
    print(json.dumps(data, indent=4))
