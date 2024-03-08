import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    java
    alias(libs.plugins.kotlin)
    `maven-publish`
}

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    //implementation("com.soywiz.korlibs.korte:korte:3.0.0")
    implementation(libs.okhttp)
    implementation(libs.coroutines.okhttp)
    implementation(libs.jackson.kotlin)
    implementation(libs.kotlinx.coroutines)
    implementation(libs.snakeyaml)
    implementation(libs.bundles.flexmark)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}

tasks {
    val jar: Jar = (this@tasks["jar"] as Jar)
    jar.manifest {
        attributes["Main-Class"] = "MainKt"
    }
    val jarDeps by creating(Copy::class.java) {
        group = "package"
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        from(configurations.runtimeClasspath.get())
        into(File(buildDir, "deps"))
    }

    val fatJarDeps by creating(Jar::class.java) {
        group = "package"
        archiveBaseName.set("notion2jekyll-deps")
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    }

    val fatJar by creating(Jar::class.java) {
        group = "package"
        manifest {
            attributes["Main-Class"] = "MainKt"
        }
        archiveBaseName.set("notion2jekyll-all")
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        with(this@tasks["jar"] as CopySpec)
        from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    }
}

tasks.withType<Test> {
    testLogging {
        events = mutableSetOf(
            //TestLogEvent.STARTED, TestLogEvent.PASSED,
            TestLogEvent.SKIPPED,
            TestLogEvent.FAILED,
            TestLogEvent.STANDARD_OUT, TestLogEvent.STANDARD_ERROR
        )
        exceptionFormat = TestExceptionFormat.FULL
        showStackTraces = true
        showStandardStreams = true
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = "1.8" // Specify the JVM version here
    }
}

group = "com.soywiz"
version = "0.0.1-SNAPSHOT"

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            //artifact(tasks.getByName("jar"))
            artifact(tasks.getByName("fatJar"))
        }
    }
}