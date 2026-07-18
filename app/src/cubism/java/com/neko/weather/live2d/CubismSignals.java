package com.neko.weather.live2d;

import java.util.concurrent.atomic.AtomicBoolean;

/** Small process-local bridge between TTS and active Cubism renderers. */
public final class CubismSignals {
    private static final AtomicBoolean SPEAKING = new AtomicBoolean(false);

    public static boolean isSpeaking() {
        return SPEAKING.get();
    }

    public static void setSpeaking(boolean speaking) {
        SPEAKING.set(speaking);
    }

    private CubismSignals() {}
}
