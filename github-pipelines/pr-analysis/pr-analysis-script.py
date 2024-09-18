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
    'instance-number': int,
}

ALLOWED_PLOT_TYPES = ['scatter', 'line']


def analyze_configuration_file(yaml_file):
    try:
        yaml_data = read_yaml(yaml_file)
        check_configuration_section(yaml_data)
        check_required_fields(yaml_data)
        check_directory(yaml_data)
        check_cron(yaml_data)
        check_instance_count(yaml_data)
        instance_specified = check_specified_instances(yaml_data)
        tag_specified = check_specified_tags(yaml_data)
        check_if_any_instance_specified(instance_specified, tag_specified)
        check_nodes_section(yaml_data)
        check_if_all_nodes_have_id(yaml_data)
        nodes = sorted(yaml_data['nodes'], key=lambda d: d['node-id'])
        default_node, nodes = get_default_node_configuration(nodes)
        output_defined = False
        for node in nodes:
            check_ansible_configuration(default_node, node)
            check_benchmark_command(default_node, node)
            output_defined = check_output_command(default_node, node) or output_defined
            check_image(node)
            check_instance_type(node)
        check_output_defined(output_defined)
        check_plots_section(yaml_data)
        for plot in yaml_data['plots']:
            check_plot(plot)
        check_series_other(yaml_data)
    except ValueError as e:
        print_stderr(e)
        return 1
    return 0


def read_yaml(yaml_file):
    with open(yaml_file, 'r') as f:
        try:
            yaml_data = yaml.safe_load(f)
        except yaml.YAMLError as exc:
            raise ValueError(f"Error reading YAML file: {exc}")
        if not isinstance(yaml_data, dict):
            raise ValueError("YAML file should be a dictionary after reading.")
        return yaml_data


def check_configuration_section(yaml_data):
    if 'configuration' not in yaml_data:
        raise ValueError("Missing 'configuration' section.")
    if not isinstance(yaml_data['configuration'], dict):
        raise ValueError("'configuration' section should be of type dictionary.")


def check_required_fields(yaml_data):
    for key, value_type in REQUIRED_CONFIGURATION_FIELDS.items():
        if key not in yaml_data['configuration']:
            raise ValueError(f"Missing '{key}' in 'configuration' section.")
        if not isinstance(yaml_data['configuration'][key], value_type):
            raise ValueError(f"'{key}' in 'configuration' should be of type {value_type.__name__}.")


def check_directory(yaml_data):
    directory_from_path = os.path.basename(os.path.dirname(file_path))
    if directory_from_path != yaml_data['configuration']['directory']:
        raise ValueError(f"Incorrect value of 'directory' in 'configuration'. "
                         f"It is different than the location of file. "
                         f"Location: '{directory_from_path}' Configuration: "
                         f"'{yaml_data['configuration']['directory']}'")


def check_cron(yaml_data):
    if CronValidator.parse(yaml_data['configuration']['cron']) is None:
        raise ValueError(f"Value of 'cron' in 'configuration' section is not correct CRON expression.")


def check_instance_count(yaml_data):
    if yaml_data['configuration']['instance-number'] < 1:
        raise ValueError('Instance number must be at least 1')


def check_specified_instances(yaml_data):
    instance_specified = False
    if 'instance-types' in yaml_data['configuration']:
        if not isinstance(yaml_data['configuration']['instance-types'], list):
            raise ValueError(f"'instance-types' in 'configuration' should be of type list.")
        client = boto3.client('ec2', region_name='us-east-1')
        aws_instances = client._service_model.shape_for('InstanceType').enum
        for instance_type in yaml_data['configuration']['instance-types']:
            if instance_type not in aws_instances:
                raise ValueError("Incorrect value of EC2 instance in 'instance-types'.")
        instance_specified = True
    return instance_specified


def check_specified_tags(yaml_data):
    tag_specified = False
    if 'instance-tags' in yaml_data['configuration']:
        if not isinstance(yaml_data['configuration']['instance-tags'], list):
            raise ValueError(f"'instance-tags' in 'configuration' should be of type list.")
        allowed_tags = get_allowed_tags()
        for tag_list in yaml_data['configuration']['instance-tags']:
            if not isinstance(tag_list, list):
                raise ValueError(f"All elements in 'instance-tags' should be of type list.")
            for tag in tag_list:
                if tag not in allowed_tags:
                    raise ValueError(f"Tag '{tag}' is not allowed.")
        tag_specified = True
    return tag_specified


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


def check_if_any_instance_specified(instance_specified, tag_specified):
    if not instance_specified and not tag_specified:
        raise ValueError("Missing 'instance-types' or 'instance-tags' in 'configuration' section.")


def check_nodes_section(yaml_data):
    if 'nodes' not in yaml_data:
        raise ValueError("Missing 'nodes' section.")
    if not isinstance(yaml_data['nodes'], list):
        raise ValueError("'nodes' section should be of type list.")


def check_if_all_nodes_have_id(yaml_data):
    for node in yaml_data['nodes']:
        if not isinstance(node, dict):
            raise ValueError("Each node should be of type dict.")
        if 'node-id' not in node:
            raise ValueError("Missing 'node-id' in 'node' section.")
        if not isinstance(node['node-id'], int):
            raise ValueError("'node-id' in 'node' should be of type int.")


def get_default_node_configuration(nodes):
    default_node = {}
    if nodes[0]['node-id'] == 0:
        # Remove default node from the list
        default_node = nodes[0]
        nodes = nodes[1:]

        # Check ansible configuration:
        if 'ansible-configuration' in default_node \
                and not isinstance(default_node['ansible-configuration'], str):
            raise ValueError("'ansible-configuration' in 'node' should be of type str.")

        # Check benchmark command
        if 'benchmark-command' in default_node \
                and not isinstance(default_node['benchmark-command'], str):
            raise ValueError("'benchmark-command' in 'node' should be of type str.")

        # Check output command
        if 'output-command' in default_node \
                and not isinstance(default_node['output-command'], str):
            raise ValueError("'output-command' in 'node' should be of type str.")

        # Check image
        if 'image' in default_node \
                and not isinstance(default_node['image'], str):
            raise ValueError("'image' in 'node' should be of type str.")

        # Check instance type
        if 'instance-type' in default_node:
            raise ValueError("'instance-type' can be only specified for specific node")

        # Check output
        if 'output' in default_node:
            raise ValueError("'output' can be only specified for specific node")

    return default_node, nodes


def check_ansible_configuration(default_node, node):
    if 'ansible-configuration' in node:
        if not isinstance(node['ansible-configuration'], str):
            raise ValueError("'ansible-configuration' in 'node' should be of type str.")
    elif 'ansible-configuration' not in default_node:
        raise ValueError(f"Missing 'ansible-configuration' for node with ID = {node['node-id']}. "
                         f"'benchmark-command' was also not defined globally in node with ID = 0.")


def check_benchmark_command(default_node, node):
    if 'benchmark-command' in node:
        if not isinstance(node['benchmark-command'], str):
            raise ValueError("'benchmark-command' in 'node' should be of type str.")
    elif 'benchmark-command' not in default_node:
        raise ValueError(f"Missing 'benchmark-command' for node with ID = {node['node-id']}. "
                         f"'benchmark-command' was also not defined globally in node with ID = 0.")


def check_output_command(default_node, node):
    output_command_defined = False
    if 'output-command' in node:
        if not isinstance(node['output-command'], str):
            raise ValueError("'output-command' in 'node' should be of type str.")
        output_command_defined = True
    elif 'output-command' in default_node:
        output_command_defined = True
    return output_command_defined


def check_image(node):
    if 'image' in node and not isinstance(node['image'], str):
        raise ValueError("'image' in 'node' should be of type str.")


def check_instance_type(node):
    if 'instance-type' in node:
        if not isinstance(node['instance-type'], str):
            raise ValueError(f"'instance-type' in 'node' should be of type str.")
        client = boto3.client('ec2', region_name='us-east-1')
        aws_instances = client._service_model.shape_for('InstanceType').enum
        if node['instance-type'] not in aws_instances:
            raise ValueError(
                f"Incorrect value of EC2 instance in 'instance-type' for node with ID = {node['node-id']}.")


def check_output_defined(output_names):
    if not output_names:
        raise ValueError('No output defined')


def check_plots_section(yaml_data):
    if 'plots' not in yaml_data:
        raise ValueError("Missing 'plots' section.")
    if not isinstance(yaml_data['plots'], list):
        raise ValueError("'plots' section should be of type list.")


def check_plot(plot):
    plot_type = check_plot_type(plot)
    check_title(plot)
    check_xaxis(plot, plot_type)
    check_yaxis(plot)
    check_series(plot, plot_type)


def check_plot_type(plot):
    if 'type' not in plot:
        raise ValueError("Missing 'type' in 'plots' element.")
    if plot['type'] not in ALLOWED_PLOT_TYPES:
        raise ValueError(f"'type' in 'plots' should be equal to one value from {ALLOWED_PLOT_TYPES}")
    return plot['type']


def check_title(plot):
    check_str('title', plot, 'plots')


def check_xaxis(plot, plot_type):
    if plot_type == 'scatter':
        if 'xaxis' in plot:
            raise ValueError(
                "Parameter 'xaxis' not allowed for 'scatter' type. X-axis is always an execution timestamp")
    elif plot_type == 'line':
        check_str('xaxis', plot, 'plots')


def check_yaxis(plot):
    check_str('yaxis', plot, 'plots')
    check_log_scale(plot)


def check_series(plot, plot_type):
    if 'series' not in plot:
        raise ValueError("Missing 'series' in 'plots' section.")
    if not isinstance(plot['series'], list):
        raise ValueError("'series' should be of type list.")
    for series in plot['series']:
        check_str('y', series, 'series')
        check_str('legend', series, 'series')
        if plot_type == 'scatter':
            if 'x' in series:
                raise ValueError("Parameter 'x' not allowed for 'scatter' type. "
                                 "X-axis is always an execution timestamp")
        elif plot_type == 'line':
            check_str('x', series, 'series')

def check_series_other(yaml_data):
    if 'series-other' in yaml_data:
        if not isinstance(yaml_data['series-other'], list):
            raise ValueError("'series-other' should be of type list.")
        for series in yaml_data['series-other']:
            if not isinstance(series, str):
                raise ValueError(f"'{series}' in 'series-other' should be of type str.")

def check_str(key, dictionary, dictionary_name):
    if key not in dictionary:
        raise ValueError(f"Missing '{key}' in '{dictionary_name}' section.")
    if not isinstance(dictionary[key], str):
        raise ValueError(f"'{key}' in '{dictionary_name}' should be of type str.")

def check_log_scale(dictionary):
    if 'yaxis-log' in dictionary:
        if not isinstance(dictionary['yaxis-log'], int) and dictionary['yaxis-log'] != 'e':
            raise ValueError(f"'yaxis-log' in 'plots' should be of type int or equals 'e'.")


def print_stderr(*args, **kwargs):
    print(*args, file=sys.stderr, **kwargs)


if __name__ == "__main__":
    if len(sys.argv) != 2:
        print_stderr("Usage: python pr-analysis-script.py <file_path>")
        sys.exit(1)

    file_path = sys.argv[1]
    exit_code = analyze_configuration_file(file_path)
    sys.exit(exit_code)
