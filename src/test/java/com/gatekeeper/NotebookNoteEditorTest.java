package com.gatekeeper;

public final class NotebookNoteEditorTest {
    public static void main(String[] args) {
        NotebookNoteEditor editor = new NotebookNoteEditor();
        require(editor.insert('A') && editor.insert('N') && editor.insert('D'),
            "printable characters should be inserted");
        require(editor.insertNewLine() && editor.insert('1'),
            "notes should support multiple lines");
        require(editor.text().equals("AND\n1"), "inserted note should match");

        editor.moveLeft();
        require(editor.backspace(), "backspace should edit before the caret");
        require(editor.text().equals("AND1"), "backspace should remove the newline");
        require(editor.delete(), "delete should edit at the caret");
        require(editor.text().equals("AND"), "delete should remove the next character");

        editor.setText("first\nsecond");
        editor.moveHome();
        require(editor.cursor() == 6, "home should move to the current line start");
        editor.moveEnd();
        require(editor.cursor() == editor.text().length(),
            "end should move to the current line end");

        editor.setText("x".repeat(NotebookNoteEditor.MAX_LENGTH + 20));
        require(editor.text().length() == NotebookNoteEditor.MAX_LENGTH,
            "loaded notes should respect the visible capacity");
        require(!editor.insert('x'), "full notes should reject more text");

        System.out.println("NotebookNoteEditorTest: all checks passed");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
