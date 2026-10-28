<div align="center">

<img src="Logo.png" alt="BitChord app icon" width="160" />

# BitChord Linux

### A personal Linux desktop fork of BitChord, with working MPRIS

</div>

> [!IMPORTANT]
> **This is a personal, unofficial fork.** It is not affiliated with, endorsed by, or supported by the original BitChord author, by the BitChord Desktop — Revived maintainers, or by YouTube / Google. Please do not ask them for support with this fork.

> [!NOTE]
> **Heavily AI-assisted.** I'm a digital artist and Linux user, not a programmer. Most of the code changes in this fork were developed with a lot of help from an AI assistant (Claude), and I test them on my own machine (Fedora, KDE Plasma / Hyprland). Expect rough edges, and please read the code before trusting it.

---

## Why this fork exists

The upstream project dropped its native desktop app in favour of a web player, and the last releases no longer ship Linux packages (rpm, AppImage, etc.). The desktop app also had no working **MPRIS** support, so media keys and desktop widgets (for example the Plasma media widget) could not control it.

I wanted a native desktop version on Fedora, so I forked it and fixed the things that bothered me. I'm sharing it in case it is useful to someone else.

## What's different from upstream

**My own changes**

- **MPRIS working** on Linux (media widgets and keys), based on the upstream fix for MPRIS capability changes.
- **Lyrics no longer freeze** when the window loses focus (for example when it sits on a second monitor): the foreground check now uses `STARTED` instead of `RESUMED`.
- **Restore last track and queue on startup**: the app remembers the last played track and the queue (also when it came from a playlist) and reopens paused, starting the track from the beginning.
- **rpm and AppImage builds** for Fedora/Linux.

**Taken from other forks**

- The refreshed desktop interface and extra features come from **BitChord Desktop — Revived** (see credits): responsive sidebar, optional classic Explore cards, light mode polish, scroll fades, player dock changes and more.

> Everything under "taken from other forks" is the work of the respective authors. I only merged it and adapted it where it conflicted with my changes.


## Credits

This project would not exist without the work of others:

- **BitChord** by [@kushagrasinghx](https://github.com/kushagrasinghx) and its contributors: the original app this fork is built on. [Repository](https://github.com/kushagrasinghx/BitChord) · [Contributors](https://github.com/kushagrasinghx/BitChord/graphs/contributors) · [Support the original author on Ko-fi](https://ko-fi.com/kushagrasinghx)
- **BitChord Desktop — Revived** by [@Bntagaaa](https://github.com/Bntagaaa): the community continuation of the desktop app, source of the interface and features merged here. [Repository](https://github.com/Bntagaaa/BitChord-Desktop-Revived)
- **Lyrics animation** inspired by [am-lyrics](https://github.com/binimum/am-lyrics) by binimum, and the lyrics providers credited in the [upstream README](https://github.com/kushagrasinghx/BitChord/blob/main/README.md).
- The AI assistant that helped me write, debug and merge most of the changes.

For the full feature list and third-party acknowledgements, see the [upstream README](https://github.com/kushagrasinghx/BitChord/blob/main/README.md).

## Disclaimer

BitChord and this fork are independent third-party clients. They are **not** associated with Google LLC, YouTube, YouTube Music, Spotify or any other referenced service. They do not host or store copyrighted music. You are responsible for complying with your local laws and with the terms of service of the services you use. This software is provided as is, without warranty of any kind.

## License

This project keeps the original **GNU General Public License v3.0 (GPLv3)**. See the [LICENSE](LICENSE) file. Modified versions and redistributions must keep the license, the attribution notices and provide the corresponding source.
