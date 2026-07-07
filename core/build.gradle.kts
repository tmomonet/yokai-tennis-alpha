plugins {
    `java-library`
}

dependencies {
    api("com.badlogicgames.gdx:gdx:1.14.2")
    implementation("com.google.android.gms:play-services-games-v2:21.0.0")

    testImplementation(platform("org.junit:junit-bom:6.0.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<JavaCompile> {
    options.release.set(17)
}

tasks.test {
    useJUnitPlatform()
}
