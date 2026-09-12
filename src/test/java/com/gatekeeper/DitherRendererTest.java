package com.gatekeeper;

/** Regression checks for which scenes receive environmental color dithering. */
public final class DitherRendererTest {
    public static void main(String[] args) {
        require(DitherRenderer.appliesTo(GameScene.BEDROOM, true),
            "bedroom environment should receive color dithering");
        require(DitherRenderer.appliesTo(GameScene.STREET, true),
            "street environment should receive color dithering");
        require(DitherRenderer.appliesTo(GameScene.SHOP, true),
            "shop environment should receive color dithering");
        require(!DitherRenderer.appliesTo(GameScene.BOARD, true),
            "circuit board UI must remain crisp without color dithering");
        require(!DitherRenderer.appliesTo(GameScene.BEDROOM, false),
            "the developer toggle must disable color dithering");
        System.out.println("DitherRendererTest: all checks passed");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
