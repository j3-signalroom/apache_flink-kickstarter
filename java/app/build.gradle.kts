plugins {
    application
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

dependencies {
    implementation("org.apache.kafka:kafka-clients:3.8.0")
    implementation("org.apache.flink:flink-java:1.20.0")
    compileOnly("org.apache.flink:flink-streaming-java:1.20.0")
    implementation("org.apache.flink:flink-clients:1.20.0")
    implementation("org.apache.flink:flink-connector-base:1.20.0")
    implementation("org.apache.flink:flink-connector-kafka:3.2.0-1.19")
    implementation("org.apache.flink:flink-connector-datagen:1.20.0")
    implementation("org.apache.flink:flink-json:1.20.0")
    implementation("org.slf4j:slf4j-log4j12:2.0.7")
    implementation("software.amazon.awssdk:secretsmanager:2.26.29")
    implementation("software.amazon.awssdk:ssm:2.26.29")
    implementation("org.json:json:20240303")
    testImplementation("org.apache.flink:flink-test-utils:1.20.0")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.0")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.10.0")
    testImplementation("org.apache.flink:flink-test-utils-junit:1.20.0")
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
        mainClass.set("apache_flink.kickstarter.datastream_api.DataGeneratorJob")
    } else {
        mainClass.set("apache_flink.kickstarter." + appMainClass)
    }    
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