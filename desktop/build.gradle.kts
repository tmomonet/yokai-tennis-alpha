plugins {
    application
}

dependencies {
    implementation(project(":core"))
    implementation("com.badlogicgames.gdx:gdx-backend-lwjgl3:1.14.2")
    implementation("com.badlogicgames.gdx:gdx-platform:1.14.2:natives-desktop")
}

application {
    mainClass.set("com.cafeyokai.tennis.desktop.DesktopLauncher")
}

tasks.withType<JavaCompile> {
    options.release.set(17)
}
