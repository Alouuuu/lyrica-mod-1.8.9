"""One-shot importer of upstream Lyrica sources into the 1.8.9 port (hard fork).

Mirrors lyrics-popup/build.gradle.kts `prepareStandaloneClientJava`: copies the selected
files, renames the `fandroid` package to `relaylyrics`, and never overwrites a file that
already exists in the port (ported files are edited by hand afterwards).

Usage (from legacy-1.8.9/):  python -I tools/import_upstream.py <group> [<group> ...]
Groups: protocol, logic, client
"""
import pathlib
import sys

ROOT = pathlib.Path(__file__).resolve().parents[2]          # lyrica-mod/
DEST = ROOT / "legacy-1.8.9" / "src" / "main" / "java"

MOD_MAIN = ROOT / "mod/src/main/java/dev/fandroid/client/music"
MOD_CLIENT = ROOT / "mod/src/client/java/dev/fandroid/client"
POPUP_CLIENT = ROOT / "lyrics-popup/src/client/java/dev/relaylyrics/client"
PROTOCOL = ROOT / "shared-protocol/src/main/java/dev/fandroid/protocol"

LOGIC = """LyricsAnimation LyricsTiming PopupResizeGesture PopupSizeProfile PopupAlignment
PopupHudTargets KawarpRaster GlassButtonRaster ResponsiveLyricsLayout CanvasPopupLayout
AdaptivePopupLayout PopupDockLayout PopupDockMotion IslandAudioMotion IslandLyrics
IslandWaitingMotion RoundedVideoRaster UnavailableLyricsTransition LyricsEffectRaster
LyricsEffectCache LyricsShadowKernel LyricsRenderText LyricsRomanizer LyricsViewport
ManualLyricsScroll SpringValue WorldLyricsPlan WorldLyricsDirector WorldLyricsPlacement
WorldLyricsMotion WorldLyricsLayout WorldLyricsExperience WorldLyricsPresentation WorldLyricsStage
WorldLyricsRecovery WorldLyricsSpace WorldLyricsLights""".split()

# MVP client set (phase 1). World lyrics, karaoke, presence, Gemini, video, compat: out of scope.
CLIENT_MOD = """music/MediaBridgeConnector music/BundledMediaBridge music/WindowsBridgeInstaller
music/MusicService music/MusicPlaybackClock music/MultiSourceLyricsClient music/ImportedLyricsClient
music/SpicyLyricsApiClient music/CoverArtTexture music/PlaybackProgressTransition
music/LyricsTranslationSession music/AudioDucking music/PlatformTextures music/ReleaseSupport
music/SpotifyAccountClient music/YouTubeMusicCompanion
ui/LyricsOverlay ui/PopupIcons ui/LiquidGlass ui/LyricsBloomRenderer ui/GuiTransform
ui/PlatformTextStyles ui/RetroDraw ui/RoundedGui ui/FanDroidScreenBase ui/SettingsButtonBase
ui/HudSafeArea ui/PlatformCursor ui/SpotifyAccountScreen ui/YouTubeMusicConnectionScreen
LyricsEffectsMode LyricsFont LyricsTextAlignment LyricsFontWeight LyricsGlowIntensity
LyricsTransitionSpeed MusicSource LyricsRenderType CompatibilityManager GoogleFontManager
music/GeminiShaderSession music/GeminiShaderClient music/GeminiShaderSpec music/GeminiShaderStore
music/GeminiLyricsSession music/GeminiLyricsClient music/GeminiLyricsSpec music/GeminiLyricsStore
music/GeminiArtLibrary ui/GoogleFontScreen""".split()

CLIENT_POPUP = """ClientPreferences LyricsSettingsCatalog LyricsSettingsDemo LyricsSettingsPresets
SungTextColor ui/LyricsConfigScreen ui/SettingsColor ui/SettingsIconRaster ui/SettingsListMotion
ui/SettingsMotion ui/SettingsTextRaster ui/SettingsTypography ui/LyricsGlyphRasterizer
InventoryLyricsController InventoryPopupTransition InventoryOverlayCompat ui/PopupAlignmentGuides""".split()


def rename(text: str) -> str:
    return text.replace("fandroid", "relaylyrics")


def copy(src: pathlib.Path, rel: str) -> None:
    out = DEST / rename(rel)
    if out.exists():
        print("skip (exists)", out.relative_to(DEST))
        return
    if not src.exists():
        print("MISSING", src)
        return
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_text(rename(src.read_text(encoding="utf-8")), encoding="utf-8", newline="\n")
    print("copied", out.relative_to(DEST))


def main(groups):
    if "protocol" in groups:
        copy(PROTOCOL / "ProtocolCodec.java", "dev/fandroid/protocol/ProtocolCodec.java")
        for sub in ("lyrics", "music"):
            for f in sorted((PROTOCOL / sub).glob("*.java")):
                copy(f, f"dev/fandroid/protocol/{sub}/{f.name}")
    if "logic" in groups:
        for name in LOGIC:
            copy(MOD_MAIN / f"{name}.java", f"dev/fandroid/client/music/{name}.java")
    if "client" in groups:
        for name in CLIENT_MOD:
            copy(MOD_CLIENT / f"{name}.java", f"dev/fandroid/client/{name}.java")
        for name in CLIENT_POPUP:
            copy(POPUP_CLIENT / f"{name}.java", f"dev/relaylyrics/client/{name}.java")


if __name__ == "__main__":
    main(set(sys.argv[1:]))
