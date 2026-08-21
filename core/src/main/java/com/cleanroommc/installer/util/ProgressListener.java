package com.cleanroommc.installer.util;

public interface ProgressListener {

    ProgressListener NONE = new ProgressListener() {

        @Override
        public void stage(String name) { }

        @Override
        public void detail(String message) { }

        @Override
        public void progress(long done, long total) { }

    };

    /**
     * A new phase started: "Downloading libraries".
     */
    void stage(String name);

    /**
     * What is currently happening: file name, a URL etc.
     */
    void detail(String message);

    /**
     * Absolute progress within the current stage. A total of {@code -1} means indeterminate.
     */
    void progress(long done, long total);

    /**
     * Whether the user asked to stop. Implementations that cannot be canceled return false.
     */
    default boolean cancelled() {
        return false;
    }

}
