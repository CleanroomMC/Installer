package com.cleanroommc.installer.net;

import com.cleanroommc.installer.InstallerMeta;
import com.cleanroommc.installer.target.ExitCode;
import com.cleanroommc.installer.target.InstallException;
import com.cleanroommc.installer.util.Hashes;
import com.cleanroommc.installer.util.Log;
import com.cleanroommc.installer.util.ProgressListener;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.atomic.AtomicLong;

public final class Downloader {

    private static final int CONNECT_TIMEOUT_MS = 10_000;
    private static final int READ_TIMEOUT_MS = 30_000;
    private static final int BUFFER = 64 * 1024;

    private final Log log;
    private final boolean offline;
    private final int attempts;

    public Downloader(Log log, boolean offline) {
        this(log, offline, 3);
    }

    public Downloader(Log log, boolean offline, int attempts) {
        this.log = log;
        this.offline = offline;
        this.attempts = Math.max(1, attempts);
    }

    public boolean offline() {
        return this.offline;
    }

    /**
     * Downloads to {@code destination} unless an intact copy is already there.
     *
     * @return true when bytes were transferred, false when the existing file was already correct
     */
    public boolean download(String url, Path destination, String sha1, long size, ProgressListener listener) throws InstallException {
        try {
            if (!Hashes.corrupt(destination, sha1, size)) {
                this.log.debug("Already present: {}", destination);
                return false;
            }
        } catch (IOException e) {
            throw new InstallException(ExitCode.TARGET, "Unable to inspect " + destination, e);
        }
        if (this.offline) {
            throw new InstallException(ExitCode.NETWORK, "Offline mode: " + destination.getFileName() + " is missing and " + url + " cannot be fetched");
        }
        IOException last = null;
        for (int attempt = 1; attempt <= this.attempts; attempt++) {
            try {
                transfer(url, destination, size, listener);
                verify(url, destination, sha1, size);
                return true;
            } catch (IOException e) {
                last = e;
                this.log.warn("Download of {} failed (attempt {}/{}): {}", url, attempt, this.attempts, e.getMessage());
                sleepBackoff(attempt);
            }
        }
        throw new InstallException(ExitCode.NETWORK, "Unable to download " + url, last);
    }

    /** Fetches a small resource into memory. Used for manifests and version indexes. */
    public byte[] fetch(String url) throws InstallException {
        if (this.offline) {
            throw new InstallException(ExitCode.NETWORK, "Offline mode: cannot fetch " + url);
        }
        IOException last = null;
        for (int attempt = 1; attempt <= this.attempts; attempt++) {
            HttpURLConnection connection = null;
            try {
                connection = open(url);
                connection.connect();
                int status = connection.getResponseCode();
                if (status != HttpURLConnection.HTTP_OK) {
                    throw new IOException("HTTP " + status + " from " + url);
                }
                try (InputStream in = connection.getInputStream()) {
                    return readFully(in);
                }
            } catch (IOException e) {
                last = e;
                sleepBackoff(attempt);
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }
        throw new InstallException(ExitCode.NETWORK, "Unable to fetch " + url, last);
    }

    /**
     * Whether a URL is there, without fetching it. Used to pick between artifact names that have changed across releases.
     */
    public boolean exists(String url) {
        if (this.offline) {
            return false;
        }
        HttpURLConnection connection = null;
        try {
            connection = open(url);
            connection.setRequestMethod("HEAD");
            connection.connect();
            return connection.getResponseCode() == HttpURLConnection.HTTP_OK;
        } catch (IOException e) {
            return false;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private void transfer(String url, Path destination, long expectedSize, ProgressListener listener) throws IOException {
        Path parent = destination.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Path partial = destination.resolveSibling(destination.getFileName() + ".part");
        long resumeFrom = Files.isRegularFile(partial) ? Files.size(partial) : 0L;
        if (expectedSize > 0 && resumeFrom >= expectedSize) {
            // A partial file at or past the expected length is garbage, not a resume point.
            Files.deleteIfExists(partial);
            resumeFrom = 0L;
        }

        HttpURLConnection connection = open(url);
        if (resumeFrom > 0) {
            connection.setRequestProperty("Range", "bytes=" + resumeFrom + "-");
        }
        try {
            connection.connect();
            int status = connection.getResponseCode();
            boolean resumed = status == HttpURLConnection.HTTP_PARTIAL;
            if (status != HttpURLConnection.HTTP_OK && !resumed) {
                throw new IOException("HTTP " + status + " from " + url);
            }
            if (!resumed) {
                resumeFrom = 0L;
            }
            long total = expectedSize > 0 ? expectedSize : contentLength(connection, resumeFrom);
            AtomicLong done = new AtomicLong(resumeFrom);
            if (listener != null) {
                listener.detail(destination.getFileName().toString());
                listener.progress(resumeFrom, total);
            }
            try (InputStream in = connection.getInputStream();
                 OutputStream out = resumed
                         ? Files.newOutputStream(partial, StandardOpenOption.CREATE, StandardOpenOption.APPEND)
                         : Files.newOutputStream(partial, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                byte[] buffer = new byte[BUFFER];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                    if (listener != null) {
                        listener.progress(done.addAndGet(read), total);
                    }
                }
            }
        } finally {
            connection.disconnect();
        }
        Files.move(partial, destination, StandardCopyOption.REPLACE_EXISTING);
    }

    private void verify(String url, Path destination, String sha1, long size) throws IOException, InstallException {
        if (size > 0) {
            long actual = Files.size(destination);
            if (actual != size) {
                Files.deleteIfExists(destination);
                throw new InstallException(ExitCode.VERIFICATION,
                        "Size mismatch for " + url + ": expected " + size + " bytes, got " + actual);
            }
        }
        if (sha1 == null || sha1.trim().isEmpty()) {
            return;
        }
        String actual = Hashes.sha1(destination);
        if (!sha1.trim().equalsIgnoreCase(actual)) {
            Files.deleteIfExists(destination);
            throw new InstallException(ExitCode.VERIFICATION,
                    "SHA-1 mismatch for " + url + System.lineSeparator()
                            + "  expected " + sha1.trim() + System.lineSeparator()
                            + "  actual   " + actual + System.lineSeparator()
                            + "  saved to " + destination);
        }
    }

    private static long contentLength(HttpURLConnection connection, long resumeFrom) {
        long length = connection.getContentLengthLong();
        return length < 0 ? -1 : length + resumeFrom;
    }

    private static HttpURLConnection open(String url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
        connection.setReadTimeout(READ_TIMEOUT_MS);
        connection.setRequestProperty("User-Agent", InstallerMeta.USER_AGENT);
        connection.setInstanceFollowRedirects(true);
        return connection;
    }

    private static byte[] readFully(InputStream in) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[BUFFER];
        int read;
        while ((read = in.read(chunk)) != -1) {
            buffer.write(chunk, 0, read);
        }
        return buffer.toByteArray();
    }

    private static void sleepBackoff(int attempt) {
        try {
            Thread.sleep(Math.min(4000L, 250L * (1L << (attempt - 1))));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

}
