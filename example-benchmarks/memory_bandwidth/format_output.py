import csv
import json
import sys
from collections import defaultdict

def parse_file_to_json(csv_file):
    bw_sum_count = defaultdict(lambda: {'sum': 0, 'count': 0})

    with open(csv_file, 'r') as file:
        reader = csv.DictReader(file)
        for row in reader:
            threads = int(row['threads'])
            bw = float(row['bw'])

            bw_sum_count[threads]['sum'] += bw
            bw_sum_count[threads]['count'] += 1

    threads_list = []
    bw_list = []
    for threads, values in bw_sum_count.items():
        avg_bw = values['sum'] / values['count']
        threads_list.append(threads)
        bw_list.append(avg_bw)


    data = {
        "thread_counts": threads_list,
        "memory_bandwidth": bw_list,
    }
    json_output = json.dumps(data, indent=4)
    print(json_output)

if __name__ == "__main__":
    if len(sys.argv) != 2:
        print("Usage: python format_output.py <file_path>")
    else:
        file_path = sys.argv[1]
        parse_file_to_json(file_path)
