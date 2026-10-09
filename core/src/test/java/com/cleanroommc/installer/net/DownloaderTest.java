/*
 * Copyright (c) 2026 CleanroomMC contributors
 * SPDX-License-Identifier: LGPL-3.0-only
 */

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

class DownloaderTest {

    private static final byte[] BODY = "cleanroom".getBytes(StandardCharsets.UTF_8);
    private static final String BODY_SHA1 = "4b825dc642cb6eb9a060e54bf8d69288fbee4904";

    @TempDir Path directory;

    private HttpServer server;
    private String base;

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

    @BeforeEach
    void startServer() throws IOException {
        this.server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        this.server.createContext("/good", exchange -> respond(exchange, BODY));
        this.server.createContext(
            "/truncated",
            exchange -> respond(exchange, "clean".getBytes(StandardCharsets.UTF_8))
        );
        this.server.createContext(
            "/good.sha1",
            exchange -> respond(exchange, (sha1() + "\n").getBytes(StandardCharsets.UTF_8))
        );
        this.server.createContext(
            "/truncated.sha1",
            exchange -> respond(exchange, sha1().getBytes(StandardCharsets.UTF_8))
        );
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
        assertThat(downloader.download(this.base + "/good", file, sha1(), BODY.length, ProgressListener.NONE)).isTrue();
        assertThat(new String(Files.readAllBytes(file), StandardCharsets.UTF_8)).isEqualTo("cleanroom");
    }

    @Test
    void skipsAFileThatIsAlreadyCorrect() throws Exception {
        Path file = this.directory.resolve("artifact.jar");
        Downloader downloader = downloader();
        downloader.download(this.base + "/good", file, sha1(), BODY.length, ProgressListener.NONE);
        assertThat(downloader.download(this.base + "/good", file, sha1(), BODY.length, ProgressListener.NONE))
            .as("an intact file must not be fetched again")
            .isFalse();
    }

    @Test
    void aWrongHashFailsAndLeavesNoFileBehind() {
        Path file = this.directory.resolve("artifact.jar");
        InstallException failure = catchThrowableOfType(
            InstallException.class,
            () -> downloader().download(
                this.base + "/good",
                file,
                "0000000000000000000000000000000000000000",
                0,
                ProgressListener.NONE
            )
        );
        assertThat(failure.exitCode()).isEqualTo(ExitCode.VERIFICATION);
        assertThat(failure.getMessage().contains("/good")).as("the message must name the URL").isTrue();
        assertThat(Files.exists(file)).as("a file that failed verification must not be left in place").isFalse();
    }

    @Test
    void aWrongSizeFailsBeforeHashing() {
        Path file = this.directory.resolve("artifact.jar");
        InstallException failure = catchThrowableOfType(
            InstallException.class,
            () -> downloader().download(this.base + "/truncated", file, sha1(), BODY.length, ProgressListener.NONE)
        );
        assertThat(failure.exitCode()).isEqualTo(ExitCode.VERIFICATION);
        assertThat(failure.getMessage().contains("expected " + BODY.length)).isTrue();
    }

    @Test
    void sidecarChecksumReplacesAStaleCopyAndRejectsAWrongBody() throws Exception {
        Path file = this.directory.resolve("artifact.jar");
        Files.write(file, "stale".getBytes(StandardCharsets.UTF_8));
        assertThat(downloader().downloadVerified(this.base + "/good", file, ProgressListener.NONE)).isTrue();
        assertThat(Files.readAllBytes(file)).isEqualTo(BODY);

        InstallException failure = catchThrowableOfType(
            InstallException.class,
            () -> downloader().downloadVerified(
                this.base + "/truncated",
                this.directory.resolve("other.jar"),
                ProgressListener.NONE
            )
        );
        assertThat(failure.exitCode()).isEqualTo(ExitCode.VERIFICATION);
    }

    @Test
    void aPartialFileIsOnlyResumedWhenAChecksumCanCatchASplice() throws Exception {
        this.server.createContext("/ranged", exchange -> {
            boolean ranged = exchange.getRequestHeaders().containsKey("Range");
            byte[] body = ranged ? "room".getBytes(StandardCharsets.UTF_8) : BODY;
            exchange.sendResponseHeaders(ranged ? 206 : 200, body.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
        });
        Path file = this.directory.resolve("artifact.jar");
        Path partial = this.directory.resolve("artifact.jar.part");

        Files.write(partial, "clean".getBytes(StandardCharsets.UTF_8));
        downloader().download(this.base + "/ranged", file, sha1(), 0, ProgressListener.NONE);
        assertThat(Files.readAllBytes(file)).isEqualTo(BODY);

        Files.delete(file);
        Files.write(partial, "stale".getBytes(StandardCharsets.UTF_8));
        downloader().download(this.base + "/ranged", file, null, 0, ProgressListener.NONE);
        assertThat(Files.readAllBytes(file)).isEqualTo(BODY);
    }

    @Test
    void serverErrorsBecomeNetworkFailures() {
        Path file = this.directory.resolve("artifact.jar");
        InstallException failure = catchThrowableOfType(
            InstallException.class,
            () -> downloader().download(this.base + "/broken", file, null, 0, ProgressListener.NONE)
        );
        assertThat(failure.exitCode()).isEqualTo(ExitCode.NETWORK);
    }

    @Test
    void offlineModeNeverReachesTheNetwork() {
        Path file = this.directory.resolve("artifact.jar");
        Downloader offline = new Downloader(Log.console(), true, 1);
        InstallException failure = catchThrowableOfType(
            InstallException.class,
            () -> offline.download(this.base + "/good", file, null, 0, ProgressListener.NONE)
        );
        assertThat(failure.exitCode()).isEqualTo(ExitCode.NETWORK);
        assertThat(failure.getMessage().contains("Offline")).isTrue();
    }

    @Test
    void existsProbesWithoutDownloading() {
        Downloader downloader = downloader();
        assertThat(downloader.exists(this.base + "/good")).isTrue();
        assertThat(downloader.exists(this.base + "/broken")).isFalse();
    }

    private Downloader downloader() {
        return new Downloader(Log.console(), false, 1);
    }

}
