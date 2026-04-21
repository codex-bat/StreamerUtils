# StreamerUtils

![StreamerUtils logo](https://cdn.modrinth.com/data/placeholder.png)

**StreamerUtils** is a server‑side Fabric mod that enhances Minecraft chat for streamers and their communities. It adds **custom nameplate icons, coloured usernames, Twitch integration, follow alerts, sound effects, and some more** – all fully configurable per player.

---

## ✨ Features

- **Custom nameplate icons** – Choose from Twitch, YouTube, Kick, heart, crown, and more (pixel art made by me)
- **Coloured usernames** – Just the 16 official Minecraft colors
- **Twitch integration** – Connect your Twitch account to show live status, handle `!highlight` and `!firework` commands, and receive follow alerts with custom sounds
- **Follow sound alerts** – Configurable per player: none, personal (your own followers), global (any streamer’s followers), or both
- **Global brackets** – Server operators can change the brackets around player names (e.g., `[Player]` → `{Player}`)
- **Toggleable welcome message** – Explains the mod and provides clickable documentation/download links
- **Short prefix mode** – Switch between `[SU]` and `[StreamerUtils]` in chat
- **Per‑player settings** – All preferences are saved and survive server restarts
- **Fully open source** – Licensed under GPL‑3.0‑or‑later

---

## 📥 Installation

1. **Install Fabric Loader** (version 0.14.0 or later) – [instructions here](https://fabricmc.net/use/)
2. **Download the mod JAR** from [Modrinth](https://modrinth.com/mod/streamerutils) or [GitHub](https://github.com/codex-bat/StreamerUtils)
3. Place the JAR in your server’s `mods` folder (client‑side installation is optional – only needed if you want to see the custom icons)
4. Start the server – the mod will generate default configs
5. (Optional) Install the **companion resource pack** (link below) to see all custom icons

---

## 🔧 Commands

Use `/su help` in‑game for a full list. Main commands:

| Command | Description |
|---------|-------------|
| `/su` | Show your current settings |
| `/su icon set <icon>` | Change your chat icon (twitch, youtube, kick, heart, crown, none) |
| `/su color <color>` | Change your name colour (e.g. `red`, `#FFAA00`) |
| `/su stream start/stop` | Toggle “Live” status (changes nameplate and enables Twitch alerts) |
| `/su soundalert mode <mode>` | Choose who triggers follow sounds |
| `/su twitch setup <channel> <bot> <oauth> <client_id>` | Connect your Twitch account (see [Twitch Setup Guide](#twitch-setup)) |
| `/su bracket set <left> [right]` | (OP only) Change global brackets |

---

## 📚 Documentation & Help

Full documentation is available in the [GitHub Wiki](https://github.com/codex-bat/StreamerUtils/wiki) or the `docs/` folder in the repository. It includes:

- Detailed command reference
- Twitch OAuth token generation guide
- Configuration file explanation
- How to create custom icons (resource pack format)

**Quick links:**

[![Modrinth](https://img.shields.io/badge/Download-Modrinth-1bd96a?style=for-the-badge&logo=modrinth)](https://modrinth.com/mod/streamerutils)
[![GitHub](https://img.shields.io/badge/Download-GitHub-lightgrey?style=for-the-badge&logo=github)](https://github.com/codex-bat/StreamerUtils)

*Companion resource pack (recommended):* [Download here](https://assets.punkord.com/StreamerUtils_Release.zip)

---

## 🔧 Building from Source

```bash
git clone https://github.com/Codexbat/streamerutils.git
cd streamerutils
./gradlew build
```
The compiled JAR will be in build/libs/.

--- 

## 🧪 Planned Features

- More platform icons (TikTok, Discord, etc.)
- Twitch chat relay to Minecraft (optional)
- Web dashboard for configuration
- Compatibility with Minecraft 1.21+

---

## 📜 License

This project is licensed under the **GPL‑3.0‑or‑later**.
<br>You are free to use, modify, and distribute it as long as you comply with the license terms.
See the [LICENSE](LICENSE) file for details.

**Third‑party licenses:**
<br>This mod includes the MIT‑licensed [Twitch4J‑fabric](https://github.com/twitch4j/twitch4j-fabric) and Apache 2.0 licensed [commons-logging](https://commons.apache.org/proper/commons-logging/changes.html#a1.2). Their license notices are included in the licenses/ folder and NOTICE.txt.

---

## 💬 Contact & Support

- **Discord**: [Join the community](https://codexbat.dev/discord)
- **GitHub Issues**: [Report bugs or request features](https:github.com/codex-bat/StreamerUtils/issues)
- **Modrinth**: [Project Frontpage](https://modrinth.com/mod/streamerutils)

---

## 🙏 Acknowledgements

- **Philipp Heuer** and **Awakened Redstone** – Twitch4J library
- Fabric community – Fabric API and Loom
- All testers and contributors 

_Made with ❤️ by [Codex.bat](https://github.com/codex-bat)_
