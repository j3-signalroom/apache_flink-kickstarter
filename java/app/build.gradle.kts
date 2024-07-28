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
    implementation("org.apache.kafka:kafka-clients:3.7.0")
    implementation("org.apache.flink:flink-java:1.19.1")
    compileOnly("org.apache.flink:flink-streaming-java:1.19.1")
    implementation("org.apache.flink:flink-clients:1.19.1")
    implementation("org.apache.flink:flink-connector-base:1.19.1")
    implementation("org.apache.flink:flink-connector-kafka:3.2.0-1.19")
    implementation("org.apache.flink:flink-connector-datagen:1.19.1")
    implementation("org.apache.flink:flink-json:1.19.1")
    implementation("org.slf4j:slf4j-log4j12:2.0.7")
    testImplementation("org.apache.flink:flink-test-utils:1.19.1")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.0")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.10.0")
    testImplementation("org.apache.flink:flink-test-utils-junit:1.19.1")
}

// --- If the version is not provided, use the default
if (appVersion.isNullOrEmpty()) {
    version = "x.xx.xx.xxx"
} else {
    version = appVersion
}

description = rootProject.name

java {
    sourceCompatibility = JavaVersion.VERSION_11
}

application {
    // --- If the main class is not provided, use the default
    if (appMainClass.isNullOrEmpty()) {
        mainClass.set("data_stream_api.UserStatisticsJob")
    } else {
        mainClass.set("data_stream_api." + appMainClass)
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