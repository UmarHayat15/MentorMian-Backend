val ktor_version: String by project
val kotlin_version: String by project
val logback_version: String by project
val exposed_version: String by project
val postgresql_version: String by project
val hikari_version: String by project
val flyway_version: String by project
val tika_version: String by project
val pgvector_version: String by project
val okhttp_version: String by project

plugins {
    kotlin("jvm") version "2.0.21"
    id("io.ktor.plugin") version "2.3.12"
    id("org.jetbrains.kotlin.plugin.serialization") version "2.0.21"
    `maven-publish`
}

group = "com.aitutor"
version = (project.findProperty("packageVersion") as String?) ?: "0.1.0-SNAPSHOT"

application {
    mainClass.set("com.aitutor.ApplicationKt")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

repositories {
    mavenCentral()
}

dependencies {
    // Ktor Server
    implementation("io.ktor:ktor-server-core-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-netty-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-content-negotiation-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-config-yaml:$ktor_version")
    implementation("io.ktor:ktor-server-cors-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-status-pages-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-call-logging-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-default-headers-jvm:$ktor_version")

    // Ktor Serialization
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:$ktor_version")

    // Ktor Client (for calling LLM APIs)
    implementation("io.ktor:ktor-client-core-jvm:$ktor_version")
    implementation("io.ktor:ktor-client-cio-jvm:$ktor_version")
    implementation("io.ktor:ktor-client-content-negotiation-jvm:$ktor_version")

    // Database
    implementation("org.jetbrains.exposed:exposed-core:$exposed_version")
    implementation("org.jetbrains.exposed:exposed-dao:$exposed_version")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposed_version")
    implementation("org.jetbrains.exposed:exposed-java-time:$exposed_version")
    implementation("org.jetbrains.exposed:exposed-json:$exposed_version")
    implementation("org.postgresql:postgresql:$postgresql_version")
    implementation("com.zaxxer:HikariCP:$hikari_version")

    // pgvector
    implementation("com.pgvector:pgvector:$pgvector_version")

    // Flyway Migrations
    implementation("org.flywaydb:flyway-core:$flyway_version")
    implementation("org.flywaydb:flyway-database-postgresql:$flyway_version")

    // PDF Processing
    implementation("org.apache.tika:tika-core:$tika_version")
    implementation("org.apache.tika:tika-parsers-standard-package:$tika_version")

    // Logging
    implementation("ch.qos.logback:logback-classic:$logback_version")

    // OkHttp (for SSE streaming from LLM APIs)
    implementation("com.squareup.okhttp3:okhttp:$okhttp_version")
    implementation("com.squareup.okhttp3:okhttp-sse:$okhttp_version")

    // Testing
    testImplementation("io.ktor:ktor-server-tests-jvm:$ktor_version")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlin_version")
}

publishing {
    val githubRepository = System.getenv("GITHUB_REPOSITORY")
        ?: project.findProperty("gpr.repoPath") as String?
        ?: "UmarHayat15/MentorMian-Backend"

    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/$githubRepository")
            credentials {
                username = System.getenv("GITHUB_ACTOR") ?: project.findProperty("gpr.user") as String? ?: ""
                password = System.getenv("GITHUB_TOKEN") ?: project.findProperty("gpr.key") as String? ?: ""
            }
        }
    }
    publications {
        create<MavenPublication>("gpr") {
            from(components["java"])
            groupId = project.group.toString()
            artifactId = rootProject.name
            version = project.version.toString()
        }
    }
}
