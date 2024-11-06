# TUManyBenchmarks - Benchmark repository
## About
The repository store benchmarks executed by TUManyBenchmarks service.
Each directory contains separate benchmark, which can be reproduced locally.

## How to add new benchmark?
1. Fork a repository
2. Add benchmark in a new directory
3. Add Ansible playbook to configure benchmark environment
4. Configure benchmark execution in the `configuration.yml` file
5. Create merge request
6. The GitHub action pipeline validates the `configuration.yml` file
7. The repository maintainer accepts new benchmark
8. Benchmark is automatically and regularly executed