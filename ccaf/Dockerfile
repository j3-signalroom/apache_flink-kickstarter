# Use a Python base image
FROM python:3.11.11-bookworm

# Install OpenJDK
RUN apt-get update && apt-get install -y openjdk-17-jdk

# Set JAVA_HOME
ENV JAVA_HOME=/usr/lib/jvm/java-17-openjdk-arm64
 
# Container metadata
LABEL maintainer=j3@thej3.com \
      description="Apache Flink Kickstarter Project, showcasing Confluent Clound for Apache Flink"
 
# Set the working directory
WORKDIR /app
 
# Install Poetry
RUN pip install --upgrade pip && pip install poetry
 
# Copy only the necessary file to optimize the build
COPY pyproject.toml poetry.lock ./
 
# Install dependencies
RUN poetry config virtualenvs.create false && poetry install --no-root --no-interaction
 
# Copy the rest of the application code
COPY . .

# Set the environment variables for the AWS credentials
RUN --mount=type=secret,id=AWS_ACCESS_KEY_ID,env=AWS_ACCESS_KEY_ID \
    --mount=type=secret,id=AWS_SECRET_ACCESS_KEY,env=AWS_SECRET_ACCESS_KEY \
    --mount=type=secret,id=AWS_SESSION_TOKEN,env=AWS_SESSION_TOKEN

# Fetch deployment arguments, and default values if none were passed
ARG AWS_REGION
ENV AWS_REGION=${AWS_REGION:-us-east-1}
ARG CATALOG_NAME
ENV CATALOG_NAME=${CATALOG_NAME:-flink_kickstarter}
ARG DATABASE_NAME
ENV DATABASE_NAME=${DATABASE_NAME:-flink_kickstarter}
 
# Create the entrypoint.sh script
# --- This identifies the default executable for the container.  The script will run a
# --- Python app, and not allow the host to change its intended purpose.
RUN if [[ -z "$AWS_ACCESS_KEY_ID" ]] ; \
then \
printf "#!/usr/bin/env sh \
\ncd /app \
\npoetry install \
\npoetry run avro_flight_consolidator_ccaf_app --catalog_name '${CATALOG_NAME}' --database-name '${DATABASE_NAME}' --aws-region '${AWS_REGION}' \
\n" > scripts/entrypoint.sh ; \
else \
printf "#!/usr/bin/env sh \
\nexport AWS_ACCESS_KEY_ID=${AWS_ACCESS_KEY_ID} \
\nexport AWS_SECRET_ACCESS_KEY=${AWS_SECRET_ACCESS_KEY} \
\nexport AWS_SESSION_TOKEN=${AWS_SESSION_TOKEN} \
\nexport AWS_REGION=${AWS_REGION} \
\ncd /app \
\npoetry install \
\npoetry run avro_flight_consolidator_ccaf_app --catalog_name \"flink_kickstarter\" --database-name \"flink_kickstarter\" --aws-region \"us-east-1\" \
\n" > scripts/entrypoint.sh ; \
fi
 
RUN ["chmod", "+x", "scripts/entrypoint.sh"]
 
# AWS ECS Fargate requires the entrypoint.sh script to be named docker-entrypoint.sh and
# located in the /usr/local/bin folder of the container
RUN cp scripts/entrypoint.sh /usr/local/bin/docker-entrypoint.sh
RUN ["chmod", "+x", "/usr/local/bin/docker-entrypoint.sh"]
 
# Create User Group with newly created Rootless System Account as a member.  The new member
# doesn't have a shell (i.e., home directory)
RUN groupadd -r app_runner && useradd -r -s /bin/false -g app_runner app_runner
 
# Giving the newly created users permission to the application folder ONLY
RUN chown -R app_runner:app_runner /app
 
# Change the Home directory of the app_runner user
RUN usermod -d /app app_runner
 
# Start the Python application with the newly created user
USER app_runner
ENTRYPOINT ["scripts/entrypoint.sh"]