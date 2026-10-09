/*
 * Copyright (c) 2026 CleanroomMC contributors
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package com.cleanroommc.installer.target.action;

import com.cleanroommc.installer.target.InstallContext;
import com.cleanroommc.installer.target.InstallException;

import java.nio.file.Path;

/**
 * Fetch a remote artifact, verifying size and SHA-1.
 */
public final class DownloadAction extends Action {

    private final String url;
    private final String sha1;
    private final long size;

    private final boolean sidecar;

    public DownloadAction(String url, Path destination, String sha1, long size) {
        super(destination);
        this.url = url;
        this.sha1 = sha1;
        this.size = size;
        this.sidecar = false;
    }

    /**
     * An artifact whose checksum is read from the {@code .sha1} file beside it in a Maven repository.
     */
    public DownloadAction(String url, Path destination) {
        super(destination);
        this.url = url;
        this.sha1 = null;
        this.size = 0L;
        this.sidecar = true;
    }

    public String url() {
        return this.url;
    }

    @Override
    public long networkBytes() {
        return Math.max(0L, this.size);
    }

    @Override
    public String describe() {
        return "DOWNLOAD " + this.url + " -> " + destination();
    }

    @Override
    public void execute(InstallContext context) throws InstallException {
        if (this.sidecar) {
            context.downloader().downloadVerified(this.url, destination(), context.listener());
        } else {
            context.downloader().download(this.url, destination(), this.sha1, this.size, context.listener());
        }
    }

}
