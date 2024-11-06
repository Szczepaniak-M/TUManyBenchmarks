import re
import json
import sys


def parse_sockperf_output(file_path):
    parsed_data = {}
    avg_rtt_pattern = r'avg-rtt=(\d+\.\d+)'
    std_dev_pattern = r'std-dev=(\d+\.\d+)'
    min_pattern = r'<MIN> observation =\s+(\d+\.\d+)'
    max_pattern = r'<MAX> observation =\s+(\d+\.\d+)'
    percentile_pattern = r'percentile (\d+\.\d+) =\s+(\d+\.\d+)'

    with open(file_path, 'r') as file:
        content = file.read()

        avg_rtt_match = re.search(avg_rtt_pattern, content)
        if avg_rtt_match:
            parsed_data['average'] = float(avg_rtt_match.group(1))

        std_dev_match = re.search(std_dev_pattern, content)
        if std_dev_match:
            parsed_data['average_plus_std_dev'] = parsed_data['average'] + float(std_dev_match.group(1))
            parsed_data['average_minus_std_dev'] = parsed_data['average'] - float(std_dev_match.group(1))

        min_match = re.search(min_pattern, content)
        if min_match:
            parsed_data['minimum'] = float(min_match.group(1))

        max_match = re.search(max_pattern, content)
        if max_match:
            parsed_data['maximum'] = float(max_match.group(1))

        for percentile_match in re.finditer(percentile_pattern, content):
            percentile = percentile_match.group(1).replace('.', '_')
            value = float(percentile_match.group(2))
            parsed_data[f'percentile_{percentile}'] = value

    print(json.dumps(parsed_data, indent=4))


if __name__ == "__main__":
    if len(sys.argv) != 2:
        print("Usage: python parse_results.py <results.txt>")
        sys.exit(1)

    file_path = sys.argv[1]
    parse_sockperf_output(file_path)
