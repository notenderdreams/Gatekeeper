package com.gatekeeper;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

public final class Main {
    private Main() {}

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame window = new JFrame("GATEKEEPER: A Logic Tale");
            GamePanel game = new GamePanel();
            window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            window.setResizable(true);
            window.setContentPane(game);
            window.pack();
            window.setLocationRelativeTo(null);
            window.setVisible(true);
            game.requestFocusInWindow();
        });
    }
}
