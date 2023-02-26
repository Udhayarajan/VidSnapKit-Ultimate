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
    `kotlin-dsl`
    id("io.github.gradle-nexus.publish-plugin") version "1.2.0"
}

repositories {
    gradlePluginPortal()
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
        }
    }
}