plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("maven-publish")
}

android {
    namespace = "com.dungz.our_ads"
    compileSdk = 35

    defaultConfig {
        minSdk = 24
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {

    // Compose
    implementation(platform("androidx.compose:compose-bom:2024.09.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.runtime:runtime")
    
    // Google Mobile Ads
    implementation("com.google.android.gms:play-services-ads-lite:23.6.0")
    
    debugImplementation("androidx.compose.ui:ui-tooling")

    // Firebase Remote Config
    implementation("com.google.firebase:firebase-config-ktx:22.0.1")

    // Coroutines Play Services (for Task.await())
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")

    // DataStore Preferences
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Core
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])
                groupId = "com.github.dungpro1572000"
                artifactId = "our_ads"
                version = "1.1.0"

                // Đảm bảo transitive dependencies được khai báo trong POM
                pom {
                    name.set("Our Ads")
                    description.set("Android Ads library with AdMob, Remote Config, and DataStore")
                    url.set("https://github.com/dungpro1572000/our_ads")

                    licenses {
                        license {
                            name.set("The Apache License, Version 2.0")
                            url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                        }
                    }
                }
            }
        }

        repositories {
            mavenLocal()
        }
    }
}
