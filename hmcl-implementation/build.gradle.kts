plugins {
    id("java")
    id("checkstyle")
}

group = "net.burningtnt"
version = "1.0-SNAPSHOT"

checkstyle {
    sourceSets = mutableSetOf()
}

repositories {
    mavenCentral()
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}

tasks.getByName("build") {
    dependsOn(tasks.getByName("checkstyleMain") {
        group = "build"
    })
    dependsOn(tasks.getByName("checkstyleTest") {
        group = "build"
    })
}

dependencies {
    implementation(rootProject)

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.2")
}