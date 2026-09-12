package com.gatekeeper;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineListener;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/** Small cached WAV player. Missing or unsupported audio never interrupts gameplay. */
public final class SoundManager {
    public enum MusicMode {
        MENU, PLAYLIST
    }

    private static final long FADE_OUT_MICROS = 5_000_000L;
    private static final long FADE_IN_MICROS = 4_000_000L;
    private static final float AMBIENT_GAIN_MULTIPLIER = 2.5f;
    private static final Map<String, List<String>> SCENE_SOUNDS = buildSceneSounds();
    private final Map<String, Clip> clips = new HashMap<>();
    private final Map<String, Float> clipVolumeMultipliers = new HashMap<>();
    private final Map<String, String> clipScenes = new HashMap<>();
    private final Map<String, Map<String, Float>> sceneVolumes = new HashMap<>();
    private final ExecutorService previewExecutor = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "gatekeeper-audio-preview");
        thread.setDaemon(true);
        return thread;
    });
    private final AtomicLong previewSequence = new AtomicLong();
    private final AtomicLong activePreviewRequest = new AtomicLong();
    private final AtomicReference<PreviewRequest> pendingPreview = new AtomicReference<>();
    private final AtomicBoolean previewWorkerScheduled = new AtomicBoolean();
    private MusicMode currentMode;
    private MusicMode desiredMode;
    private String menuTrackPath;
    private List<String> playlist = Collections.emptyList();
    private int playlistIndex;
    private Clip music;
    private String musicPath;
    private String targetTrackPath;
    private float fadeMultiplier = 1.0f;
    private boolean isFadingOut;
    private boolean isFadingIn;
    private long fadeStartTimeNano;
    private long fadeDurationNanos;
    private float fadeStartMultiplier = 1.0f;
    private Clip ambient;
    private String ambientPath;
    private Clip preview;
    private String previewPath;
    private LineListener previewListener;
    private volatile boolean previewing;
    private boolean musicMuted;
    private boolean musicUnavailable;
    private boolean ambientUnavailable;
    private float masterVolume = 1.0f;
    private float musicVolume = 0.40f;
    private float fxVolume = 0.85f;

    public void play(String resourcePath) {
        play(resourcePath, "GLOBAL", 1.0f);
    }

    public void play(String resourcePath, float volumeMultiplier) {
        play(resourcePath, "GLOBAL", volumeMultiplier);
    }

    public void play(String resourcePath, String sceneName, float volumeMultiplier) {
        if (previewing) return;
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
        setGain(clip, gameplayGain(masterVolume * fxVolume * sceneVolume(sceneName, resourcePath)
            * safeMultiplier));
        clip.start();
    }

    /** Loads editor previews off the UI thread and isolates them from gameplay audio. */
    public void preview(String resourcePath, String sceneName) {
        long requestId = previewSequence.incrementAndGet();
        activePreviewRequest.set(requestId);
        previewing = true;
        float previewGain = masterVolume * fxVolume * sceneVolume(sceneName, resourcePath);
        pendingPreview.set(new PreviewRequest(requestId, resourcePath, previewGain));
        schedulePreviewWorker();
    }

    public void close() {
        activePreviewRequest.set(0L);
        pendingPreview.set(null);
        previewing = false;
        stopMusic();
        stopAmbient();
        stopPreview();
        previewExecutor.shutdownNow();
        for (Clip clip : clips.values()) clip.close();
        clips.clear();
        clipVolumeMultipliers.clear();
        clipScenes.clear();
    }

    private void stopPreview() {
        if (preview != null) {
            if (previewListener != null) preview.removeLineListener(previewListener);
            preview.stop();
            preview.close();
            preview = null;
        }
        previewPath = null;
        previewListener = null;
    }

    private void schedulePreviewWorker() {
        if (previewWorkerScheduled.compareAndSet(false, true)) {
            previewExecutor.execute(this::drainPreviewRequests);
        }
    }

    private void drainPreviewRequests() {
        try {
            PreviewRequest request;
            while ((request = pendingPreview.getAndSet(null)) != null) {
                muteGameplayAudio();
                if (preview != null && request.resourcePath().equals(previewPath)) {
                    if (previewListener != null) preview.removeLineListener(previewListener);
                    previewListener = null;
                    if (preview.isRunning()) preview.stop();
                    preview.setFramePosition(0);
                } else {
                    stopPreview();
                    preview = load(request.resourcePath());
                    if (preview == null) {
                        finishPreview(request.id(), null);
                        continue;
                    }
                    previewPath = request.resourcePath();
                }
                if (request.id() != activePreviewRequest.get() || pendingPreview.get() != null) {
                    continue;
                }
                setGain(preview, request.gain());
                PreviewRequest startedRequest = request;
                Clip startedPreview = preview;
                previewListener = event -> {
                    if (event.getType() == javax.sound.sampled.LineEvent.Type.STOP) {
                        previewExecutor.execute(() -> finishPreview(startedRequest.id(), startedPreview));
                    }
                };
                preview.addLineListener(previewListener);
                preview.start();
            }
        } finally {
            previewWorkerScheduled.set(false);
            if (pendingPreview.get() != null) schedulePreviewWorker();
        }
    }

    private void finishPreview(long requestId, Clip finishedPreview) {
        if (requestId != activePreviewRequest.get()) return;
        if (finishedPreview != null && preview == finishedPreview) {
            if (previewListener != null) finishedPreview.removeLineListener(previewListener);
            previewListener = null;
        }
        if (activePreviewRequest.compareAndSet(requestId, 0L)) {
            previewing = activePreviewRequest.get() != 0L;
            if (!previewing) refreshVolumes();
        }
    }

    private record PreviewRequest(long id, String resourcePath, float gain) {}

    public void playMenuMusic(String resourcePath) {
        if (resourcePath == null) return;
        this.menuTrackPath = resourcePath;
        this.desiredMode = MusicMode.MENU;
    }

    public void playGamePlaylist(List<String> tracks) {
        if (tracks == null || tracks.isEmpty()) return;
        if (!this.playlist.equals(tracks)) {
            this.playlist = List.copyOf(tracks);
        }
        this.desiredMode = MusicMode.PLAYLIST;
    }

    public void loop(String resourcePath) {
        playMenuMusic(resourcePath);
    }

    public void stopMenuMusic() {
        if (currentMode == MusicMode.MENU || desiredMode == MusicMode.MENU) {
            stopAmbient();
            if (music != null) {
                music.stop();
                music.close();
                music = null;
                musicPath = null;
            }
            isFadingOut = false;
            isFadingIn = false;
            fadeMultiplier = 0.0f;
        }
    }

    public void updateMusic() {
        if (previewing || musicUnavailable || desiredMode == null) return;

        if (desiredMode != currentMode) {
            if (currentMode == MusicMode.MENU && desiredMode == MusicMode.PLAYLIST) {
                stopMenuMusic();
                currentMode = desiredMode;
            } else if (music != null && !isFadingOut) {
                beginFadeOut(FADE_OUT_MICROS);
            }
        }

        if (isFadingOut) {
            float progress = Math.min(1.0f, (System.nanoTime() - fadeStartTimeNano) / (float) fadeDurationNanos);
            fadeMultiplier = Math.max(0.0f, fadeStartMultiplier * (1.0f - progress));
            setGain(music, musicGain(masterVolume * musicVolume * fadeMultiplier));
            if (ambient != null && currentMode == MusicMode.MENU) {
                setGain(ambient, ambientGain(masterVolume * musicVolume * fadeMultiplier));
            }
            if (progress >= 1.0f) {
                music.stop();
                music.close();
                music = null;
                if (currentMode == MusicMode.MENU) {
                    stopAmbient();
                }
                isFadingOut = false;
                fadeMultiplier = 0.0f;
            }
            return;
        }

        if (isFadingIn) {
            float progress = Math.min(1.0f, (System.nanoTime() - fadeStartTimeNano) / (float) fadeDurationNanos);
            fadeMultiplier = progress;
            setGain(music, musicGain(masterVolume * musicVolume * fadeMultiplier));
            if (ambient != null && currentMode == MusicMode.MENU) {
                setGain(ambient, ambientGain(masterVolume * musicVolume * fadeMultiplier));
            }
            if (progress >= 1.0f) {
                isFadingIn = false;
                fadeMultiplier = 1.0f;
            }
            return;
        }

        if (music == null) {
            String nextPath = selectNextTrackPath();
            if (nextPath == null) return;
            music = load(nextPath);
            if (music == null) {
                if (desiredMode == MusicMode.PLAYLIST && !playlist.isEmpty()) {
                    playlistIndex = (playlistIndex + 1) % playlist.size();
                }
                return;
            }
            musicPath = nextPath;
            currentMode = desiredMode;
            beginFadeIn(FADE_IN_MICROS);
            music.setFramePosition(0);
            music.start();
            return;
        }

        long length = music.getMicrosecondLength();
        long pos = music.getMicrosecondPosition();
        long remaining = length - pos;

        if (remaining <= 0 || !music.isRunning()) {
            music.stop();
            music.close();
            music = null;
            fadeMultiplier = 0.0f;
            if (currentMode == MusicMode.PLAYLIST && !playlist.isEmpty()) {
                playlistIndex = (playlistIndex + 1) % playlist.size();
            }
        } else if (remaining <= FADE_OUT_MICROS) {
            fadeMultiplier = Math.max(0.0f, remaining / (float) FADE_OUT_MICROS);
            setGain(music, musicGain(masterVolume * musicVolume * fadeMultiplier));
        } else {
            fadeMultiplier = 1.0f;
            setGain(music, musicGain(masterVolume * musicVolume));
        }
    }

    public void updateCrossfade() {
        // Handled within updateMusic()
    }

    public void switchToTrack(String resourcePath) {
        if (resourcePath == null) return;
        if (resourcePath.equals(musicPath) && !isFadingOut) return;
        targetTrackPath = resourcePath;
        if (playlist.contains(resourcePath)) {
            playlistIndex = playlist.indexOf(resourcePath);
        }
        if (music != null) {
            beginFadeOut(FADE_OUT_MICROS);
        }
    }

    public MusicMode musicMode() { return currentMode; }
    public String currentMusicPath() { return musicPath; }
    public String targetTrackPath() { return targetTrackPath != null ? targetTrackPath : musicPath; }
    public boolean isFadingOut() { return isFadingOut; }
    public boolean isFadingIn() { return isFadingIn; }
    public int playlistIndex() { return playlistIndex; }

    private String selectNextTrackPath() {
        if (targetTrackPath != null) {
            String target = targetTrackPath;
            targetTrackPath = null;
            return target;
        }
        if (desiredMode == MusicMode.MENU) {
            return menuTrackPath;
        }
        if (playlist.isEmpty()) return null;
        if (playlistIndex < 0 || playlistIndex >= playlist.size()) {
            playlistIndex = 0;
        }
        return playlist.get(playlistIndex);
    }

    private void beginFadeOut(long durationMicros) {
        isFadingOut = true;
        isFadingIn = false;
        fadeStartTimeNano = System.nanoTime();
        fadeDurationNanos = Math.max(1_000_000L, durationMicros * 1000L);
        fadeStartMultiplier = fadeMultiplier;
    }

    private void beginFadeIn(long durationMicros) {
        isFadingIn = true;
        isFadingOut = false;
        fadeStartTimeNano = System.nanoTime();
        fadeDurationNanos = Math.max(1_000_000L, durationMicros * 1000L);
        fadeMultiplier = 0.0f;
        if (music != null) {
            setGain(music, musicGain(masterVolume * musicVolume * fadeMultiplier));
        }
        if (ambient != null && currentMode == MusicMode.MENU) {
            setGain(ambient, ambientGain(masterVolume * musicVolume * fadeMultiplier));
        }
    }

    public void loopAmbient(String resourcePath) {
        if (previewing) return;
        if (ambientUnavailable) return;
        if (ambient != null && resourcePath.equals(ambientPath) && ambient.isRunning()) return;
        stopAmbient();
        ambient = load(resourcePath);
        if (ambient == null) {
            ambientUnavailable = true;
            return;
        }
        ambientPath = resourcePath;
        float currentFade = (currentMode == MusicMode.MENU && (isFadingIn || isFadingOut)) ? fadeMultiplier : 1.0f;
        setGain(ambient, ambientGain(masterVolume * musicVolume * currentFade));
        ambient.loop(Clip.LOOP_CONTINUOUSLY);
    }

    public float masterVolume() { return masterVolume; }
    public float musicVolume() { return musicVolume; }
    public float fxVolume() { return fxVolume; }

    public void setMusicMuted(boolean muted) {
        if (musicMuted == muted) return;
        musicMuted = muted;
        if (music != null) setGain(music, musicGain(masterVolume * musicVolume * fadeMultiplier));
    }

    public static String[] soundScenes() {
        return SCENE_SOUNDS.keySet().toArray(new String[0]);
    }

    public static String[] sceneSounds(String sceneName) {
        List<String> sounds = SCENE_SOUNDS.get(sceneName);
        return sounds == null ? new String[0] : sounds.toArray(new String[0]);
    }

    private static float defaultSoundVolume(String resourcePath) {
        if (resourcePath == null) return 1.0f;
        if (resourcePath.endsWith("ui-open.wav")) return 0.30f;
        if (resourcePath.endsWith("ui-confirm.wav")) return 0.50f;
        return 1.0f;
    }

    public float sceneVolume(String sceneName, String resourcePath) {
        return sceneVolumes.computeIfAbsent(sceneName, ignored -> new HashMap<>())
            .getOrDefault(resourcePath, defaultSoundVolume(resourcePath));
    }

    public void setSceneVolume(String sceneName, String resourcePath, float volume) {
        sceneVolumes.computeIfAbsent(sceneName, ignored -> new HashMap<>())
            .put(resourcePath, clampVolume(volume));
    }

    public Map<String, Map<String, Float>> modifiedSceneVolumes() {
        Map<String, Map<String, Float>> modified = new LinkedHashMap<>();
        for (Map.Entry<String, Map<String, Float>> sceneEntry : sceneVolumes.entrySet()) {
            Map<String, Float> modifiedSounds = new LinkedHashMap<>();
            for (Map.Entry<String, Float> soundEntry : sceneEntry.getValue().entrySet()) {
                String path = soundEntry.getKey();
                float defaultVol = defaultSoundVolume(path);
                if (Math.abs(soundEntry.getValue() - defaultVol) > 0.001f) {
                    String fileName = path.substring(path.lastIndexOf('/') + 1);
                    modifiedSounds.put(fileName, soundEntry.getValue());
                }
            }
            if (!modifiedSounds.isEmpty()) {
                String sceneKey = sceneEntry.getKey() != null ? sceneEntry.getKey().toUpperCase() : "UNKNOWN";
                modified.put(sceneKey, modifiedSounds);
            }
        }
        return modified;
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
        if (music != null) setGain(music, musicGain(masterVolume * musicVolume * fadeMultiplier));
        if (ambient != null) {
            float currentFade = (currentMode == MusicMode.MENU && (isFadingIn || isFadingOut)) ? fadeMultiplier : 1.0f;
            setGain(ambient, ambientGain(masterVolume * musicVolume * currentFade));
        }
        for (Map.Entry<String, Clip> entry : clips.entrySet()) {
            setClipGain(entry.getKey(), entry.getValue());
        }
    }

    private void setClipGain(String resourcePath, Clip clip) {
        String sceneName = clipScenes.getOrDefault(resourcePath, "GLOBAL");
        setGain(clip, gameplayGain(masterVolume * fxVolume * sceneVolume(sceneName, resourcePath)
            * clipVolumeMultipliers.getOrDefault(resourcePath, 1.0f)));
    }

    private float gameplayGain(float gain) { return previewing ? 0.0f : gain; }
    private float ambientGain(float gain) { return gameplayGain(gain * AMBIENT_GAIN_MULTIPLIER); }
    private float musicGain(float gain) { return musicMuted ? 0.0f : gameplayGain(gain); }

    private void muteGameplayAudio() {
        if (music != null) setGain(music, 0.0f);
        if (ambient != null) setGain(ambient, 0.0f);
        for (Clip clip : clips.values()) setGain(clip, 0.0f);
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
        sounds.put("INTRO", Arrays.asList("knock.wav", "ui-open.wav"));
        sounds.put("BEDROOM", withMovement(movement, "ui-open.wav", "door-open.wav", "door-close.wav", "knock.wav"));
        sounds.put("STREET", withMovement(movement, "ui-open.wav", "door-open.wav", "door-close.wav",
            "cat/cat1.wav", "cat/cat2.wav", "cat/cat3.wav"));
        sounds.put("SHOP", withMovement(movement, "ui-open.wav", "door-open.wav", "door-close.wav"));
        sounds.put("BOARD", Arrays.asList("ui-open.wav", "ui-close.wav", "ui-select.wav", "ui-error.wav",
            "ui-confirm.wav", "ui-click.wav", "gate-place.wav", "switch.wav", "success.wav", "failure.wav"));
        sounds.put("NOTEBOOK", Arrays.asList("book-open.wav", "book-flip.wav", "ui-close.wav", "ui-select.wav"));
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
        targetTrackPath = null;
        desiredMode = null;
        currentMode = null;
        isFadingOut = false;
        isFadingIn = false;
        fadeMultiplier = 1.0f;
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
            System.err.println("Failed to load sound resource [" + resourcePath + "]: " + error);
            return null;
        }
    }
}
