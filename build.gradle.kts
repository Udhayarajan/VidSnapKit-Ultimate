plugins {
    kotlin("multiplatform") version "1.7.22"
    id("convention.publication")
    id("com.android.library")
}

group = "io.github.udhayarajan"
version = "1.0.0"

repositories {
    google()
    mavenCentral()
}


kotlin {
    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = "1.8"
        }
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
        }
    }
    android {
        publishLibraryVariants("release", "debug")

    }
    sourceSets {
        val ktor_version = "2.0.1"

        val jvmMain by getting
        val jvmTest by getting
        val androidMain by getting {
            dependencies {
//                implementation("com.google.android.material:material:1.7.0")
            }
        }
        val androidTest by getting {
            dependencies {
                implementation("junit:junit:4.13.2")
            }
        }
        val commonMain by getting {
            dependencies {
                implementation("io.ktor:ktor-client-core:$ktor_version")
                implementation("io.ktor:ktor-client-android:$ktor_version")
                implementation("io.ktor:ktor-client-serialization:$ktor_version")
                implementation("io.ktor:ktor-client-logging:$ktor_version")

                // https://mvnrepository.com/artifact/org.json/json
                implementation("org.json:json:20220924")

                // https://mvnrepository.com/artifact/org.apache.logging.log4j/log4j-core
                implementation("org.apache.logging.log4j:log4j-core:2.19.0")

            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}

android {
    compileSdk = 33
    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    defaultConfig {
        minSdk = 21
        targetSdk = 33
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    lint {
        isAbortOnError = false
    }
}