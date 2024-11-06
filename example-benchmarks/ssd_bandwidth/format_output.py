import sys
import json

def parse_fio(file_path):
    benchmark_type = file_path[:-5].split("-")[1]

    with open(file_path, "r") as f:
        fio_json = json.load(f)

    benchmark_type_json = benchmark_type.replace("rand", "")
    target_benchmark = fio_json["jobs"][0][benchmark_type_json]
    # flatten latency dict -> e.g., "lat_ns": {"N": 42, ...} -> "lat_ns_N": 42  
    latencies = {f"lat_ns_{key}_{benchmark_type}": value for key, value in target_benchmark["lat_ns"].items()}
    # remove nested json structures
    cleaned = {f"{key}_{benchmark_type}": value for key, value in target_benchmark.items() if not type(value) in [list, dict]}
    return cleaned | latencies

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Usage: python3 format_output.py <input_file_1> <input_file_2> ...")
        sys.exit(1)

    merged_dict = dict()
    for i in range(1, len(sys.argv)):
        merged_dict |= parse_fio(sys.argv[i])

    print(json.dumps(merged_dict, indent=4))
