import json
import sys
import math
from collections import defaultdict


def parse_file_to_json(file_path):
    with open(file_path, 'r') as file:
        lines = file.readlines()

    results = defaultdict(list)
    data = {
        "input_size_log10": [],
        "latency": [],
    }
    for i in range(0, len(lines)):
        size, time = lines[i].split(",")
        results[size].append(float(time))
    for key, values in results.items():
        size = math.log10(int(key) / 1024 / 1024)
        time = sum(values) / len(values)
        data["input_size_log10"].append(size)
        data["latency"].append(time)

    json_output = json.dumps(data, indent=4)
    print(json_output)


if __name__ == "__main__":
    if len(sys.argv) != 2:
        print("Usage: python format_output.py <file_path>")
    else:
        file_path = sys.argv[1]
        parse_file_to_json(file_path)
