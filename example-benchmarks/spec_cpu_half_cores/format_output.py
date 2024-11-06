import re
import csv
import sys
import json

def parse_spec_cpu_output(input_file):
    with open(input_file, 'r') as f:
        data = f.read()

    lines = data.strip().split("\n")

    parsed_data = {}
    parse = False
    for line in lines:
        if line == "\"Full Results Table\"":
            parse = True
        elif line == "\"Selected Results Table\"":
            parse = False

        if parse and re.match(r'^\d{3}\.[a-z0-9]+.+', line):
            fields = next(csv.reader([line]))
            name = fields[0].replace(".", "_")
            base_run_time = fields[2]
            base_rate = fields[3]
            base_selected = fields[4]
            peak_run_time = fields[7]
            peak_rate = fields[8]
            peak_selected = fields[9]
            if base_selected == '1':
                parsed_data[f'{name}_base_selected_time'] = float(base_run_time)
                parsed_data[f'{name}_base_selected_ratio'] = float(base_rate)
            else:
                parsed_data[f'{name}_base_not_selected_time'] = float(base_run_time)
                parsed_data[f'{name}_base_not_selected_ratio'] = float(base_rate)

            if peak_selected == '1':
                parsed_data[f'{name}_peak_selected_time'] = float(peak_run_time)
                parsed_data[f'{name}_peak_selected_ratio'] = float(peak_rate)
            else:
                parsed_data[f'{name}_peak_not_selected_time'] = float(peak_run_time)
                parsed_data[f'{name}_peak_not_selected_ratio'] = float(peak_rate)

    spec_base = re.search(r'SPECrate2017_int_base,([\d.]+)', data)
    spec_peak = re.search(r'SPECrate2017_int_peak,([\d.]+)', data)

    if spec_base:
        parsed_data['SPECrate2017_int_base'] = float(spec_base.group(1))
    if spec_peak:
        parsed_data['SPECrate2017_int_peak'] = float(spec_peak.group(1))

    print(json.dumps(parsed_data, indent=4))

if __name__ == "__main__":
    if len(sys.argv) != 2:
        print("Usage: python format_output.py <results.txt>")
        sys.exit(1)

    file_path = sys.argv[1]
    parse_spec_cpu_output(file_path)
