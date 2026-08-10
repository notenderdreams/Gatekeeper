package com.gatekeeper;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class DevLog {
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    public static void log(String jsonContent) {
        String timestamp = LocalTime.now().format(TIME_FORMATTER);
        System.out.println("[" + timestamp + "]");
        System.out.println(jsonContent);
    }
}
