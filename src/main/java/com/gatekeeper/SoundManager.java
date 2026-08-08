package com.gatekeeper;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

/** Small cached WAV player. Missing or unsupported audio never interrupts gameplay. */
public final class SoundManager {
    private static final long MUSIC_CROSSFADE_MICROS = 1_500_000L;
    private static final Map<String, List<String>> SCENE_SOUNDS = buildSceneSounds();
    private final Map<String, Clip> clips = new HashMap<>();
    private final Map<String, Float> clipVolumeMultipliers = new HashMap<>();
    private final Map<String, String> clipScenes = new HashMap<>();
    private final Map<String, Map<String, Float>> sceneVolumes = new HashMap<>();
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
        play(resourcePath, "GLOBAL", 1.0f);
    }

    public void play(String resourcePath, float volumeMultiplier) {
        play(resourcePath, "GLOBAL", volumeMultiplier);
    }

    public void play(String resourcePath, String sceneName, float volumeMultiplier) {
        Clip clip = clips.get(resourcePath);
        if (clip == null) {
            clip = load(resourcePath);
            if (clip == null) return;
            clips.put(resourcePath, clip);
        }
        float safeMultiplier = clampVolume(volumeMultiplier);
        clipVolumeMultipliers.put(resourcePath, safeMultiplier);
        clipScenes.put(resourcePath, sceneName);
        if (clip.isRunning()) clip.stop();
        clip.setFramePosition(0);
        setGain(clip, masterVolume * fxVolume * sceneVolume(sceneName, resourcePath) * safeMultiplier);
        clip.start();
    }

    public void close() {
        stopMusic();
        stopAmbient();
        for (Clip clip : clips.values()) clip.close();
        clips.clear();
        clipVolumeMultipliers.clear();
        clipScenes.clear();
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

    public static String[] soundScenes() {
        return SCENE_SOUNDS.keySet().toArray(new String[0]);
    }

    public static String[] sceneSounds(String sceneName) {
        List<String> sounds = SCENE_SOUNDS.get(sceneName);
        return sounds == null ? new String[0] : sounds.toArray(new String[0]);
    }

    public float sceneVolume(String sceneName, String resourcePath) {
        return sceneVolumes.computeIfAbsent(sceneName, ignored -> new HashMap<>())
            .getOrDefault(resourcePath, 1.0f);
    }

    public void setSceneVolume(String sceneName, String resourcePath, float volume) {
        sceneVolumes.computeIfAbsent(sceneName, ignored -> new HashMap<>())
            .put(resourcePath, clampVolume(volume));
        refreshVolumes();
    }

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
        for (Map.Entry<String, Clip> entry : clips.entrySet()) {
            setClipGain(entry.getKey(), entry.getValue());
        }
    }

    private void refreshVolumes() {
        if (music != null && fadingMusic == null) setGain(music, masterVolume * musicVolume);
        if (ambient != null) setGain(ambient, masterVolume * musicVolume);
        for (Map.Entry<String, Clip> entry : clips.entrySet()) {
            setClipGain(entry.getKey(), entry.getValue());
        }
    }

    private void setClipGain(String resourcePath, Clip clip) {
        String sceneName = clipScenes.getOrDefault(resourcePath, "GLOBAL");
        setGain(clip, masterVolume * fxVolume * sceneVolume(sceneName, resourcePath)
            * clipVolumeMultipliers.getOrDefault(resourcePath, 1.0f));
    }

    private static Map<String, List<String>> buildSceneSounds() {
        Map<String, List<String>> sounds = new LinkedHashMap<>();
        List<String> movement = Arrays.asList(
            "footstep-01.wav", "footstep-02.wav", "footstep-03.wav", "footstep-04.wav");
        sounds.put("TITLE", Arrays.asList("ui-open.wav", "ui-select.wav", "ui-confirm.wav",
            "ui-error.wav", "ui-click.wav"));
        sounds.put("CONTROLS", Collections.singletonList("ui-back.wav"));
        sounds.put("SETTINGS", Arrays.asList("ui-back.wav", "ui-select.wav", "ui-click.wav"));
        sounds.put("DEV", Arrays.asList("ui-open.wav", "ui-back.wav", "ui-select.wav",
            "ui-confirm.wav", "ui-click.wav"));
        sounds.put("BEDROOM", withMovement(movement, "ui-open.wav", "door-open.wav", "door-close.wav"));
        sounds.put("STREET", withMovement(movement, "ui-open.wav", "door-open.wav", "door-close.wav"));
        sounds.put("SHOP", withMovement(movement, "ui-open.wav", "door-open.wav", "door-close.wav"));
        sounds.put("BOARD", Arrays.asList("ui-open.wav", "ui-close.wav", "ui-select.wav", "ui-error.wav",
            "ui-confirm.wav", "ui-click.wav", "gate-place.wav", "switch.wav", "success.wav", "failure.wav"));
        sounds.put("NOTEBOOK", Arrays.asList("book-open.wav", "book-close.wav", "book-flip.wav", "ui-select.wav"));
        sounds.put("END", Collections.singletonList("ui-confirm.wav"));
        return Collections.unmodifiableMap(sounds);
    }

    private static List<String> withMovement(List<String> movement, String... sounds) {
        List<String> result = new java.util.ArrayList<>(movement);
        result.addAll(Arrays.asList(sounds));
        return Collections.unmodifiableList(result);
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
