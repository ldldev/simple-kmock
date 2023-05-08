import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.signing
import java.util.*

plugins {
    `java-library`
    `maven-publish`
    signing
}

// Stub secrets to let the project sync and build without the publication values set up
ext["signing.keyId"] = null
ext["signing.password"] = null
ext["signing.key"] = null
ext["ossrhUsername"] = null
ext["ossrhPassword"] = null

// Grabbing secrets from local.properties file or from environment variables, which could be used on CI
val secretPropsFile = project.rootProject.file("local.properties")
if (secretPropsFile.exists()) {
    secretPropsFile.reader().use {
        Properties().apply {
            load(it)
        }
    }.onEach { (name, value) ->
        ext[name.toString()] = value
    }
} else {
    ext["signing.keyId"] = System.getenv("SIGNING_KEY_ID")
    ext["signing.password"] = System.getenv("SIGNING_PASSWORD")
    ext["signing.key"] = System.getenv("SIGNING_KEY")
    ext["ossrhUsername"] = System.getenv("OSSRH_USERNAME")
    ext["ossrhPassword"] = System.getenv("OSSRH_PASSWORD")
}

val javadocJar by tasks.registering(Jar::class) {
    archiveClassifier.set("javadoc")
}

fun getExtraString(name: String) = ext[name]?.toString()

java {
    withJavadocJar()
    withSourcesJar()
}

publishing {
    publications {
        create<MavenPublication>("simpleKMock") {
            from(components["java"])
            pom {
                name.set("Simple KMock")
                description.set("A simple mocking framework for Kotlin")
                url.set("https://github.com/ldldev/simple-kmock")

                licenses {
                    license {
                        name.set("MIT")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }
                developers {
                    developer {
                        id.set("ldldev")
                        name.set("Lucas de Jong")
                        email.set("dev.ldldevelopers@gmail.com")
                    }
                }
                scm {
                    url.set("https://github.com/ldldev/simple-kmock.git")
                }
            }
        }
    }
    // Configure maven central repository
    repositories {
        maven {
            name = "sonatype"
            setUrl("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
            credentials {
                username = getExtraString("ossrhUsername")
                password = getExtraString("ossrhPassword")
            }
        }
    }
}

// Signing artifacts. Signing.* extra properties values will be used
signing {
    useInMemoryPgpKeys(
        getExtraString("signing.keyId"),
        getExtraString("signing.key"),
        getExtraString("signing.password")
    )
    sign(publishing.publications)
}
