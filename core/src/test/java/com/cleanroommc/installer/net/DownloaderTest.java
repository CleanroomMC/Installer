package com.cleanroommc.installer.net;

import com.cleanroommc.installer.target.ExitCode;
import com.cleanroommc.installer.target.InstallException;
import com.cleanroommc.installer.util.Hashes;
import com.cleanroommc.installer.util.Log;
import com.cleanroommc.installer.util.ProgressListener;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DownloaderTest {

    private static final byte[] BODY = "cleanroom".getBytes(StandardCharsets.UTF_8);
    private static final String BODY_SHA1 = "4b825dc642cb6eb9a060e54bf8d69288fbee4904";

    @TempDir
    Path directory;

    private HttpServer server;
    private String base;

    @BeforeEach
    void startServer() throws IOException {
        this.server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        this.server.createContext("/good", exchange -> respond(exchange, BODY));
        this.server.createContext("/truncated", exchange -> respond(exchange, "clean".getBytes(StandardCharsets.UTF_8)));
        this.server.createContext("/broken", exchange -> {
            exchange.sendResponseHeaders(500, -1);
            exchange.close();
        });
        this.server.start();
        this.base = "http://127.0.0.1:" + this.server.getAddress().getPort();
    }

    @AfterEach
    void stopServer() {
        this.server.stop(0);
    }

    @Test
    void downloadsAndVerifies() throws Exception {
        Path file = this.directory.resolve("artifact.jar");
        Downloader downloader = downloader();
        assertTrue(downloader.download(this.base + "/good", file, sha1(), BODY.length, ProgressListener.NONE));
        assertEquals("cleanroom", new String(Files.readAllBytes(file), StandardCharsets.UTF_8));
    }

    @Test
    void skipsAFileThatIsAlreadyCorrect() throws Exception {
        Path file = this.directory.resolve("artifact.jar");
        Downloader downloader = downloader();
        downloader.download(this.base + "/good", file, sha1(), BODY.length, ProgressListener.NONE);
        assertFalse(downloader.download(this.base + "/good", file, sha1(), BODY.length, ProgressListener.NONE),
                "an intact file must not be fetched again");
    }

    @Test
    void aWrongHashFailsAndLeavesNoFileBehind() {
        Path file = this.directory.resolve("artifact.jar");
        InstallException failure = assertThrows(InstallException.class, () -> downloader().download(
                this.base + "/good", file, "0000000000000000000000000000000000000000", 0, ProgressListener.NONE));
        assertEquals(ExitCode.VERIFICATION, failure.exitCode());
        assertTrue(failure.getMessage().contains("/good"), "the message must name the URL");
        assertFalse(Files.exists(file), "a file that failed verification must not be left in place");
    }

    @Test
    void aWrongSizeFailsBeforeHashing() {
        Path file = this.directory.resolve("artifact.jar");
        InstallException failure = assertThrows(InstallException.class, () -> downloader().download(
                this.base + "/truncated", file, sha1(), BODY.length, ProgressListener.NONE));
        assertEquals(ExitCode.VERIFICATION, failure.exitCode());
        assertTrue(failure.getMessage().contains("expected " + BODY.length));
    }

    @Test
    void serverErrorsBecomeNetworkFailures() {
        Path file = this.directory.resolve("artifact.jar");
        InstallException failure = assertThrows(InstallException.class, () -> downloader().download(
                this.base + "/broken", file, null, 0, ProgressListener.NONE));
        assertEquals(ExitCode.NETWORK, failure.exitCode());
    }

    @Test
    void offlineModeNeverReachesTheNetwork() {
        Path file = this.directory.resolve("artifact.jar");
        Downloader offline = new Downloader(Log.console(), true, 1);
        InstallException failure = assertThrows(InstallException.class,
                () -> offline.download(this.base + "/good", file, null, 0, ProgressListener.NONE));
        assertEquals(ExitCode.NETWORK, failure.exitCode());
        assertTrue(failure.getMessage().contains("Offline"));
    }

    @Test
    void existsProbesWithoutDownloading() {
        Downloader downloader = downloader();
        assertTrue(downloader.exists(this.base + "/good"));
        assertFalse(downloader.exists(this.base + "/broken"));
    }

    private Downloader downloader() {
        return new Downloader(Log.console(), false, 1);
    }

    private static String sha1() throws IOException {
        return Hashes.hash(temp(), "SHA-1");
    }

    private static Path temp() throws IOException {
        Path file = Files.createTempFile("downloader", ".bin");
        Files.write(file, BODY);
        file.toFile().deleteOnExit();
        return file;
    }

    private static void respond(HttpExchange exchange, byte[] body) throws IOException {
        exchange.sendResponseHeaders(200, body.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(body);
        }
    }

}
