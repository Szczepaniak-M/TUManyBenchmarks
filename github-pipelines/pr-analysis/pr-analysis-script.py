import os
import sys
import yaml
import boto3
from cron_validator import CronValidator
from pymongo import MongoClient

REQUIRED_CONFIGURATION_FIELDS = {
    'name': str,
    'description': str,
    'directory': str,
    'cron': str,
    'output-type': str,
    'instance-number': int,
}

ALLOWED_OUTPUT_TYPES = [
    'single-node-single-value',
    'single-node-multiple-values',
    'multiple-nodes-single-value',
    'multiple-nodes-multiple-values',
]


def get_allowed_tags():
    db_url = os.getenv('DATABASE_URL')
    client = MongoClient(db_url)
    instances_collection = client['benchmark-data']['instances']
    allowed_tags = instances_collection.aggregate([
        {'$unwind': '$tags'},
        {'$group': {'_id': None, 'distinctTags': {'$addToSet': '$tags'}}},
        {'$project': {'_id': 0, 'distinctTags': 1}}
    ])
    result = allowed_tags.next().get('distinctTags', [])
    client.close()
    return result


def analyze_configuration_file(yaml_file):
    with open(yaml_file, 'r') as f:
        # Read file
        try:
            yaml_data = yaml.safe_load(f)
        except yaml.YAMLError as exc:
            print_stderr(f"Error reading YAML file: {exc}")
            return 1

        if not isinstance(yaml_data, dict):
            print_stderr("YAML file should be a dictionary after reading.")
            return 1

        # Check configuration section
        if 'configuration' not in yaml_data:
            print_stderr("Missing 'configuration' section.")
            return 1
        if not isinstance(yaml_data['configuration'], dict):
            print_stderr("'configuration' section should be of type dictionary.")
            return 1

        # Check required fields
        for key, value_type in REQUIRED_CONFIGURATION_FIELDS.items():
            if key not in yaml_data['configuration']:
                print_stderr(f"Missing '{key}' in 'configuration' section.")
                return 1
            if not isinstance(yaml_data['configuration'][key], value_type):
                print_stderr(f"'{key}' in 'configuration' should be of type {value_type.__name__}.")
                return 1

        # Check CRON
        if CronValidator.parse(yaml_data['configuration']['cron']) is None:
            print_stderr(f"Value of 'cron' in 'configuration' section is not correct CRON expression.")
            return 1

        # Check output type
        if yaml_data['configuration']['output-type'] not in ALLOWED_OUTPUT_TYPES:
            print_stderr(f"Incorrect value of 'output-type' in 'configuration'. "
                         f"Allowed values are: {ALLOWED_OUTPUT_TYPES}")
            return 1

        # Check directory
        directory_from_path = os.path.basename(os.path.dirname(file_path))
        if directory_from_path != yaml_data['configuration']['directory']:
            print_stderr(f"Incorrect value of 'directory' in 'configuration'. "
                         f"It is different than the location of file. "
                         f"Location: '{directory_from_path}' Configuration: "
                         f"'{yaml_data['configuration']['directory']}'")
            return 1

        # Check specified instances
        instance_specified = False
        if 'instance-types' in yaml_data['configuration']:
            if not isinstance(yaml_data['configuration']['instance-types'], list):
                print_stderr(f"'instance-types' in 'configuration' should be of type list.")
                return 1
            client = boto3.client('ec2', region_name='us-east-1')
            aws_instances = client._service_model.shape_for('InstanceType').enum
            for instance_type in yaml_data['configuration']['instance-types']:
                if instance_type not in aws_instances:
                    print_stderr("Incorrect value of EC2 instance in 'instance-types'.")
                    return 1
            instance_specified = True

        # Check specified tags
        if 'instance-tags' in yaml_data['configuration']:
            if not isinstance(yaml_data['configuration']['instance-tags'], list):
                print_stderr(f"'instance-tags' in 'configuration' should be of type list.")
                return 1
            allowed_tags = get_allowed_tags()
            for tag_list in yaml_data['configuration']['instance-tags']:
                if not isinstance(tag_list, list):
                    print_stderr(f"All elements in 'instance-tags' should be of type list.")
                    return 1
                for tag in tag_list:
                    if tag not in allowed_tags:
                        print_stderr(f"Tag '{tag}' is not allowed.")
                        return 1
            instance_specified = True

        # Check whether instances or tags specified
        if not instance_specified:
            print_stderr("Missing 'instance-types' or 'instance-tags' in 'configuration' section.")
            return 1

        # Check nodes section
        if 'nodes' not in yaml_data:
            print_stderr("Missing 'nodes' section.")
            return 1

        if not isinstance(yaml_data['nodes'], list):
            print_stderr("'nodes' section should be of type list.")
            return 1

        # Check if all nodes have IDs
        for node in yaml_data['nodes']:
            if not isinstance(node, dict):
                print_stderr("Each node should be of type dict.")
                return 1
            if 'node-id' not in node:
                print_stderr("Missing 'node-id' in 'node' section.")
                return 1
            if not isinstance(node['node-id'], int):
                print_stderr("'node-id' in 'node' should be of type int.")
                return 1

        # Check if global configuration of benchmark and output command defined
        global_benchmark_command_defined = False
        global_output_command_defined = False
        global_image_defined = False

        sorted_nodes = sorted(yaml_data['nodes'], key=lambda d: d['node-id'])

        if sorted_nodes[0]['node-id'] == 0:
            global_node_configuration = sorted_nodes[0]
            # Check ansible configuration:
            if 'ansible-command' in global_node_configuration \
                    and not isinstance(global_node_configuration['ansible-configuration'], str):
                print_stderr("'ansible-configuration' in 'node' should be of type str.")
                return 1

            # Check benchmark command
            if 'benchmark-command' in global_node_configuration:
                global_benchmark_command_defined = True
                if not isinstance(global_node_configuration['benchmark-command'], str):
                    print_stderr("'benchmark-command' in 'node' should be of type str.")
                    return 1

            # Check output command
            if 'output-command' in global_node_configuration:
                global_output_command_defined = True
                if 'ansible-command' in global_node_configuration \
                        and not isinstance(global_node_configuration['ansible-configuration'], str):
                    print_stderr("'ansible-configuration' in 'node' should be of type str.")
                    return 1

            # Check image
            if 'image' in global_node_configuration:
                global_image_defined = True
                if 'image' in global_node_configuration \
                        and not isinstance(global_node_configuration['image'], str):
                    print_stderr("'image' in 'node' should be of type str.")
                    return 1

            # Check instance type
            if 'instance-type' in global_node_configuration:
                print_stderr("'instance-type' can be only specified for specific node")
                return 1
            sorted_nodes = sorted_nodes[1:]

        # Check nodes
        output_command_defined = False
        for node in sorted_nodes:
            # Check ansible configuration:
            if 'ansible-command' in node and not isinstance(node['ansible-configuration'], str):
                print_stderr("'ansible-configuration' in 'node' should be of type str.")
                return 1

            # Check benchmark command
            if 'benchmark-command' in node:
                if global_benchmark_command_defined:
                    print_stderr(f"Conflicting 'benchmark-command' for node with ID = {node['node-id']}. "
                                 f"'benchmark-command' defined previously globally in node with ID = 0.")
                    return 1
                if not isinstance(node['benchmark-command'], str):
                    print_stderr("'benchmark-command' in 'node' should be of type str.")
                    return 1
            else:
                if not global_benchmark_command_defined:
                    print_stderr(f"Missing 'benchmark-command' for node with ID = {node['node-id']}. "
                                 f"'benchmark-command' was also not defined globally in node with ID = 0.")
                    return 1

            # Check output command
            if 'output-command' in node:
                if global_output_command_defined:
                    print_stderr(f"Conflicting 'output-command' for node with ID = {node['node-id']}. "
                                 f"'output-command' defined previously globally in node with ID = 0.")
                    return 1
                if not isinstance(node['output-command'], str):
                    print_stderr("'output-command' in 'node' should be of type str.")
                    return 1
                output_command_defined = True

            # Check image
            if 'image' in node:
                if global_image_defined:
                    print_stderr(f"Conflicting 'image' for node with ID = {node['node-id']}. "
                                 f"'image' defined previously globally in node with ID = 0.")
                    return 1
                if not isinstance(node['image'], str):
                    print_stderr("'image' in 'node' should be of type str.")
                    return 1

            # Check instance type
            if 'instance-type' in node:
                if not isinstance(node['instance-type'], str):
                    print_stderr(f"'instance-type' in 'node' should be of type str.")
                    return 1
                client = boto3.client('ec2', region_name='us-east-1')
                aws_instances = client._service_model.shape_for('InstanceType').enum
                if node['instance-type'] not in aws_instances:
                    print_stderr(f"Incorrect value of EC2 instance in 'instance-type' for node with ID = {node['node-id']}.")
                    return 1

        # Check whether any output command defined
        if not output_command_defined and not global_output_command_defined:
            print_stderr("Missing 'output-command' for all nodes."
                         "'output-command' was also not defined globally in node with ID = 0.")
            return 1

        return 0


def print_stderr(*args, **kwargs):
    print(*args, file=sys.stderr, **kwargs)


if __name__ == "__main__":
    if len(sys.argv) != 2:
        print_stderr("Usage: python pr-analysis-script.py <file_path>")
        sys.exit(1)

    file_path = sys.argv[1]
    exit_code = analyze_configuration_file(file_path)
    sys.exit(exit_code)
