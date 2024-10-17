# Changelog
All notable changes to this project will be documented in this file.

The format is base on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.50.00.000] - TBD
### Added
- Issue [#31](https://github.com/j3-signalroom/apache_flink-kickstarter/issues/31).
- Issue [#362](https://github.com/j3-signalroom/apache_flink-kickstarter/issues/362).

## [0.42.00.000] - 2024-10-15
### Added
- `How to create a User-Defined Table Function (UDTF) in PyFlink to fetch data from an external source for your Flink App?` markdown.

### Changed
- Tweaked 'README.md`s.
- Tweaked code.

## [0.41.02.000] - 2024-10-12
### Added
- Issue [#101](https://github.com/j3-signalroom/apache_flink-kickstarter/issues/101).
- Issue [#301](https://github.com/j3-signalroom/apache_flink-kickstarter/issues/301).

## [0.41.01.000] - 2024-10-12
### Added
- Tweaked the `README.md`s.

## [0.41.00.000] - 2024-10-12
### Changed
- Issue [#330](https://github.com/j3-signalroom/apache_flink-kickstarter/issues/330).
- Issue [#328](https://github.com/j3-signalroom/apache_flink-kickstarter/issues/328).

## [0.40.00.000] - 2024-10-11
### Changed
- Issue [#325](https://github.com/j3-signalroom/apache_flink-kickstarter/issues/325).

### Fixed
- Resolved issue [#315](https://github.com/j3-signalroom/apache_flink-kickstarter/issues/315) in issue [#325](https://github.com/j3-signalroom/apache_flink-kickstarter/issues/325) resolution.

## [0.35.01.000] - 2024-10-10
### Changed
- Tweaked main `README.md`.

## [0.35.00.000] - 2024-10-10
### Added
- Added the `KNOWNISSUES.md`.

### Changed
- Refactored the Java models.
- Refactored the Dockerfiles.

## [0.30.10.000] - 2024-10-08
### Added
- Sink the data in DataGeneratorApp datastreams to Apache Iceberg tables.

### Changed
- No longer makes a distinction between languages when creating the Docker containers.

## [0.30.02.000] - 2024-10-08
### Added
- Markdowns that explain in detail the `run-terraform-locally.sh` and `run-flink-locally.sh`, respectively.

### Changed
- Tweaked main `README.md`.

### Fixed
- Typo in the comments on the `run-flink-locally.sh` BASH script.

## [0.30.01.000] - 2024-10-07
### Changed
- Tweaked the Java and Python `README.md`s, respectively.
- Automatically zip the Python files via the `run-flink-locally.sh` BASH script.

## [0.30.00.000] - 2024-10-07
### Added
- Converted FlightImporterApp.java to flight_importer_app.py.
- Converted UserStatisticsApp.java to user_statistics_app.py.
- Sink FlightImporterApp and UserStatisticsApp Apache Iceberg tables.
- Use User-Defined Table Function in Python to call AWS Services.

### Changed
- try-wtih-resources

## [0.25.00.000] - 2024-09-08
### Added
- Terraform the AWS S3 bucket that is used for Apache Iceberg file storage.
- Updated the `run-terraform-locally.sh` Bash script handles both the plan/apply and destroy actions.
- Terraform the AWS Secrets Manager Secrets for the Snowflake credentials.
- Terraform Snowflake resources.
- Add Unit Tests.

### Changed
- Split the Terraform configuration into different files based on the jobs of each.
- Updated the `README.md` files.
- Removed Project Nessie and MINIO docker containers from the `docker-compose.yml`
- Upgrade Terraform AWS Provider to `5.66.0`, Terraform Snowflake Provider to `0.95.0`, and Terraform Confluent Provider to `2.1.0`.
- Replaced deprecated Confluent and Snowflake resource/data blocks with updated resource/data blocks.
- Refactor `run-terraform-locally.sh` BASH script to accommodate all the new arguments for Snowflake.
- Use the `service_account_user`variable as the secrets insert value, and to customize the naming of all the resources.

## [0.22.01.000] - 2024-08-18
### Fixed
- Store and use the arguments pass to the `KafkaClientPropertiesLookup` constructor.

## [0.22.00.000] - 2024-08-17
### Added
- Added GitHub Workflow/Actions to execute the Terraform configuration in the cloud.
- Removed all deprecated calls in the creation of the Apache Flink custom Data Source.
- the `terraform.tfvars` is auto-generated in script.
- new Apache Iceberg dependencies.
- commented the code in various modules.

### Changed
- Updated `README.md` files
- Refactored the project file organization.

## [0.21.00.000] - 2024-08-12
### Changed
- Updated main `README.md` file.

## [0.20.00.000] - 2024-08-12
### Added
- Terraform the Confluent Cloud and AWS resources now.
- Created the `force-aws-secrets-destory.sh` Bash script.
- Instructions to the main `README.md` to run Terraform locally.

### Changed
- Updated `README.md` files.
- Updated Kafka Topic names.

### Fixed
- Now pass `AWS_SESSION_TOKEN` to the Apache Flink docker containers.

## [0.10.00.000] - 2024-08-10
### Added
- Created Docker containers for Apache Flink, and load them from the newly added `docker-compose.yml`.
- Created the `run-flink.locally.sh` Bash script to get the AWS credentials and pass them to `docker-compose`.
- the `Glossary.md`

### Changed
- Refactored all the DAGs.
- Refactored the `README.md` files.
- Now refer to the Flink Jobs as Flink Apps.

## [0.02.00.000] - 2024-08-05
### Changed
- Updated main `README.md`.
- Fixed typo(s) in Java build.
- Fixed typo(s) in Java built DAGs.

## [0.01.00.000] - 2024-08-04
### Added
- First release.