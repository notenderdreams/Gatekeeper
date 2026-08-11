package com.gatekeeper;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Handles game state serialization, file persistence, and save slot loading. */
public final class SaveManager {
    private static final Path SAVE_FILE_PATH = Path.of("gatekeep.json");

    private SaveManager() {}

    public static boolean hasSave() {
        return Files.exists(SAVE_FILE_PATH) && Files.isRegularFile(SAVE_FILE_PATH);
    }

    public static boolean saveGame(SaveData data) {
        if (data == null) return false;
        try {
            StringBuilder json = new StringBuilder();
            json.append("{\n");
            json.append("  \"chapter\": ").append(data.chapter).append(",\n");
            json.append("  \"scene\": \"").append(data.scene != null ? data.scene.name() : "BEDROOM").append("\",\n");
            json.append("  \"playerX\": ").append(data.playerX).append(",\n");
            json.append("  \"playerY\": ").append(data.playerY).append(",\n");
            json.append("  \"facing\": \"").append(data.facing != null ? data.facing.name() : "DOWN").append("\",\n");
            json.append("  \"crafted\": [");
            if (data.crafted != null) {
                for (int i = 0; i < data.crafted.length; i++) {
                    json.append(data.crafted[i]);
                    if (i < data.crafted.length - 1) json.append(", ");
                }
            }
            json.append("],\n");
            json.append("  \"notebookPage\": ").append(data.notebookPage).append(",\n");
            json.append("  \"autoTesterAttached\": ").append(data.autoTesterAttached).append(",\n");
            json.append("  \"catPresent\": ").append(data.catPresent).append(",\n");
            json.append("  \"catX\": ").append(data.catX).append(",\n");
            json.append("  \"catY\": ").append(data.catY).append(",\n");
            json.append("  \"catAlwaysAppears\": ").append(data.catAlwaysAppears).append(",\n");
            json.append("  \"instantStart\": ").append(data.instantStart).append(",\n");
            json.append("  \"ccBedroomBackground\": ").append(data.ccBedroomBackground).append(",\n");
            json.append("  \"ccStreetBackground\": ").append(data.ccStreetBackground).append(",\n");
            json.append("  \"boxRetrieved\": ").append(data.boxRetrieved).append(",\n");
            json.append("  \"boxOpened\": ").append(data.boxOpened).append(",\n");
            json.append("  \"workbenchInstalled\": ").append(data.workbenchInstalled).append(",\n");
            json.append("  \"starCount\": ").append(data.starCount).append(",\n");
            json.append("  \"mothCount\": ").append(data.mothCount).append(",\n");
            json.append("  \"taskbarOnRight\": ").append(data.taskbarOnRight).append("\n");
            json.append("}\n");

            Files.writeString(SAVE_FILE_PATH, json.toString(), StandardCharsets.UTF_8);
            return true;
        } catch (Exception e) {
            System.err.println("Failed to save game: " + e.getMessage());
            return false;
        }
    }

    public static SaveData loadGame() {
        if (!hasSave()) return null;
        try {
            String content = Files.readString(SAVE_FILE_PATH, StandardCharsets.UTF_8);
            SaveData data = new SaveData();

            data.chapter = parseInt(content, "chapter", 0);
            data.scene = parseEnum(content, "scene", GameScene.class, GameScene.BEDROOM);
            data.playerX = parseInt(content, "playerX", 210);
            data.playerY = parseInt(content, "playerY", 157);
            data.facing = parseEnum(content, "facing", Facing.class, Facing.DOWN);
            data.crafted = parseBooleanArray(content, "crafted", 5);
            data.notebookPage = parseInt(content, "notebookPage", 0);
            data.autoTesterAttached = parseBoolean(content, "autoTesterAttached", false);
            data.catPresent = parseBoolean(content, "catPresent", false);
            data.catX = parseInt(content, "catX", GameConstants.STREET_CAT_X);
            data.catY = parseInt(content, "catY", 152);
            data.catAlwaysAppears = parseBoolean(content, "catAlwaysAppears", false);
            data.instantStart = parseBoolean(content, "instantStart", false);
            data.ccBedroomBackground = parseBoolean(content, "ccBedroomBackground", true);
            data.ccStreetBackground = parseBoolean(content, "ccStreetBackground", true);
            data.boxRetrieved = parseBoolean(content, "boxRetrieved", false);
            data.boxOpened = parseBoolean(content, "boxOpened", false);
            data.workbenchInstalled = parseBoolean(content, "workbenchInstalled", data.chapter >= 2);
            data.starCount = parseInt(content, "starCount", 25);
            data.mothCount = parseInt(content, "mothCount", 3);
            data.taskbarOnRight = parseBoolean(content, "taskbarOnRight", false);

            return data;
        } catch (Exception e) {
            System.err.println("Failed to load game: " + e.getMessage());
            return null;
        }
    }

    public static void deleteSave() {
        try {
            Files.deleteIfExists(SAVE_FILE_PATH);
        } catch (Exception ignored) {}
    }

    private static String extractValue(String json, String key) {
        Pattern pattern = Pattern.compile("\"" + key + "\"\\s*:\\s*\"?([^\",\\]\\}\n]+)\"?");
        Matcher matcher = pattern.matcher(json);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }

    private static int parseInt(String json, String key, int fallback) {
        String val = extractValue(json, key);
        if (val == null) return fallback;
        try { return Integer.parseInt(val); } catch (Exception e) { return fallback; }
    }

    private static float parseFloat(String json, String key, float fallback) {
        String val = extractValue(json, key);
        if (val == null) return fallback;
        try { return Float.parseFloat(val); } catch (Exception e) { return fallback; }
    }

    private static boolean parseBoolean(String json, String key, boolean fallback) {
        String val = extractValue(json, key);
        if (val == null) return fallback;
        return Boolean.parseBoolean(val);
    }

    private static <T extends Enum<T>> T parseEnum(String json, String key, Class<T> enumClass, T fallback) {
        String val = extractValue(json, key);
        if (val == null) return fallback;
        try { return Enum.valueOf(enumClass, val); } catch (Exception e) { return fallback; }
    }

    private static boolean[] parseBooleanArray(String json, String key, int length) {
        boolean[] result = new boolean[length];
        Pattern pattern = Pattern.compile("\"" + key + "\"\\s*:\\s*\\[([^\\]]*)\\]");
        Matcher matcher = pattern.matcher(json);
        if (matcher.find()) {
            String[] parts = matcher.group(1).split(",");
            for (int i = 0; i < Math.min(length, parts.length); i++) {
                result[i] = Boolean.parseBoolean(parts[i].trim());
            }
        }
        return result;
    }
}
