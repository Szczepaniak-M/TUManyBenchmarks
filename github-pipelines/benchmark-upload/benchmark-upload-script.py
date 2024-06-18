import os
import sys
import yaml
from pymongo import MongoClient


def load_yaml_file(file_path):
    with open(file_path, 'r') as file:
        try:
            data = yaml.safe_load(file)
            data_with_camel_case = convert_keys_to_camel_case(data)
            return data_with_camel_case
        except yaml.YAMLError as exc:
            print(f"Error reading YAML file: {exc}")
            return None


def convert_keys_to_camel_case(input_dict):
    new_dict = {}
    for key, value in input_dict.items():
        new_key = dash_to_camel_case(key)
        # Recursively convert nested dictionaries
        if isinstance(value, dict):
            new_dict[new_key] = convert_keys_to_camel_case(value)
        elif isinstance(value, list):
            new_list = []
            for element in value:
                if isinstance(element, dict):
                    # Recursively convert nested dictionaries in a list
                    new_list.append(convert_keys_to_camel_case(element))
                else:
                    new_list.append(element)
            new_dict[new_key] = new_list
        else:
            new_dict[new_key] = value
    return new_dict


def dash_to_camel_case(dash_str):
    parts = dash_str.split('-')
    return parts[0] + ''.join(word.capitalize() for word in parts[1:])


def upload_to_mongodb(document, client):
    collection = client['benchmark-data']['benchmarks']
    directory = document.get('configuration', {}).get('directory')
    if directory:
        try:
            collection.update_one(
                {'configuration.directory': directory},
                {'$set': document},
                upsert=True
            )
        except Exception as e:
            print_stderr(f"Error updating/inserting document {document}: {e}")
            return 1
    else:
        print_stderr(f"Document does not have 'configuration.directory'. Skipping insertion for {document}")
        return 1

    return 0


def print_stderr(*args, **kwargs):
    print(*args, file=sys.stderr, **kwargs)


if __name__ == "__main__":
    if len(sys.argv) != 2:
        print_stderr("Usage: python benchmark-upload-script.py <file_path>")
        sys.exit(1)

    yml_path = sys.argv[1]
    configuration = load_yaml_file(yml_path)

    db_url = os.getenv('DATABASE_URL')
    mongo_client = MongoClient(db_url)
    exit_code = upload_to_mongodb(configuration, mongo_client)
    mongo_client.close()

    sys.exit(exit_code)
