import android.annotation.SuppressLint
import java.util.*

/*
 *    Copyright (c) 2023 Udhayarajan M
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 */

plugins {
    kotlin("multiplatform") version "1.7.22"
    id("convention.publication")
    id("com.android.library")
    id("org.jetbrains.dokka") version "1.7.20"
    id("io.github.gradle-nexus.publish-plugin") version "1.2.0"
}

group = "io.github.udhayarajan"
version = "5.5.9"
//Version Naming incremented if "<NEW_FEATURE_ADDED>.<WORKED_ON_BUG>.<BETA_VERSION_COUNT>"
//Priority on incrementing Feature > BugFix > Beta

repositories {
    google()
    mavenCentral()
}

subprojects {
    apply(plugin = "org.jetbrains.dokka")
}

kotlin {
    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = "11"
        }
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
        }
    }
    android {
        publishLibraryVariants("release", "debug")

    }
    sourceSets {
        val ktor_version = "2.3.1"

        @SuppressLint("DuplicatePlatformClasses")
        val jvmMain by getting {
            dependencies {
                // https://mvnrepository.com/artifact/org.json/json
                implementation("org.json:json:20220924")
                // https://mvnrepository.com/artifact/org.ccil.cowan.tagsoup/tagsoup
                implementation("org.ccil.cowan.tagsoup:tagsoup:1.2.1")
            }
        }

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
    sourceSets["main"].java.srcDir("src/androidMain/kotlin/")
    defaultConfig {
        minSdk = 21
        targetSdk = 33
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    lint {
        isAbortOnError = false
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.10.1")
    testImplementation(project(mapOf("path" to ":")))
    testImplementation(project(mapOf("path" to ":")))
    testImplementation(project(mapOf("path" to ":")))
    androidTestImplementation(project(mapOf("path" to ":")))
    androidTestImplementation(project(mapOf("path" to ":")))
}

tasks.dokkaHtml.configure {
    outputDirectory.set(file("$projectDir/docs"))
}

nexusPublishing {
// Stub secrets to let the project sync and build without the publication values set up
    val ext = rootProject.ext
    ext["signing.keyId"] = null
    ext["signing.password"] = null
    ext["sonatypeStagingProfileId"] = null
    ext["signing.key"] = null
    ext["ossrhUsername"] = null
    ext["ossrhPassword"] = null
    ext["sonatypeStagingProfileId"] = null

// Grabbing secrets from local.properties file or from environment variables, which could be used on CI
    val secretPropsFile = project.rootProject.file("local.properties")
    if (secretPropsFile.exists()) {
        secretPropsFile.reader().use {
            Properties().apply {
                load(it)
            }
        }.onEach { (name, value) ->
            ext[name.toString()] = value
            println(ext)
        }
    } else {
        ext["signing.keyId"] = System.getenv("SIGNING_KEY_ID")
        ext["signing.password"] = System.getenv("SIGNING_PASSWORD")
        ext["signing.key"] = System.getenv("SIGNING_KEY")
        ext["ossrhUsername"] = System.getenv("OSSRH_USERNAME")
        ext["ossrhPassword"] = System.getenv("OSSRH_PASSWORD")
        ext["sonatypeStagingProfileId"] = System.getenv("SONATYPE_STAGING_PROFILE_ID")
    }
    repositories {
        sonatype {
            stagingProfileId.set(ext["sonatypeStagingProfileId"].toString())
            username.set(ext["ossrhUsername"].toString())
            password.set(ext["ossrhPassword"].toString())
            nexusUrl.set(uri("https://s01.oss.sonatype.org/service/local/"))
            snapshotRepositoryUrl.set(uri("https://s01.oss.sonatype.org/content/repositories/snapshots/"))
        }
    }
}
