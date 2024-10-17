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
val flinkVersion: String = "1.19.1"
val kafkaVersion: String = "3.7.0"
val junitVersion: String = "5.10.0"
val awssdkVersion: String = "2.26.29"
var icebergVersion: String = "1.6.1"

dependencies {
    implementation("org.apache.hadoop:hadoop-common:3.3.6")
    implementation("org.apache.kafka:kafka-clients:${kafkaVersion}")
    implementation("org.apache.flink:flink-java:${flinkVersion}")
    compileOnly("org.apache.flink:flink-streaming-java:${flinkVersion}")
    compileOnly("org.apache.flink:flink-table-common:${flinkVersion}")
    compileOnly("org.apache.flink:flink-table:${flinkVersion}")
    compileOnly("org.apache.flink:flink-table-api-java-bridge:${flinkVersion}")
    implementation("org.apache.flink:flink-clients:${flinkVersion}")
    implementation("org.apache.flink:flink-connector-base:${flinkVersion}")
    implementation("org.apache.flink:flink-connector-kafka:3.2.0-1.19")
    implementation("org.apache.flink:flink-connector-datagen:${flinkVersion}")
    implementation("org.apache.flink:flink-json:${flinkVersion}")
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
    implementation("org.apache.iceberg:iceberg-flink-runtime-1.19:${icebergVersion}")
    implementation("net.snowflake:snowflake-jdbc:3.19.0")
    
    testImplementation("org.apache.flink:flink-test-utils:${flinkVersion}")
    testImplementation("org.junit.jupiter:junit-jupiter:${junitVersion}")
    testImplementation("org.junit.jupiter:junit-jupiter-api:${junitVersion}")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:${junitVersion}")
    testImplementation("org.apache.flink:flink-test-utils-junit:${flinkVersion}")
}

// --- If the version is not provided, use the default
version = appVersion ?: "x.xx.xx.xxx"

description = rootProject.name

java {
    sourceCompatibility = JavaVersion.VERSION_11
}

application {
    // --- If the main class is not provided, use the default
    if (appMainClass.isNullOrEmpty()) {
        mainClass.set("kickstarter.DataGeneratorApp")
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