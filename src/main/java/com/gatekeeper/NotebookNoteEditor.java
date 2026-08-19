package com.gatekeeper;

/** Owns editable notebook-note text and caret operations. */
final class NotebookNoteEditor {
    static final int MAX_LENGTH = 420;
    private static final int MAX_LINES = 26;

    private final StringBuilder text = new StringBuilder();
    private int cursor;

    String text() {
        return text.toString();
    }

    int cursor() {
        return cursor;
    }

    void setText(String value) {
        text.setLength(0);
        if (value != null) {
            String normalized = value.replace("\r\n", "\n").replace('\r', '\n');
            int length = Math.min(normalized.length(), MAX_LENGTH);
            text.append(normalized, 0, length);
        }
        cursor = text.length();
    }

    boolean insert(char character) {
        if (text.length() >= MAX_LENGTH || character == '\uffff'
            || Character.isISOControl(character)) return false;
        text.insert(cursor, character);
        cursor++;
        return true;
    }

    boolean insertNewLine() {
        if (text.length() >= MAX_LENGTH || lineCount() >= MAX_LINES) return false;
        text.insert(cursor, '\n');
        cursor++;
        return true;
    }

    boolean backspace() {
        if (cursor <= 0) return false;
        text.deleteCharAt(cursor - 1);
        cursor--;
        return true;
    }

    boolean delete() {
        if (cursor >= text.length()) return false;
        text.deleteCharAt(cursor);
        return true;
    }

    void moveLeft() {
        cursor = Math.max(0, cursor - 1);
    }

    void moveRight() {
        cursor = Math.min(text.length(), cursor + 1);
    }

    void moveHome() {
        while (cursor > 0 && text.charAt(cursor - 1) != '\n') cursor--;
    }

    void moveEnd() {
        while (cursor < text.length() && text.charAt(cursor) != '\n') cursor++;
    }

    private int lineCount() {
        int count = 1;
        for (int index = 0; index < text.length(); index++) {
            if (text.charAt(index) == '\n') count++;
        }
        return count;
    }
}
