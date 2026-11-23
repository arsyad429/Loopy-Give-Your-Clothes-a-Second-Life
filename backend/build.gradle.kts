plugins {
    kotlin("jvm") version "2.2.10"
    application
}
kotlin { jvmToolchain(17) }
application { mainClass.set("id.ac.loopy.server.ApplicationKt") }
dependencies {
    implementation("io.ktor:ktor-server-core-jvm:3.2.3")
    implementation("io.ktor:ktor-server-netty-jvm:3.2.3")
    implementation("io.ktor:ktor-client-core-jvm:3.2.3")
    implementation("io.ktor:ktor-client-cio-jvm:3.2.3")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")
    implementation("ch.qos.logback:logback-classic:1.5.18")

    testImplementation(kotlin("test-junit"))
    testImplementation("io.ktor:ktor-server-test-host-jvm:3.2.3")
    testImplementation("io.ktor:ktor-client-mock-jvm:3.2.3")

    implementation("io.ktor:ktor-server-auth-jvm:3.2.3")
    implementation("com.zaxxer:HikariCP:7.1.0")
    implementation("org.flywaydb:flyway-core:11.20.3")
    implementation("org.flywaydb:flyway-database-postgresql:11.20.3")
    runtimeOnly("org.postgresql:postgresql:42.7.14")

    implementation("io.ktor:ktor-server-body-limit-jvm:3.2.3")
    implementation("io.ktor:ktor-server-status-pages-jvm:3.2.3")
    implementation("com.drewnoakes:metadata-extractor:2.19.0")
}
