import org.jetbrains.kotlin.gradle.plugin.KotlinJsCompilerType
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization") version "1.6.0"
}

repositories {
    mavenCentral()
    maven(url = "https://maven.pkg.jetbrains.space/public/p/kotlinx-html/maven")
}

kotlin {
    jvm {
        withJava()
        compilations {
            val main by getting

            tasks {
                register<Copy>("copyResources") {
                    group = "publish"
                    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
                    from(main.output.resourcesDir)
                    destinationDir = File("$projectDir/build/publish")
                }
                register<Jar>("buildFatJar") {
                    group = "publish"
                    archiveClassifier.set("all")
                    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
                    dependsOn(named("jvmJar"))
                    dependsOn(named("copyResources"))
                    manifest {
                        attributes["Main-Class"] = "com.lehaine.littlekt.samples.DisplayTestKt"
                    }
                    destinationDirectory.set(File("$projectDir/build/publish/"))
                    from(
                        main.runtimeDependencyFiles.map { if (it.isDirectory) it else zipTree(it) },
                        main.output.classesDirs
                    )
                    doLast {
                        project.logger.lifecycle("[LittleKt] The bundled jar is available at: ${outputs.files.first()}")
                    }
                }

            }
        }
        compilations.all {
            kotlinOptions.jvmTarget = "11"
        }
        testRuns["test"].executionTask.configure {
            useJUnit()
        }
    }
    js(KotlinJsCompilerType.IR) {
        binaries.executable()
        browser {
            testTask {
                useKarma {
                    useChromeHeadless()
                }
            }
        }

        this.attributes.attribute(
            KotlinPlatformType.attribute,
            KotlinPlatformType.js
        )

        compilations.all {
            kotlinOptions.sourceMap = true
        }
    }
    val kotlinCoroutinesVersion: String by project

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":core"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinCoroutinesVersion")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        val jvmMain by getting {
            dependencies {
                implementation(project(":core"))
            }
        }
        val jvmTest by getting
        val jsMain by getting {
            dependencies {
                val kotlinxHtmlVersion = "0.7.2"
                implementation(project(":core"))
                implementation("org.jetbrains.kotlinx:kotlinx-html-js:$kotlinxHtmlVersion")
            }

        }
        val jsTest by getting

        all {
            languageSettings.apply {
                progressiveMode = true
                optIn("kotlinx.coroutines.ExperimentalCoroutinesApi")
                optIn("kotlin.contracts.ExperimentalContracts")
                optIn("kotlin.ExperimentalUnsignedTypes")
                optIn("kotlin.ExperimentalStdlibApi")
                optIn("kotlinx.serialization.ExperimentalSerializationApi")
                optIn("kotlin.time.ExperimentalTime")
            }
        }
    }
}
