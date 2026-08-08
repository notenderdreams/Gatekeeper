package com.gatekeeper;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import java.util.HashMap;
import java.util.Map;

/** Small cached WAV player. Missing or unsupported audio never interrupts gameplay. */
public final class SoundManager {
    private static final long MUSIC_CROSSFADE_MICROS = 1_500_000L;
    private final Map<String, Clip> clips = new HashMap<>();
    private Clip music;
    private Clip fadingMusic;
    private String musicPath;
    private Clip ambient;
    private String ambientPath;
    private boolean musicUnavailable;
    private boolean ambientUnavailable;
    private float masterVolume = 1.0f;
    private float musicVolume = 0.75f;
    private float fxVolume = 0.85f;

    public void play(String resourcePath) {
        Clip clip = clips.get(resourcePath);
        if (clip == null) {
            clip = load(resourcePath);
            if (clip == null) return;
            clips.put(resourcePath, clip);
        }
        if (clip.isRunning()) clip.stop();
        clip.setFramePosition(0);
        setGain(clip, masterVolume * fxVolume);
        clip.start();
    }

    public void close() {
        stopMusic();
        stopAmbient();
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
        setGain(music, masterVolume * musicVolume);
        music.loop(Clip.LOOP_CONTINUOUSLY);
    }

    public void updateMusic() {
        if (music == null || fadingMusic != null || !music.isRunning()) return;
        long remaining = music.getMicrosecondLength() - music.getMicrosecondPosition();
        if (remaining <= 0 || remaining > MUSIC_CROSSFADE_MICROS) return;
        fadingMusic = load(musicPath);
        if (fadingMusic == null) return;
        setGain(fadingMusic, 0.0f);
        fadingMusic.loop(Clip.LOOP_CONTINUOUSLY);
        setGain(music, masterVolume * musicVolume);
        musicCrossfadeStart = System.nanoTime();
    }

    private long musicCrossfadeStart;

    public void updateCrossfade() {
        if (fadingMusic == null) return;
        float progress = Math.min(1.0f, (System.nanoTime() - musicCrossfadeStart) / 1_000_000_000.0f
            / (MUSIC_CROSSFADE_MICROS / 1_000_000.0f));
        float musicGain = masterVolume * musicVolume;
        setGain(music, musicGain * (1.0f - progress));
        setGain(fadingMusic, musicGain * progress);
        if (progress >= 1.0f) {
            music.stop();
            music.close();
            music = fadingMusic;
            fadingMusic = null;
        }
    }

    public void loopAmbient(String resourcePath) {
        if (ambientUnavailable) return;
        if (ambient != null && resourcePath.equals(ambientPath) && ambient.isRunning()) return;
        stopAmbient();
        ambient = load(resourcePath);
        if (ambient == null) {
            ambientUnavailable = true;
            return;
        }
        ambientPath = resourcePath;
        setGain(ambient, masterVolume * musicVolume);
        ambient.loop(Clip.LOOP_CONTINUOUSLY);
    }

    public float masterVolume() { return masterVolume; }
    public float musicVolume() { return musicVolume; }
    public float fxVolume() { return fxVolume; }

    public void setMasterVolume(float volume) {
        masterVolume = clampVolume(volume);
        refreshVolumes();
    }

    public void setMusicVolume(float volume) {
        musicVolume = clampVolume(volume);
        refreshVolumes();
    }

    public void setFxVolume(float volume) {
        fxVolume = clampVolume(volume);
        for (Clip clip : clips.values()) setGain(clip, masterVolume * fxVolume);
    }

    private void refreshVolumes() {
        if (music != null && fadingMusic == null) setGain(music, masterVolume * musicVolume);
        if (ambient != null) setGain(ambient, masterVolume * musicVolume);
        for (Clip clip : clips.values()) setGain(clip, masterVolume * fxVolume);
    }

    private static float clampVolume(float volume) {
        return Math.max(0.0f, Math.min(1.0f, volume));
    }

    public void stopAmbient() {
        if (ambient != null) {
            ambient.stop();
            ambient.close();
            ambient = null;
            ambientPath = null;
        }
    }

    public void stopMusic() {
        if (music != null) {
            music.stop();
            music.close();
            music = null;
            musicPath = null;
        }
        if (fadingMusic != null) {
            fadingMusic.stop();
            fadingMusic.close();
            fadingMusic = null;
        }
    }

    private static void setGain(Clip clip, float linearGain) {
        try {
            FloatControl gain = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
            float safeGain = Math.max(0.0001f, Math.min(1.0f, linearGain));
            float decibels = (float) (20.0 * Math.log10(safeGain));
            gain.setValue(Math.max(gain.getMinimum(), Math.min(gain.getMaximum(), decibels)));
        } catch (IllegalArgumentException ignored) {
            // Some platforms expose no master-gain control; playback still works.
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
