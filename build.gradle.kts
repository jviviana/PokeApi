plugins {
    id("java")
    application
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    // libreria para leer el JSON que responde PokeAPI (JSONObject, JSONArray)
    implementation("org.json:json:20250517")

    testImplementation(platform("org.junit:junit-bom:6.0.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.release = 17 // funciona con cualquier JDK 17 o mas nuevo
}

application {
    mainClass = "pokemon.Main"
}

tasks.test {
    useJUnitPlatform()
}
