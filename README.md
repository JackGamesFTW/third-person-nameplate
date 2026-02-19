# Third Person Nameplate

A Hytale mod that lets players see their own nameplate above their head in third-person.

The Hytale client always hides the local player's own nameplate, regardless of camera mode. This mod works around that by spawning an invisible hologram entity mounted to the player that carries a `Nameplate` component — visible to everyone, including the player themselves.

## Features

- **Toggle command** - `/shownameplate` to toggle your own nameplate on/off (off by default)
- **Smooth positioning** - the hologram is mounted to the player via `MountedComponent`, so it follows smoothly with no tick-lag jitter
- **Crouch support** - nameplate Y offset adjusts when the player crouches
- **Mirror system** - if another plugin changes a player's nameplate text (e.g. display name plugins), the hologram automatically mirrors the update
- **Clean lifecycle** - hologram entities are non-serialized (never saved to disk) and cleaned up on disconnect/world drain

## How it works

1. On `/shownameplate` the mod spawns a `ProjectileComponent`-based entity mounted to the player with a Y offset
2. The player's native nameplate text is moved to the hologram entity, and the native nameplate is cleared to prevent duplicates for other players
3. A `RefChangeSystem` watches for nameplate text changes on player entities and mirrors them to the hologram
4. An `EntityTickingSystem` monitors the player's crouch state and adjusts the hologram's mount offset accordingly
5. On toggle off, the hologram is removed and the native nameplate text is restored

## Building

```
./gradlew build
```

The built jar will be in `build/libs/`.

## Installation

Drop the built jar into your Hytale server's `mods/` directory.

### Dependencies

- `Hytale:EntityModule` (built-in)

## Project structure

```
src/main/java/me/jack/thirdpersonnameplate/
├── ThirdPersonNameplatePlugin.java       # Main plugin — spawn/remove lifecycle, event listeners
├── commands/
│   └── ShowNameplateCommand.java         # /shownameplate toggle command
├── components/
│   └── NameplateOwnerComponent.java      # Marker component linking hologram → player
└── systems/
    ├── NameplateMirrorSystem.java        # Mirrors nameplate text changes to hologram
    └── CrouchOffsetSystem.java           # Adjusts Y offset on crouch
```

## License

MIT