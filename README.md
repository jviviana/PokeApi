# Pokémon Stadium Lite

Juego de combate Pokémon en Java Swing que usa [PokeAPI](https://pokeapi.co/).

## Requisitos

- JDK 17 o más nuevo
- Conexión a internet (los Pokémon se descargan de PokeAPI)

## Cómo correrlo

Desde la terminal, en la carpeta del proyecto:

```bash
# Windows
gradlew.bat run

# Linux / macOS
./gradlew run
```

O desde IntelliJ: abrir el proyecto y ejecutar `src/main/java/pokemon/Main.java`.

## Estructura

```
src/main/java/pokemon/
├── Main.java                  punto de entrada
├── api/                       cliente HTTP de PokeAPI
├── combate/                   reglas del combate (Battle) y su listener
├── interfaz/                  ventana y paneles Swing
└── modelo/                    clase Pokemon
```
