plugins {
    application
    id("org.springframework.boot") version "3.5.3"
    id("io.spring.dependency-management") version "1.1.7"
    jacoco
    id("org.sonarqube") version "5.1.0.4882"
}

group = "com.kousenit"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(24)
    }
}

application {
    mainClass.set("com.kousenit.veojava.VeoVideoDemo")
}

tasks.withType<JavaCompile> {
    options.compilerArgs.addAll(listOf("--enable-preview"))
}

tasks.withType<Test> {
    jvmArgs("--enable-preview")
}

// Enable preview features for bootRun task
tasks.named<org.springframework.boot.gradle.tasks.run.BootRun>("bootRun") {
    jvmArgs("--enable-preview")
}

// Enable preview features for application plugin run task
tasks.named<JavaExec>("run") {
    jvmArgs("--enable-preview")
    standardInput = System.`in`  // Enable console input for interactive mode
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
    useJUnitPlatform()
    finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        html.required.set(true)
        html.outputLocation.set(layout.buildDirectory.dir("reports/jacoco/html"))
    }
}

// Ensure sonar task runs after JaCoCo report is generated
tasks.named("sonar") {
    dependsOn(tasks.jacocoTestReport)
}

// Optional: Set minimum coverage thresholds
tasks.jacocoTestCoverageVerification {
    violationRules {
        rule {
            limit {
                minimum = 0.80.toBigDecimal() // 80% coverage minimum
            }
        }
    }
}

// SonarQube configuration
sonarqube {
    properties {
        property("sonar.projectKey", "kousen_VeoJava")
        property("sonar.organization", "kousen-it-inc")
        property("sonar.host.url", "https://sonarcloud.io")
        
        // Coverage configuration
        property("sonar.coverage.jacoco.xmlReportPaths", "build/reports/jacoco/test/jacocoTestReport.xml")
        property("sonar.junit.reportPaths", "build/test-results/test")
        
        // Source and test directories
        property("sonar.sources", "src/main/java")
        property("sonar.tests", "src/test/java")
        
        // Exclusions
        property("sonar.exclusions", "**/VeoVideoDemo.java")  // Exclude demo class from coverage requirements
        property("sonar.test.exclusions", "**/integration/**")  // Exclude integration tests from coverage
        property("sonar.coverage.exclusions", "**/VeoVideoDemo.java,**/integration/**")  // Also exclude from coverage
        
        // Rule exclusions for educational/demo project
        property("sonar.issue.ignore.multicriteria", "e1,e2,e3,e4")
        property("sonar.issue.ignore.multicriteria.e1.ruleKey", "java:S112") // Generic exceptions are appropriate here
        property("sonar.issue.ignore.multicriteria.e1.resourceKey", "**/*.java")
        property("sonar.issue.ignore.multicriteria.e2.ruleKey", "java:S1845") // Unnamed variables are valid Java 22+ feature
        property("sonar.issue.ignore.multicriteria.e2.resourceKey", "**/*.java")
        property("sonar.issue.ignore.multicriteria.e3.ruleKey", "java:S1192") // String literal duplication - often false positives
        property("sonar.issue.ignore.multicriteria.e3.resourceKey", "**/*.java")
        property("sonar.issue.ignore.multicriteria.e4.ruleKey", "java:S125") // Commented code - false positives on educational comments
        property("sonar.issue.ignore.multicriteria.e4.resourceKey", "**/*Test.java")
    }
}

