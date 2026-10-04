# Pokémon Stadium Lite

Juego de combate Pokémon en Java Swing que usa [PokeAPI](https://pokeapi.co/).

## Requisitos

- JDK 17 o más nuevo
- Conexión a internet (los Pokémon se descargan de PokeAPI)

## Cómo correrlo

Abrir la carpeta del proyecto en IntelliJ y ejecutar `src/pokemon/Main.java`.
La librería para leer JSON ya viene incluida en `lib/json-20240303.jar`.

## Estructura

```
src/pokemon/
├── Main.java                  punto de entrada
├── api/                       cliente HTTP de PokeAPI
├── combate/                   reglas del combate (Battle) y su listener
├── interfaz/                  ventana y paneles Swing
└── modelo/                    clase Pokemon
```
