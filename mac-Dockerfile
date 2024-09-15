# Base image from https://nightlies.apache.org/flink/flink-docs-release-1.20/docs/deployment/resource-providers/standalone/docker/
FROM arm64v8/flink:1.20.0-scala_2.12-java17

# Install python3 and pip3
# Install libbz2-dev, a development package for the bzip2 library (libbz2), which is used for compressing and decompressing data using the Burrows-Wheeler block-sorting text compression algorithm and Huffman coding.
# Install libffi-dev, a development package for the libffi library, which provides a portable, high-level programming interface to various calling conventions.
# Install libssl-dev, a development package for the OpenSSL library, which is a general-purpose cryptography library that provides an open-source implementation of the Secure Sockets Layer (SSL) and Transport Layer Security (TLS) protocols.
# Install zlib1g-dev, a development package for the zlib library, which is a software library used for data compression.
# Install openjdk-17-jdk-headless, a headless version of the OpenJDK 17 Java Development Kit (JDK) that does not include any graphical user interface (GUI) libraries or tools.
# ***The above packages are required for building and installing PyFlink.***
RUN apt-get update -y && apt-get install -y python3 python3-pip python3-dev openjdk-17-jdk-headless libbz2-dev libffi-dev libssl-dev zlib1g-dev && \
    rm -rf /var/lib/apt/lists/* && \
    ln -s /usr/bin/python3 /usr/bin/python
    
# Set JAVA_HOME
ENV JAVA_HOME=/usr/lib/jvm/java-17-openjdk-arm64

# ---
# Download JARs to FLINK_HOME/lib to make them available to Flink
# ---
# --- Iceberg Flink Library
RUN curl -L https://repo1.maven.org/maven2/org/apache/iceberg/iceberg-flink-runtime-1.19/1.6.1/iceberg-flink-runtime-1.19-1.6.1.jar -o /opt/flink/lib/iceberg-flink-runtime-1.19-1.6.1.jar

# --- Hive Flink Library
RUN curl -L https://repo1.maven.org/maven2/org/apache/flink/flink-sql-connector-hive-3.1.3_2.12/1.20.0/flink-sql-connector-hive-3.1.3_2.12-1.20.0.jar -o /opt/flink/lib/flink-sql-connector-hive-3.1.3_2.12-1.20.0.jar

# --- Hadoop Common Classes
RUN curl -L https://repo1.maven.org/maven2/org/apache/hadoop/hadoop-common/3.4.0/hadoop-common-3.4.0.jar -o /opt/flink/lib/hadoop-common-3.4.0.jar

# --- Hadoop AWS Classes
RUN curl -L https://repo.maven.apache.org/maven2/org/apache/flink/flink-shaded-hadoop-2-uber/2.8.3-10.0/flink-shaded-hadoop-2-uber-2.8.3-10.0.jar -o /opt/flink/lib/flink-shaded-hadoop-2-uber-2.8.3-10.0.jar 

# --- AWS Bundled Classes
RUN curl -L https://repo1.maven.org/maven2/software/amazon/awssdk/bundle/2.27.9/bundle-2.27.9.jar -o /opt/flink/lib/bundle-2.27.9.jar

# Install Nano to edit files
RUN apt update && apt install -y nano

# Install PyFlink
RUN pip3 install --upgrade pip
RUN pip3 install pipenv
RUN pip3 install "grpcio-tools>=1.29.0,<=1.50.0"
RUN pip3 install setuptools>=37.0.0
RUN pip3 install apache-flink==1.20.0
RUN pip3 install "pyiceberg[s3fs,hive,pandas]"
RUN pip3 install boto3
RUN pip3 install botocore
RUN pip3 install confluent-kafka==2.5.3
RUN pip3 install s3fs
RUN pip3 install utils

# Set the entrypoint to Flink's entrypoint script
ENTRYPOINT ["/docker-entrypoint.sh"]
