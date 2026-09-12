package com.gatekeeper;

import java.util.List;

public final class SoundManagerTest {
    public static void main(String[] args) {
        SoundManager sound = new SoundManager();

        // 1. Verify all configured music resources exist
        require(SoundManager.class.getResource(GameConstants.MENU_MUSIC) != null,
            "MENU_MUSIC resource must exist: " + GameConstants.MENU_MUSIC);
        require(SoundManager.class.getResource(GameConstants.ROAD_AMBIENCE) != null,
            "ROAD_AMBIENCE resource must exist: " + GameConstants.ROAD_AMBIENCE);

        for (String track : GameConstants.GAMEPLAY_PLAYLIST) {
            require(SoundManager.class.getResource(track) != null,
                "GAMEPLAY_PLAYLIST track resource must exist: " + track);
        }

        for (String track : GameConstants.ALL_MUSIC_TRACKS) {
            require(SoundManager.class.getResource(track) != null,
                "ALL_MUSIC_TRACKS resource must exist: " + track);
        }

        // 2. Track name formatting
        require("OVERNIGHT LOOP (MENU)".equals(GameConstants.musicTrackName(GameConstants.MENU_MUSIC)),
            "MENU_MUSIC should format as OVERNIGHT LOOP (MENU)");
        require("LANTERN RAIN".equals(GameConstants.musicTrackName("/assets/audio/music/lantern-rain.wav")),
            "lantern-rain should format as LANTERN RAIN");
        require("WORKBENCH HOURS".equals(GameConstants.musicTrackName("/assets/audio/music/workbench-hours.wav")),
            "workbench-hours should format as WORKBENCH HOURS");

        // 3. Initial state
        require(sound.musicMode() == null, "initial music mode should be null");
        require(sound.currentMusicPath() == null, "initial music path should be null");
        require(sound.playlistIndex() == 0, "initial playlist index should be 0");
        require(Math.abs(sound.musicVolume() - 0.40f) < 0.001f, "default music volume should be 0.40f");

        // 4. Set menu music mode
        sound.playMenuMusic(GameConstants.MENU_MUSIC);
        sound.updateMusic();
        require(sound.musicMode() == SoundManager.MusicMode.MENU,
            "mode should switch to MENU after updateMusic");
        require(sound.isFadingIn(), "menu music should fade in on startup");
        sound.loopAmbient(GameConstants.ROAD_AMBIENCE);

        // 5. Switch to playlist mode (going to play)
        sound.playGamePlaylist(GameConstants.GAMEPLAY_PLAYLIST);
        require(sound.playlistIndex() == 0, "playlist index should start at 0");
        sound.updateMusic();
        require(!sound.isFadingOut(),
            "menu music should not fade out when going to play; it should stop instantly");
        require(sound.musicMode() == SoundManager.MusicMode.PLAYLIST,
            "mode should switch to PLAYLIST immediately");

        // 6. Test transitioning from playlist to menu (fades out smoothly)
        sound.playMenuMusic(GameConstants.MENU_MUSIC);
        sound.updateMusic();
        require(sound.isFadingOut(),
            "transitioning from playlist to menu should fade out smoothly");

        // 7. Test stopMenuMusic
        sound.stopMusic();
        sound.playMenuMusic(GameConstants.MENU_MUSIC);
        sound.updateMusic();
        require(sound.musicMode() == SoundManager.MusicMode.MENU, "should switch to MENU");
        sound.stopMenuMusic();
        require(sound.currentMusicPath() == null, "stopMenuMusic should clear current track");
        require(!sound.isFadingOut(), "stopMenuMusic should not be fading out");

        // Reset to playlist mode for remaining tests
        sound.playGamePlaylist(GameConstants.GAMEPLAY_PLAYLIST);
        sound.updateMusic();

        // 8. Test dev track switching via switchToTrack
        String targetTrack = GameConstants.GAMEPLAY_PLAYLIST.get(2); // lantern-rain
        sound.switchToTrack(targetTrack);
        require(targetTrack.equals(sound.targetTrackPath()),
            "targetTrackPath should reflect the selected track");
        require(sound.playlistIndex() == 2,
            "playlistIndex should update to match the selected track index");
        require(sound.isFadingOut(),
            "dev track switching during gameplay should fade out smoothly");

        // 9. Volume clamping
        sound.setMasterVolume(-1.0f);
        require(sound.masterVolume() == 0.0f, "master volume should clamp to 0.0");
        sound.setMasterVolume(2.0f);
        require(sound.masterVolume() == 1.0f, "master volume should clamp to 1.0");

        sound.setMusicVolume(-0.5f);
        require(sound.musicVolume() == 0.0f, "music volume should clamp to 0.0");
        sound.setMusicVolume(1.5f);
        require(sound.musicVolume() == 1.0f, "music volume should clamp to 1.0");

        sound.setFxVolume(-0.5f);
        require(sound.fxVolume() == 0.0f, "fx volume should clamp to 0.0");
        sound.setFxVolume(1.5f);
        require(sound.fxVolume() == 1.0f, "fx volume should clamp to 1.0");

        // 10. Stop music
        sound.stopMusic();
        require(sound.musicMode() == null, "stopMusic should clear musicMode");
        require(sound.currentMusicPath() == null, "stopMusic should clear currentMusicPath");
        require(!sound.isFadingOut(), "stopMusic should clear isFadingOut");
        require(!sound.isFadingIn(), "stopMusic should clear isFadingIn");

        sound.close();
        System.out.println("SoundManagerTest: all checks passed");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
