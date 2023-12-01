plugins {
    alias(libs.plugins.kotlin)
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
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit")
    testImplementation(libs.junit)
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

    // https://superuser.com/questions/370388/simple-built-in-way-to-encrypt-and-decrypt-a-file-on-a-mac-via-command-line
    val jarEncrypted by creating(Exec::class.java) {
        group = "package"
        dependsOn(jar)
        dependsOn(fatJarDeps)
        // https://superuser.com/questions/370388/simple-built-in-way-to-encrypt-and-decrypt-a-file-on-a-mac-via-command-line
        doFirst {
            val inputFile = jar.archiveFile.get()
            val pass = "MDd_oWiEGNfmpTy_q7NR7m"
            commandLine(
                "openssl", "des3",
                "-pass", "pass:$pass",
                "-in", inputFile,
                "-out", inputFile.asFile.absolutePath.replace(".jar", ".bin")
            )
        }
    }

    val jarAndCopy by creating(Copy::class.java) {
        group = "package"
        dependsOn(jar)
        from(jar.archiveFile)
        into(File(projectDir, "../soywiz.com"))
        into(File(projectDir, "../blog.korge.org"))
    }
}
