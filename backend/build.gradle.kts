plugins {
    kotlin("jvm") version "2.2.10"
    application
}
kotlin { jvmToolchain(17) }
application { mainClass.set("id.ac.loopy.server.ApplicationKt") }
dependencies {
    implementation("io.ktor:ktor-server-core-jvm:3.2.3")
    implementation("io.ktor:ktor-server-netty-jvm:3.2.3")
    implementation("ch.qos.logback:logback-classic:1.5.18")
    testImplementation(kotlin("test-junit"))
    testImplementation("io.ktor:ktor-server-test-host-jvm:3.2.3")
}
