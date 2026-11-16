# Lyrica 1.8.9 - Minecraft Music & Synced Lyrics Mod

Lyrica 1.8.9 is an unofficial port of [Lyrica](https://github.com/fakemaluco/lyrica-mod) to Minecraft 1.8.9 with Forge. It displays real-time synchronized lyrics from Spotify, YouTube Music, Apple Music, and more. Enjoy customizable 2D and 3D lyrics and playback controls.

![Lyrica banner](https://i.postimg.cc/ryYtqpRn/lyricabanner.png)

[![Latest release](https://img.shields.io/github/v/release/Alouuuu/lyrica-mod-1.8.9?label=release)](https://github.com/Alouuuu/lyrica-mod-1.8.9/releases/latest)

**Music, playback controls, and synchronized lyrics inside Minecraft.**

[Download](#download) · [Features](#features) · [Differences from Lyrica](#differences-from-lyrica) · [Privacy](#privacy) · [Documentation](#documentation)

## New in 0.9.4-1.8.9

The first release of the 1.8.9 port, based on Lyrica 0.9.4. It brings the HUD lyrics, the in-game player, world lyrics, translation, and romanization to Forge 1.8.9, and checks this fork’s releases for updates. See the [0.9.4-1.8.9 release notes](legacy-1.8.9/docs/releases/0.9.4-1.8.9.md).

## Features

### Your music in Minecraft

![In-game player animation](https://i.postimg.cc/d3vnvmHZ/musicingame-animado.gif)

Pause, skip, seek, and adjust the volume from the in-game player. On Windows, Lyrica can pick up Spotify, YouTube, Apple Music, and other players that expose media controls to the system. Start your music and open the player in Minecraft.

![Player and synchronized lyrics](https://i.postimg.cc/zDpFK5bV/01-player-letras.webp)

![Section divider](https://i.postimg.cc/Nf39rP4P/divisor.png)

### Lyrics in your world

![Animated world lyrics](https://i.postimg.cc/rmWc7c3w/Imerssive-Lyrics-ondas-v2.gif)

Follow synchronized lyrics in the world, in the on-screen panel, or in the compact player. Word-by-word timing depends on the lyrics available for the track.

![Lyrics rendered in a Minecraft scene](https://i.postimg.cc/3JJ9cW5V/02-letras-no-cenario.webp)

![Section divider](https://i.postimg.cc/Nf39rP4P/divisor.png)

### Make it yours

![Customization preview](https://i.postimg.cc/T1Y7gJV2/Customization-animado.gif)

Choose fonts, adjust the layout, and change where lyrics appear. Lyrica supports multiple lyrics sources, translation, romanization, and Google Fonts. Press **L** to open the settings; you can change the key in **Controls**.

![Lyrica appearance settings](https://i.postimg.cc/t40wJfSy/Appearance-original-restaurado.png)

## Differences from Lyrica

The 1.8.9 port runs on Minecraft 1.8.9 and Java 8, so some features of the Fabric versions are not available yet:

- Music Presence and the multiplayer karaoke, including the `/music` and `/karaoke` commands.
- Animated artwork and Canvas video. Lyrica shows the static cover instead.
- The Jade integration.

WebP and AVIF covers are requested as JPEG or PNG instead. Terrain lighting from world lyrics is off by default, because it rebuilds chunks. Report problems with the port in [this repository](https://github.com/Alouuuu/lyrica-mod-1.8.9), not in the original Lyrica.

## Download

1. Install [Forge](https://files.minecraftforge.net/net/minecraftforge/forge/index_1.8.9.html) 11.15.1.2318 for Minecraft 1.8.9.
2. Get the Lyrica JAR from [GitHub Releases](https://github.com/Alouuuu/lyrica-mod-1.8.9/releases/latest).
3. Place the JAR in your instance’s `mods` folder and launch Minecraft.

| Minecraft | Forge | Java | JAR suffix |
| --- | --- | --- | --- |
| 1.8.9 | 11.15.1.2318 | 8 | `-1.8.9.jar` |

The release JAR includes the native Windows x64 media bridge. For the Fabric versions (1.21.1, 1.21.11, 26.2, and 26.3), use the [original Lyrica](https://github.com/fakemaluco/lyrica-mod/releases/latest).

## Privacy

Enabled online lyrics providers receive track information such as title and artist to look up lyrics. You can manage those providers in **Sources**. Spotify account access and Gemini experiments are optional.

The port does not share your music with other players. When update checks are on, Lyrica asks this fork’s GitHub Releases whether a newer version exists.

See [Privacy and data controls](docs/PRIVACY.md) for settings and details.

## Documentation

- [User guide in Portuguese](docs/GUIDE.md)
- [Cinematic world lyrics](docs/cinematic-lyrics.md)
- [Project structure and build instructions](docs/DEVELOPMENT.md) for the Fabric versions
- Building the 1.8.9 port: run `gradlew build` in `legacy-1.8.9/` with JDKs 8, 17, and 21 installed. The JAR is written to `legacy-1.8.9/build/libs/`.
- [License](LICENSE) and [third-party notices](THIRD_PARTY_NOTICES.md)

Created by **Fakiz**. 1.8.9 port by **Alou_**. Inspired by [Spicy Lyrics](https://github.com/Spikerko/spicy-lyrics).
