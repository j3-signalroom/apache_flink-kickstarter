# Changelog
All notable changes to this project will be documented in this file.

The format is base on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.30.00.000] - TBD
## Added
- Terraform the AWS S3 bucket that is used for Apache Iceberg file storage
- Updated the `run-terraform-locally.sh` Bash script handles both the plan\apply and destroy actions
- 

### Changed
- Split the Terraform configuration into different files based on the jobs of each
- Updated the `README.md` files
- Removed Project Nessie and MINIO docker containers from the `docker-compose.yml`

## [0.22.01.000] - 2024-08-18
### Fixed
- Store and use the arguments pass to the `KafkaClientPropertiesLookup` constructor 

## [0.22.00.000] - 2024-08-17
### Added
- Added GitHub Workflow/Actions to execute the Terraform configuration in the cloud
- Removed all deprecated calls in the creation of the Apache Flink custom Data Source
- the `terraform.tfvars` is auto-generated in script
- new Apache Iceberg dependencies
- commented the code in various modules

### Changed
- Updated `README.md` files
- Refactored the project file organization

## [0.21.00.000] - 2024-08-12
### Changed
- Updated main `README.md` file

## [0.20.00.000] - 2024-08-12
### Added
- Terraform the Confluent Cloud and AWS resources now
- Created the `force-aws-secrets-destory.sh` Bash script
- Instructions to the main `README.md` to run Terraform locally

### Changed
- Updated `README.md` files
- Updated Kafka Topic names

### Fixed
- Now pass `AWS_SESSION_TOKEN` to the Apache Flink docker containers

## [0.10.00.000] - 2024-08-10
### Added
- Created Docker containers for Apache Flink, and load them from the newly added `docker-compose.yml`
- Created the `run-flink.locally.sh` Bash script to get the AWS credentials and pass them to `docker-compose`
- the `Glossary.md`

### Changed
- Refactored all the DAGs
- Refactored the `README.md` files
- Now refer to the Flink Jobs as Flink Apps

## [0.02.00.000] - 2024-08-05
### Changed
- Updated main `README.md`
- Fixed typo(s) in Java build
- Fixed typo(s) in Java built DAGs

## [0.01.00.000] - 2024-08-04
### Added
- First release