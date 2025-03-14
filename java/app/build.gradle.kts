plugins {
    application
    id("org.kordamp.gradle.project-enforcer") version "0.14.0"
}

// --- Read the Gradle properties file
val appVersion: String? by project
val appMainClass: String? by project

repositories {
    mavenLocal()
    mavenCentral()
    maven {
        url = uri("https://packages.confluent.io/maven/")
    }
}

// --- Dependency version numbers
val flinkVersion: String = "1.20"
val flinkVersionWithPatch: String = flinkVersion + ".0"
val hadoopVersion: String = "3.3.6"
val kafkaVersion: String = "3.7.0"
val junitVersion: String = "5.10.0"
val awssdkVersion: String = "2.26.29"
var icebergVersion: String = "1.8.1"
var confluentKafkaVersion: String = "7.7.1"
var jacksonVersion: String = "2.18.1"

dependencies {
    implementation("org.apache.kafka:kafka-clients:${kafkaVersion}")
    implementation("org.apache.avro:avro:1.12.0")
    implementation("io.confluent:kafka-avro-serializer:${confluentKafkaVersion}")
    implementation("io.confluent:kafka-schema-registry-client:${confluentKafkaVersion}")
    implementation("io.confluent:confluent-log4j:1.2.17-cp12")
    implementation("tech.allegro.schema.json2avro:converter:0.2.15")
    implementation("com.fasterxml.jackson.core:jackson-databind:${jacksonVersion}")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-avro:${jacksonVersion}")
    implementation("org.apache.hadoop:hadoop-common:${hadoopVersion}")
    implementation("org.apache.flink:flink-java:${flinkVersionWithPatch}")
    compileOnly("org.apache.flink:flink-streaming-java:${flinkVersionWithPatch}")
    compileOnly("org.apache.flink:flink-table-common:${flinkVersionWithPatch}")
    compileOnly("org.apache.flink:flink-table:${flinkVersionWithPatch}")
    compileOnly("org.apache.flink:flink-table-api-java-bridge:${flinkVersionWithPatch}")
    implementation("org.apache.flink:flink-clients:${flinkVersionWithPatch}")
    implementation("org.apache.flink:flink-connector-base:${flinkVersionWithPatch}")
    implementation("org.apache.flink:flink-connector-kafka:3.3.0-${flinkVersion}")
    implementation("org.apache.flink:flink-connector-datagen:${flinkVersionWithPatch}")
    implementation("org.apache.flink:flink-avro:${flinkVersionWithPatch}")
    implementation("org.apache.flink:flink-avro-confluent-registry:${flinkVersionWithPatch}")
    implementation("org.apache.flink:flink-json:${flinkVersionWithPatch}")
    implementation("org.slf4j:slf4j-log4j12:2.0.7")
    implementation("software.amazon.awssdk:sdk-core:${awssdkVersion}")
    implementation("software.amazon.awssdk:secretsmanager:${awssdkVersion}")
    implementation("software.amazon.awssdk:ssm:${awssdkVersion}")
    implementation("software.amazon.awssdk:glue:${awssdkVersion}")
    implementation("software.amazon.awssdk:kms:${awssdkVersion}")
    implementation("software.amazon.awssdk:s3:${awssdkVersion}")
    implementation("software.amazon.awssdk:sts:${awssdkVersion}")
    implementation("software.amazon.awssdk:dynamodb:${awssdkVersion}")
    implementation("org.json:json:20240303")
    runtimeOnly("org.apache.iceberg:iceberg-core:${icebergVersion}")
    runtimeOnly("org.apache.iceberg:iceberg-aws:${icebergVersion}")
    implementation("org.apache.iceberg:iceberg-snowflake:${icebergVersion}")
    implementation("org.apache.iceberg:iceberg-flink-runtime-${flinkVersion}:$icebergVersion") {
        exclude(group = "io.dropwizard.metrics", module = "metrics-core")
    }
    implementation("net.snowflake:snowflake-jdbc:3.19.0")
    
    testImplementation("org.apache.flink:flink-test-utils:${flinkVersionWithPatch}")
    testImplementation("org.junit.jupiter:junit-jupiter:${junitVersion}")
    testImplementation("org.junit.jupiter:junit-jupiter-api:${junitVersion}")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:${junitVersion}")
    testImplementation("org.apache.flink:flink-test-utils-junit:${flinkVersionWithPatch}")
}

// --- If the version is not provided, use the default
version = appVersion ?: "x.xx.xx.xxx"

description = rootProject.name

java {
    sourceCompatibility = JavaVersion.VERSION_17
}

application {
    // --- If the main class is not provided, use the default
    if (appMainClass.isNullOrEmpty()) {
        mainClass.set("kickstarter.JsonDataGeneratorApp")
    } else {
        mainClass.set("kickstarter." + appMainClass)
    }    
}

tasks.withType<Zip> {
    isZip64 = true
}

tasks {
    val fatJar = register<Jar>("fatJar") {
        dependsOn.addAll(listOf("compileJava", "processResources"))
        archiveBaseName.set(rootProject.name)
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        manifest {
            attributes["Main-Class"] = application.mainClass
            attributes["Implementation-Title"] = rootProject.name
            attributes["Implementation-Version"] = project.version
        }
        from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) } + sourceSets.main.get().output)
    }
    build {
        dependsOn(fatJar)
    }
}

tasks.compileJava {
    options.isIncremental = false
}

tasks.named<Test>("test") {
    useJUnitPlatform() 
    jvmArgs = listOf(
        "--add-opens", "java.base/java.util=ALL-UNNAMED",
        "--add-opens", "java.base/java.time=ALL-UNNAMED",
        "--add-opens", "java.base/java.lang.invoke=ALL-UNNAMED"
    )
}