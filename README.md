# Local Control

Local Control is a client-side Minecraft mod for local control of time, weather, and visual environmental parameters without affecting the server state.

This project was first set up as a Fabric-compatible mod for the current stable client ecosystem and can later be ported to Quilt without changing the mod logic itself.

## Features

- Local day-night cycle override
- Preset time selection
- Smooth time drift and custom speed override
- Local weather override: clear, rain, thunder
- Additional client-side environment controls
- GUI menu with Russian and English localization
- Keyboard shortcuts for opening the menu and toggling local modes
- JSON configuration saved in the game config directory

## Requirements

- Minecraft Java Edition: 1.21.1
- Fabric Loader: 0.16.14
- Fabric API: 0.116.1+1.21.1
- Java: 25
- Gradle: 8+

## Installation

1. Place the built jar in the `mods` folder of your Fabric client installation.
2. Start the client using a Fabric-compatible launcher.
3. Use the configured hotkeys or open the menu from the Minecraft controls.

## Build

```bash
gradle build
```

The final jar is produced at:

```text
build/libs/LocalControl-1.0.0.jar
```

## Client-side safety

This mod only changes the client world state. It does not send commands, packets, or world updates to the server. Multiplayer effects remain visible only to the local user.
