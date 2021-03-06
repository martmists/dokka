package org.jetbrains

import com.github.jengelman.gradle.plugins.shadow.ShadowExtension
import com.jfrog.bintray.gradle.BintrayExtension
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.kotlin.dsl.*

class DokkaPublicationBuilder {
    enum class Component {
        Java, Shadow
    }

    var artifactId: String? = null
    var component: Component = Component.Java
}

fun Project.registerDokkaArtifactPublication(publicationName: String, configure: DokkaPublicationBuilder.() -> Unit) {
    configure<PublishingExtension> {
        publications {
            register<MavenPublication>(publicationName) {
                val builder = DokkaPublicationBuilder().apply(configure)
                this.artifactId = builder.artifactId
                when (builder.component) {
                    DokkaPublicationBuilder.Component.Java -> from(components["java"])
                    DokkaPublicationBuilder.Component.Shadow -> run {
                        artifact(tasks["sourcesJar"])
                        extensions.getByType(ShadowExtension::class.java).component(this)
                    }
                }
            }
        }
    }

    configureBintrayPublication(publicationName)
}

fun Project.configureBintrayPublication(vararg publications: String) {
    val dokka_version: String by this
    val dokka_publication_channel: String by this
    extensions.configure<BintrayExtension>("bintray") {
        user = System.getenv("BINTRAY_USER")
        key = System.getenv("BINTRAY_KEY")
        dryRun = System.getenv("BINTRAY_DRY_RUN") == "true" ||
                project.properties["bintray_dry_run"] == "true"
        pkg = PackageConfig().apply {
            repo = dokka_publication_channel
            name = "dokka"
            userOrg = "kotlin"
            desc = "Dokka, the Kotlin documentation tool"
            vcsUrl = "https://github.com/kotlin/dokka.git"
            setLicenses("Apache-2.0")
            version = VersionConfig().apply {
                name = dokka_version
            }
        }
        setPublications(*publications)
    }
}

