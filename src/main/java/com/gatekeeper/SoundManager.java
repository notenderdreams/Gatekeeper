package com.gatekeeper;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import java.util.HashMap;
import java.util.Map;

/** Small cached WAV player. Missing or unsupported audio never interrupts gameplay. */
public final class SoundManager {
    private final Map<String, Clip> clips = new HashMap<>();
    private Clip music;
    private String musicPath;
    private boolean musicUnavailable;

    public void play(String resourcePath) {
        Clip clip = clips.get(resourcePath);
        if (clip == null) {
            clip = load(resourcePath);
            if (clip == null) return;
            clips.put(resourcePath, clip);
        }
        if (clip.isRunning()) clip.stop();
        clip.setFramePosition(0);
        clip.start();
    }

    public void close() {
        stopMusic();
        for (Clip clip : clips.values()) clip.close();
        clips.clear();
    }

    public void loop(String resourcePath) {
        if (musicUnavailable) return;
        if (music != null && resourcePath.equals(musicPath) && music.isRunning()) return;
        stopMusic();
        music = load(resourcePath);
        if (music == null) {
            musicUnavailable = true;
            return;
        }
        musicPath = resourcePath;
        music.loop(Clip.LOOP_CONTINUOUSLY);
    }

    public void stopMusic() {
        if (music != null) {
            music.stop();
            music.close();
            music = null;
            musicPath = null;
        }
    }

    private static Clip load(String resourcePath) {
        try (AudioInputStream stream = AudioSystem.getAudioInputStream(
                SoundManager.class.getResource(resourcePath))) {
            Clip clip = AudioSystem.getClip();
            clip.open(stream);
            return clip;
        } catch (Exception error) {
            return null;
        }
    }
}
