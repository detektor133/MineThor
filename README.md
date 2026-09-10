<h1 align="center">Angel Aura Amethyst</h1>

<img src="https://github.com/AngelAuraMC/Amethyst-Android/blob/v3_openjdk/app_pojavlauncher/src/main/assets/amethyst.png" align="left" width="130" height="130" alt="Amethyst logo">

[![Android CI](https://github.com/AngelAuraMC/Amethyst-Android/workflows/Android%20CI/badge.svg)](https://github.com/AngelAuraMC/Amethyst-Android/actions)
[![GitHub commit activity](https://img.shields.io/github/commit-activity/m/AngelAuraMC/Amethyst-Android)](https://github.com/AngelAuraMC/Amethyst-Android/actions)
[![Crowdin](https://badges.crowdin.net/pojavlauncher/localized.svg)](https://crowdin.com/project/pojavlauncher)
[![Discord](https://img.shields.io/discord/724163890803638273.svg?label=&logo=discord&logoColor=ffffff&color=7389D8&labelColor=6A7EC2)](https://discord.gg/5ptqkyZxEy)

*From [Boardwalk](https://github.com/zhuowei/Boardwalk)'s ashes and [PojavLauncher](https://github.com/PojavLauncherTeam/PojavLauncher)'s ruined reputation, here comes Amethyst!*

Amethyst is a launcher that allows you to play Minecraft: Java Edition on your Android and [iOS](https://github.com/AngelAuraMC/Amethyst-iOS) devices.

For more details, check out our [wiki](https://wiki.angelauramc.dev)!

> **This fork** adds **MineThor**, a companion HUD overlay (inspired by the AYN Thor handheld's companion screen) that mirrors live Minecraft state — player position, health, hunger, XP, and inventory with rendered item icons — on top of the game while it runs. See [MineThor](#minethor-companion-hud) below for how it works and its current limitations.

## Table of Contents

* [Introduction](#introduction)
* [Getting Amethyst](#getting-amethyst)
* [Building](#building)
    * [Quick Build (Recommended)](#quick-build-recommended)
    * [Detailed Build](#detailed-build)
* [MineThor Companion HUD](#minethor-companion-hud)
* [Current Status](#current-status)
* [Known Issues](#known-issues)
* [FAQ](#faq)
* [Contributing](#contributing)
* [Support](#support)
* [License](#license)
* [Credits & Dependencies](#credits--dependencies)
* [Roadmap](#roadmap)

## Introduction

* Amethyst is a Minecraft: Java Edition launcher for Android and iOS based on [Boardwalk](https://github.com/zhuowei/Boardwalk) and [PojavLauncher](https://github.com/PojavLauncherTeam/PojavLauncher)
* This launcher can launch almost all available Minecraft versions ranging from rd-132211 to 1.21 snapshots (including Combat Test versions)
* Modding via Forge and Fabric are also supported.
* This repository contains source code for Android. For iOS/iPadOS, check out [Amethyst-iOS](https://github.com/AngelAuraMC/Amethyst-iOS).

## Getting Amethyst

You can get Amethyst via two methods:

1. **Releases:** Download the latest prebuilt app from [nightly.link](https://nightly.link/AngelAuraMC/Amethyst-Android/workflows/android/v3_openjdk/app-debug%20%28recommended%29.zip) or select an older version from our [automatic builds](https://github.com/AngelAuraMC/Amethyst-Android/actions).
2. **Build from Source:** Follow the [building instructions](#building) below.

## Building

### Quick Build (Recommended)

The easiest way to build Amethyst is to use the pre-built JREs provided by our CI.

1. Clone the repository: `git clone --recursive https://github.com/AngelAuraMC/Amethyst-Android.git`
2. Build the launcher: `./gradlew :app_pojavlauncher:assembleDebug` (Use `gradlew.bat` on Windows)

The built APK will be located in `app_pojavlauncher/build/outputs/apk/debug/`.

### Detailed Build

If you need more control over the build process, follow these steps:

1. **Java Runtime Environment (JRE):** Download the `jre8-pojav` artifact from our [CI auto builds](https://github.com/AngelAuraMC/openjdk-build-multiarch/actions).  This package contains pre-built JREs for all supported architectures.  If you need to build the JRE yourself, follow the instructions in the [android-openjdk-build-multiarch](https://github.com/AngelAuraMC/openjdk-build-multiarch) repository.

2. **LWJGL:** The build instructions for the custom LWJGL are available over the [LWJGL repository](https://github.com/AngelAuraMC/lwjgl3).

3. **Language List:** Because languages are auto-added by Crowdin, you need to run the language list generator before building. In the project directory, run:
   * Linux/macOS:
     ```bash
     chmod +x scripts/languagelist_updater.sh
     bash scripts/languagelist_updater.sh
     ```
   * Windows:
     ```batch
     scripts\languagelist_updater.bat
     ```

4. **Build GLFW stub:** `./gradlew :jre_lwjgl3glfw:build`

5. **Build the launcher:** `./gradlew :app_pojavlauncher:assembleDebug` (Replace `gradlew` with `gradlew.bat` on Windows).

## MineThor Companion HUD

MineThor is this fork's addition: a live HUD overlay that shows the running Minecraft game's player state (position, health, hunger, armor, XP, day time, held/inventory items with rendered icons) on top of the game surface, similar to a handheld console's companion screen.

**How it works:**

* A small Java agent (`methods_injector_agent/.../minethor`) is injected into the launched Minecraft JVM. It uses reflection to read live player/world state off the game's own objects (`ReflectiveMinecraftAdapter`), renders item icons on the Minecraft render thread (`MinecraftIconRenderer`), and serves both over a local loopback socket (`MineThorBridgeServer`).
* The Android launcher (`app_pojavlauncher/.../minethor`) connects to that socket, decodes the protocol (`CompanionProtocolCodec`), and draws the HUD (`ThorHudView`) using textures pulled from the running Minecraft instance's own assets (`MineThorMinecraftAssets`) plus a bitmap font (`MineThorBitmapFont`).
* Because Minecraft has no public/stable API for this, MineThor resolves the correct (obfuscated) method and field names for whatever version is launched by downloading Mojang's official `client_mappings` for that version and building a name descriptor from it at launch time (`MineThorMappingResolver`), rather than hardcoding names for one version. A fully hardcoded fallback table (`MappedMinecraftAdapter`) is used only if that dynamic resolution fails.

**Known limitations:**

* The icon-rendering path (`MinecraftIconRenderer`) calls into `GuiGraphics`, which only exists from Minecraft 1.20 onward — versions before 1.20 used a different (`PoseStack`-based) rendering pipeline, so item icon rendering does not work pre-1.20. Player state (position/health/inventory counts/etc.) does not depend on `GuiGraphics` and is not subject to this limit.
* Mapping resolution degrades gracefully per-property now (one shifted method signature no longer invalidates the entire descriptor), but a from-scratch legacy render path for pre-1.20 icons hasn't been built yet.
* Textures are now bound to the specific version being launched (`MineThorMinecraftAssets.setActiveVersion`) and re-resolved on every launch, rather than being cached for the life of the launcher process.

## Current Status

* [x] OpenJDK 8 Mobile port: ARM32, ARM64, x86, x86_64
* [x] OpenJDK 17 Mobile port: ARM32, ARM64, x86, x86_64
* [x] OpenJDK 21 Mobile port: ARM32, ARM64, x86, x86_64
* [x] Headless mod installer
* [x] Mod installer with GUI
* [x] OpenGL in OpenJDK environment
* [x] OpenAL (works on most devices)
* [x] Support for Minecraft 1.12.2 and below
* [x] Support for Minecraft 1.13 and above
* [x] Support for Minecraft 1.17 (22w13a) and above
* [x] Game surface zooming
* [x] New input pipe rewritten to native code
* [x] Rewritten entire controls system
* [ ] More to come!

## Known Issues

For upstream launcher issues, see the [Amethyst issue tracker](https://github.com/AngelAuraMC/Amethyst-Android/issues).

MineThor-specific:

* Item icon rendering requires Minecraft 1.20+ (see [Known limitations](#minethor-companion-hud) above); player/world state HUD elements work on any version with official Mojang mappings.
* The bridge agent reads live game state from the socket-reading thread without hopping onto Minecraft's client/render thread, so a state read racing a concurrent inventory mutation can occasionally surface as a one-tick "disconnected" HUD flicker rather than a crash.

This is a first published pass of this fork — expect rough edges, and issues/PRs are welcome.

## FAQ

See our [wiki](https://wiki.angelauramc.dev/) for more information.

## Contributing

Contributions are welcome! We welcome any type of contribution, not only code. For example, you can help improve the [wiki](https://github.com/AngelAuraMC/angelauramc.github.io/), contribute to the [translations](https://crowdin.com/project/pojavlauncher), or submit bug reports and feature requests.

Any code change should be submitted as a pull request. The description should explain what the code does and give steps to execute it.

## Support

For support, please join our [Discord server](https://discord.gg/5ptqkyZxEy).

## License

Amethyst is licensed under [GNU LGPLv3](https://github.com/AngelAuraMC/Amethyst-Android/blob/v3_openjdk/LICENSE).

## Credits & Dependencies

* [Boardwalk](https://github.com/zhuowei/Boardwalk) (JVM Launcher): Unknown License/[Apache License 2.0](https://github.com/zhuowei/Boardwalk/blob/master/LICENSE) or GNU GPLv2.
* [PojavLauncher](https://github.com/PojavLauncherTeam/PojavLauncher): [GLGPL](https://github.com/PojavLauncherTeam/PojavLauncher/blob/v3_openjdk/LICENSE)
* Android Support Libraries: [Apache License 2.0](https://android.googlesource.com/platform/prebuilts/maven_repo/android/+/master/NOTICE.txt).
* [GL4ES](https://github.com/AngelAuraMC/gl4es): [MIT License](https://github.com/ptitSeb/gl4es/blob/master/LICENSE).
* [MobileGlues](https://github.com/MobileGL-Dev/MobileGlues): [LGPL-2.1 License](https://github.com/MobileGL-Dev/MobileGlues/blob/dev-es/LICENSE).
* [Krypton Wrapper](https://github.com/BZLZHH/NG-GL4ES): [MIT License](https://github.com/BZLZHH/NG-GL4ES/blob/main/LICENSE)
* [ANGLE](https://chromium.googlesource.com/angle/angle): [All Rights Reserved](app_pojavlauncher/src/main/assets/licenses/ANGLE_LICENSE).
* [OpenJDK](https://github.com/AngelAuraMC/openjdk-multiarch-jdk8u): [GNU GPLv2 License](https://openjdk.java.net/legal/gplv2+ce.html).
* [LWJGL3](https://github.com/AngelAuraMC/lwjgl3): [BSD-3 License](https://github.com/LWJGL/lwjgl3/blob/master/LICENSE.md).
* [LWJGLX](https://github.com/AngelAuraMC/lwjglx) (LWJGL2 API compatibility layer for LWJGL3): unknown license.
* [Mesa 3D Graphics Library](https://gitlab.freedesktop.org/mesa/mesa): [MIT License](https://docs.mesa3d.org/license.html).
* [bhook](https://github.com/bytedance/bhook) (Used for exit code trapping): [MIT license](https://github.com/bytedance/bhook/blob/main/LICENSE).
* [libepoxy](https://github.com/anholt/libepoxy): [MIT License](https://github.com/anholt/libepoxy/blob/master/COPYING).
* [virglrenderer](https://github.com/AngelAuraMC/virglrenderer): [MIT License](https://gitlab.freedesktop.org/virgl/virglrenderer/-/blob/master/COPYING).
* [OpenAL-Soft](https://github.com/kcat/openal-soft): [GNU GPLv2](app_pojavlauncher/src/main/assets/licenses/OPENAL-SOFT_GPL2)
  * [oboe](https://github.com/google/oboe): [Apache License 2.0](app_pojavlauncher/src/main/assets/licenses/OBOE_APACHE2).
  * [pfffft](https://bitbucket.org/jpommier/pffft/src/master/): [ARR](app_pojavlauncher/src/main/assets/licenses/PFFFT_LICENSE)
* [SDL3](https://github.com/libsdl-org/SDL): [zlib License](https://github.com/libsdl-org/SDL/blob/main/LICENSE.txt)
* [sdl2-compat](https://github.com/libsdl-org/sdl2-compat): [zlib License](https://github.com/libsdl-org/sdl2-compat/blob/main/LICENSE.txt)
* Thanks to [MCHeads](https://mc-heads.net) for providing Minecraft avatars.

## Roadmap

We are currently focusing on:

* Exploring new rendering technologies.

Future plans include:

* Improving stability and performance.
* Enhancing the mod installation experience.

We welcome community feedback and suggestions for our roadmap.  Please feel free to open a feature request in our [issue tracker](https://github.com/AngelAuraMC/Amethyst-Android/issues).
