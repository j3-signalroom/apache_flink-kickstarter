# Use a Python base image
FROM python:3.11.11-bookworm

# Container metadata
LABEL maintainer=j3@thej3.com \
      description="Apache Flink Kickstarter Project, showcasing Confluent Clound for Apache Flink"

# Install OpenJDK
RUN apt-get update && apt-get install -y openjdk-17-jdk

# Set JAVA_HOME
ENV JAVA_HOME=/usr/lib/jvm/java-17-openjdk-arm64

# Download UV
COPY --from=ghcr.io/astral-sh/uv:latest /uv /usr/local/bin/uv
  
# Set the working directory
WORKDIR /ccaf
 
# Copy the rest of the application code
COPY . .

RUN uv sync --frozen

# Fetch deployment arguments, and default values if none were passed
ARG AWS_REGION
ENV AWS_REGION=${AWS_REGION:-us-east-1}
ARG AWS_ACCESS_KEY_ID
ENV AWS_ACCESS_KEY_ID=${AWS_ACCESS_KEY_ID}
ARG AWS_SECRET_ACCESS_KEY
ENV AWS_SECRET_ACCESS_KEY=${AWS_SECRET_ACCESS_KEY}
ARG AWS_SESSION_TOKEN
ENV AWS_SESSION_TOKEN=${AWS_SESSION_TOKEN}
ARG CATALOG_NAME
ENV CATALOG_NAME=${CATALOG_NAME:-flink_kickstarter}
ARG DATABASE_NAME
ENV DATABASE_NAME=${DATABASE_NAME:-flink_kickstarter}
 
# Create the entrypoint.sh script
# --- This identifies the default executable for the container.  The script will run a
# --- Python app, and not allow the host to change its intended purpose.
RUN if [[ -z "$AWS_ACCESS_KEY_ID" ]] ; \
then \
printf "#!/bin/bash \
\nuv run flight_consolidator --catalog-name '${CATALOG_NAME}' --database-name '${DATABASE_NAME}' --aws-region '${AWS_REGION}' \
\n" > scripts/entrypoint.sh ; \
else \
printf "#!/bin/bash \
\nexport AWS_ACCESS_KEY_ID=${AWS_ACCESS_KEY_ID} \
\nexport AWS_SECRET_ACCESS_KEY=${AWS_SECRET_ACCESS_KEY} \
\nexport AWS_SESSION_TOKEN=${AWS_SESSION_TOKEN} \
\nexport AWS_REGION=${AWS_REGION} \
\nuv run flight_consolidator --catalog-name '${CATALOG_NAME}' --database-name '${DATABASE_NAME}' --aws-region '${AWS_REGION}' \
\n" > scripts/entrypoint.sh ; \
fi
 
RUN ["chmod", "+x", "scripts/entrypoint.sh"]
 
# AWS ECS Fargate requires the entrypoint.sh script to be named docker-entrypoint.sh and
# located in the /usr/local/bin folder of the container
RUN cp scripts/entrypoint.sh /usr/local/bin/docker-entrypoint.sh
RUN ["chmod", "+x", "/usr/local/bin/docker-entrypoint.sh"]
 

ENTRYPOINT ["scripts/entrypoint.sh"]