plugins {
    application
    id("com.gradleup.shadow") version "9.2.2"
}

// --- Read the Gradle properties file
val appMainClass: String? by project

repositories {
    mavenCentral()
    maven {
        url = uri("https://packages.confluent.io/maven/")
    }
}

// --- Dependency version numbers
val flinkVersion: String = "2.1.0"
val hadoopVersion: String = "3.4.2"
val kafkaVersion: String = "4.0.1"
val awssdkVersion: String = "2.35.10"
var icebergVersion: String = "1.10.0"
var confluentKafkaVersion: String = "8.0.0"
var jacksonVersion: String = "2.18.1"

dependencies {
    implementation("org.slf4j:slf4j-api:2.0.17")
    runtimeOnly("ch.qos.logback:logback-classic:1.5.12")
    
    implementation("org.apache.kafka:kafka-clients:${kafkaVersion}")
    implementation("org.apache.avro:avro:1.12.1")
    implementation("io.confluent:kafka-avro-serializer:${confluentKafkaVersion}")
    implementation("io.confluent:kafka-schema-registry-client:${confluentKafkaVersion}")
    implementation("tech.allegro.schema.json2avro:converter:0.3.0")
    implementation("com.fasterxml.jackson.core:jackson-databind:${jacksonVersion}")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-avro:${jacksonVersion}")
    implementation("org.apache.hadoop:hadoop-common:${hadoopVersion}")
    implementation("org.json:json:20250517")

    // --- Flink dependencies
    compileOnly("org.apache.flink:flink-core:${flinkVersion}")
    implementation("org.apache.flink:flink-runtime:${flinkVersion}")
    compileOnly("org.apache.flink:flink-streaming-java:${flinkVersion}")
    compileOnly("org.apache.flink:flink-table-common:${flinkVersion}")
    compileOnly("org.apache.flink:flink-table-runtime:${flinkVersion}")
    compileOnly("org.apache.flink:flink-table-api-java-bridge:${flinkVersion}")
    implementation("org.apache.flink:flink-clients:${flinkVersion}")
    compileOnly("org.apache.flink:flink-connector-base:${flinkVersion}")
    implementation("org.apache.flink:flink-connector-kafka:4.0.1-2.0")
    implementation("org.apache.flink:flink-connector-datagen:${flinkVersion}")
    implementation("org.apache.flink:flink-avro:${flinkVersion}")
    implementation("org.apache.flink:flink-avro-confluent-registry:${flinkVersion}")
    implementation("org.apache.flink:flink-json:${flinkVersion}")

    // --- AWS SDK v2 dependencies
    implementation("software.amazon.awssdk:sdk-core:${awssdkVersion}")
    implementation("software.amazon.awssdk:secretsmanager:${awssdkVersion}")
    implementation("software.amazon.awssdk:ssm:${awssdkVersion}")
    implementation("software.amazon.awssdk:glue:${awssdkVersion}")
    implementation("software.amazon.awssdk:kms:${awssdkVersion}")
    implementation("software.amazon.awssdk:s3:${awssdkVersion}")
    implementation("software.amazon.awssdk:sts:${awssdkVersion}")
    implementation("software.amazon.awssdk:dynamodb:${awssdkVersion}")

    // --- Iceberg dependencies
    runtimeOnly("org.apache.iceberg:iceberg-core:${icebergVersion}")
    implementation("org.apache.iceberg:iceberg-api:${icebergVersion}")
    runtimeOnly("org.apache.iceberg:iceberg-common:${icebergVersion}")
    runtimeOnly("org.apache.iceberg:iceberg-aws:${icebergVersion}")
    implementation("org.apache.iceberg:iceberg-snowflake:${icebergVersion}")
    implementation("org.apache.iceberg:iceberg-flink-2.0:${icebergVersion}")

    implementation("net.snowflake:snowflake-jdbc:3.27.0")
    
    // --- Flink test dependencies
    testImplementation("org.apache.flink:flink-test-utils:${flinkVersion}")
    testImplementation("org.apache.flink:flink-test-utils-junit:${flinkVersion}")

    // --- JUnit Jupiter for testing
    testImplementation(libs.junit.jupiter)

    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    // --- This dependency is used by the application.
    implementation(libs.guava)
}

// --- If the version is not provided, use the default
version = "dev-SNAPSHOT"

description = rootProject.name

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

application {
    // --- If the main class is not provided, use the default
    if (appMainClass.isNullOrEmpty()) {
        mainClass.set("kickstarter.AvroDataGeneratorApp")
    } else {
        mainClass.set("kickstarter." + appMainClass)
    }    
}

tasks.withType<Zip> {
    isZip64 = true
}

tasks.shadowJar {
    archiveBaseName.set(rootProject.name)
    archiveClassifier.set("")
    mergeServiceFiles()
    
    manifest {
        attributes(
            "Main-Class" to application.mainClass.get(),
            "Implementation-Title" to rootProject.name,
            "Implementation-Version" to project.version
        )
    }
}

tasks.build {
    dependsOn(tasks.shadowJar)
}

tasks.named<Test>("test") {
    useJUnitPlatform() 
    jvmArgs = listOf(
        "--add-opens", "java.base/java.util=ALL-UNNAMED",
        "--add-opens", "java.base/java.time=ALL-UNNAMED",
        "--add-opens", "java.base/java.lang.invoke=ALL-UNNAMED"
    )
}
