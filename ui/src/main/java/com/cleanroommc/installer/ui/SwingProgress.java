package com.cleanroommc.installer.ui;

import com.cleanroommc.installer.util.ProgressListener;

import javax.swing.SwingUtilities;
import java.util.concurrent.atomic.AtomicBoolean;

public final class SwingProgress implements ProgressListener {

    private static final long PAINT_INTERVAL_MS = 50L;

    static void run(Runnable action) {
        if (SwingUtilities.isEventDispatchThread()) {
            action.run();
            return;
        }
        try {
            SwingUtilities.invokeAndWait(action);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private final ProgressWindow window;
    private final AtomicBoolean cancelled = new AtomicBoolean();

    private long lastPaint;

    public SwingProgress(ProgressWindow window) {
        this.window = window;
    }

    public void cancel() {
        this.cancelled.set(true);
    }

    @Override
    public boolean cancelled() {
        return this.cancelled.get();
    }

    @Override
    public void stage(String name) {
        this.window.updateStatus(name);
    }

    @Override
    public void detail(String message) {
        this.window.updateDetail(message);
    }

    @Override
    public void progress(long done, long total) {
        long now = System.currentTimeMillis();
        boolean finished = total > 0 && done >= total;
        if (!finished && now - this.lastPaint < PAINT_INTERVAL_MS) {
            return;
        }
        this.lastPaint = now;
        if (total <= 0) {
            this.window.disableProgress();
            return;
        }
        this.window.enableProgress();
        this.window.setProgress((int) (done * 100L / total));
    }

}
