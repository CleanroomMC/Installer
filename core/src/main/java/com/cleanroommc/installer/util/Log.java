package com.cleanroommc.installer.util;

import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * A deliberately tiny logger. Everything goes to a file whose path is printed on both success and
 * failure — a double-clicked jar that loses its output is the single most common installer support
 * complaint.
 */
public final class Log implements AutoCloseable {

    public enum Level { DEBUG, INFO, WARN, ERROR }

    private final SimpleDateFormat stamp = new SimpleDateFormat("HH:mm:ss.SSS");
    private final PrintStream console;
    private final Path file;
    private final Writer writer;
    private Level consoleLevel = Level.INFO;

    public static Log toFile(Path file) {
        return new Log(System.err, file);
    }

    public static Log console() {
        return new Log(System.err, null);
    }

    private Log(PrintStream console, Path file) {
        this.console = console;
        this.file = file;
        Writer opened = null;
        if (file != null) {
            try {
                if (file.getParent() != null) {
                    Files.createDirectories(file.getParent());
                }
                opened = Files.newBufferedWriter(file, StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (IOException e) {
                console.println("[WARN ] Unable to open log file " + file + ": " + e);
            }
        }
        this.writer = opened;
    }

    public Path file() {
        return this.file;
    }

    public void consoleLevel(Level level) {
        this.consoleLevel = level;
    }

    public void debug(String message, Object... args) {
        log(Level.DEBUG, message, args, null);
    }

    public void info(String message, Object... args) {
        log(Level.INFO, message, args, null);
    }

    public void warn(String message, Object... args) {
        log(Level.WARN, message, args, null);
    }

    public void error(String message, Object... args) {
        log(Level.ERROR, message, args, null);
    }

    public void error(Throwable thrown, String message, Object... args) {
        log(Level.ERROR, message, args, thrown);
    }

    private void log(Level level, String message, Object[] args, Throwable thrown) {
        String text = format(message, args);
        String line = "[" + pad(level.name()) + "] " + text;
        if (level.ordinal() >= this.consoleLevel.ordinal()) {
            this.console.println(line);
            if (thrown != null && this.consoleLevel == Level.DEBUG) {
                thrown.printStackTrace(this.console);
            }
        }
        if (this.writer != null) {
            try {
                this.writer.write(this.stamp.format(new Date()) + " " + line + System.lineSeparator());
                if (thrown != null) {
                    StringWriter trace = new StringWriter();
                    thrown.printStackTrace(new PrintWriter(trace));
                    this.writer.write(trace.toString());
                }
                this.writer.flush();
            } catch (IOException ignored) {
                // A failing log file must never take the install down with it.
            }
        }
    }

    /** SLF4J-style {@code {}} placeholders, without the dependency. */
    static String format(String message, Object[] args) {
        if (args == null || args.length == 0) {
            return message;
        }
        StringBuilder builder = new StringBuilder(message.length() + 32);
        int argument = 0;
        int index = 0;
        while (index < message.length()) {
            int placeholder = message.indexOf("{}", index);
            if (placeholder < 0 || argument >= args.length) {
                builder.append(message, index, message.length());
                break;
            }
            builder.append(message, index, placeholder).append(args[argument++]);
            index = placeholder + 2;
        }
        return builder.toString();
    }

    private static String pad(String level) {
        StringBuilder builder = new StringBuilder(level);
        while (builder.length() < 5) {
            builder.append(' ');
        }
        return builder.toString();
    }

    @Override
    public void close() {
        if (this.writer != null) {
            try {
                this.writer.close();
            } catch (IOException ignored) {
            }
        }
    }

}
