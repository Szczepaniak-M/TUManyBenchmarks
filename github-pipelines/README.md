# # TUManyBenchmarks - GitHub Actions Pipelines
## About
The directory contains files used by GitHub Actions pipelines 
to validate and upload new benchmarks to the MongoDB.
All files from this directory should be placed in directory `.github/workflows/`.
The role of each subdirectory:
- `pr-analysis` check mistakes in the structure and values of `configuration.yml`
- `benchmark-upload` upload the configuration after commit to `main` branch
- `example` directory contains example of correct configuration file

## Technology:
The project was created using following technologies:
- GitHub Actions - CI/CD tool
- Python - scripts executing the key logic
- boto3 - communication with AWS
- pymongo - communication with MongoDB