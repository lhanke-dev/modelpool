plugins {
    id("common-configuration")

    antlr
    `java-library`
}

dependencies {
    implementation("org.reflections:reflections:${reflectionsVersion}")
    antlr("org.antlr:antlr4:${antlrVersion}")
}

sourceSets {
    val antlrGen by creating {
        java {
            setSrcDirs(listOf("src-gen/main/java"))
        }
        dependencies {
            add("antlrGenImplementation", "org.antlr:antlr4:${antlrVersion}")
        }
    }
    val main by getting {
        java {
            setSrcDirs(listOf("src/main/java")) // remove paths appended by antlr plugin
            compileClasspath += antlrGen.output
            runtimeClasspath += antlrGen.output
        }
    }
    val test by getting {
        java {
            setSrcDirs(listOf("src/test/java")) // remove paths appended by antlr plugin
            compileClasspath += antlrGen.output
            runtimeClasspath += antlrGen.output
        }
    }
}

tasks {
    generateGrammarSource {
        maxHeapSize = "64m"
        outputDirectory = File(project.sourceSets.getByName("antlrGen").allJava.srcDirs.stream().findFirst().get().absolutePath)
    }

    clean {
        doLast {
            project.sourceSets.getByName("antlrGen").allJava.srcDirs.forEach {
                project.delete(fileTree(it.absolutePath))
                File(it.absolutePath).listFiles()?.forEach {
                    project.delete(it)
                }
            }
        }
    }

    compileJava {
        dependsOn("compileAntlrGenJava")
    }

    getByName("compileAntlrGenJava") {
        dependsOn("generateGrammarSource")
    }
}