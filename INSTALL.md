# RenderExtender 1.8.9 - Pojav Launcher Cheat

## Sobre
Mod para Minecraft 1.8.9 rodando no Pojav Launcher via Forge.
GUI roxa estilo CS:GO com módulos de combate.

## Módulos
| Módulo   | Keybind | Descrição                    |
|----------|---------|------------------------------|
| KillAura | R       | Auto-ataque FUD              |
| Reach    | -       | Alcance extendido            |
| HitBox   | -       | Hitbox expandida             |
| Scaffold | V       | Auto-construção Ninja Bridge |
| GUI      | X       | ClickGUI CS:GO purple        |

## Build
```bash
# Requer JDK 8 + Gradle 4.4+
cd pojav-cheat-189
gradle wrapper
./gradlew build
```

## Instalação no Pojav Launcher
1. Perfil Forge 1.8.9
2. Copiar `build/libs/RenderExtender-2.0.jar` para mods/
3. Pressionar X para abrir GUI

## FUD Features
- String obfuscation multi-camada
- CPS randomization + jitter
- GCD rotation fix
- Ninja Bridge bypass
- Hit chance randomizado
- Silent switching
