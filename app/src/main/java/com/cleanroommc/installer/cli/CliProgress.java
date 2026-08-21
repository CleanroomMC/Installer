package com.cleanroommc.installer.cli;

import com.cleanroommc.installer.util.ProgressListener;

import java.io.PrintStream;

/**
 * Renders progress as a single rewritten line, or as NDJSON when {@code --json} is on.
 */
public final class CliProgress implements ProgressListener {

    private final PrintStream out;
    private final boolean json;
    private final boolean quiet;
    private String stage = "";
    private String detail = "";
    private int lastWidth;
    private long lastPaint;

    public CliProgress(PrintStream out, boolean json, boolean quiet) {
        this.out = out;
        this.json = json;
        this.quiet = quiet;
    }

    @Override
    public void stage(String name) {
        this.stage = name;
        this.detail = "";
        if (this.json) {
            this.out.println("{\"event\":\"stage\",\"name\":" + quote(name) + "}");
        } else if (!this.quiet) {
            clear();
            this.out.println(name);
        }
    }

    @Override
    public void detail(String message) {
        this.detail = message;
    }

    @Override
    public void progress(long done, long total) {
        if (this.json) {
            this.out.println("{\"event\":\"progress\",\"stage\":" + quote(this.stage)
                    + ",\"detail\":" + quote(this.detail)
                    + ",\"done\":" + done + ",\"total\":" + total + "}");
            return;
        }
        if (this.quiet) {
            return;
        }
        long now = System.currentTimeMillis();
        boolean finished = total > 0 && done >= total;
        if (!finished && now - this.lastPaint < 50L) {
            return;
        }
        this.lastPaint = now;
        StringBuilder line = new StringBuilder("  ");
        if (total > 0) {
            line.append(done * 100L / total).append("% ");
        }
        line.append(this.detail);
        clear();
        this.out.print(line);
        this.out.flush();
        this.lastWidth = line.length();
    }

    /** Ends the progress line so ordinary output does not land on top of it. */
    public void done() {
        if (!this.json && !this.quiet && this.lastWidth > 0) {
            clear();
            this.out.flush();
        }
    }

    private void clear() {
        if (this.lastWidth > 0) {
            StringBuilder blank = new StringBuilder("\r");
            for (int i = 0; i < this.lastWidth; i++) {
                blank.append(' ');
            }
            this.out.print(blank.append('\r'));
            this.lastWidth = 0;
        }
    }

    static String quote(String value) {
        if (value == null) {
            return "null";
        }
        StringBuilder builder = new StringBuilder("\"");
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"':
                    builder.append("\\\"");
                    break;
                case '\\':
                    builder.append("\\\\");
                    break;
                case '\n':
                    builder.append("\\n");
                    break;
                case '\r':
                    builder.append("\\r");
                    break;
                case '\t':
                    builder.append("\\t");
                    break;
                default:
                    if (c < 0x20) {
                        builder.append(String.format("\\u%04x", (int) c));
                    } else {
                        builder.append(c);
                    }
            }
        }
        return builder.append('"').toString();
    }

}
