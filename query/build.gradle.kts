import org.gradle.api.publish.maven.MavenPublication
import org.gradle.kotlin.dsl.publishing
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
    `maven-publish`
}

android {
    namespace = "com.blipblipcode.query"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        minSdk = 24

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}
kotlin{
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}


publishing {
    publications {
        create<MavenPublication>("release") {
            groupId = "com.github.LeandroLCD"
            artifactId = "query"
            version = project.version.toString()
        }
    }
}

afterEvaluate {
    val releaseComponent = components.findByName("release")
    if (releaseComponent != null) {
        publishing {
            publications {
                val pub = getByName("release") as MavenPublication
                pub.from(releaseComponent)
            }
        }
    } else {
        logger.warn("Android 'release' component not found; maven publication won't include component artifacts.")
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.sqlite.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}

tasks.register("runQueryUnitTests") {
    group = "verification"
    description = "Ejecuta solo los tests unitarios del módulo query"

    dependsOn("testDebugUnitTest")

    doLast {
        println("✅ Tests unitarios del módulo query completados")
    }
}
