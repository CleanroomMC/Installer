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

    public DownloadAction(String url, Path destination, String sha1, long size) {
        super(destination);
        this.url = url;
        this.sha1 = sha1;
        this.size = size;
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
        context.downloader().download(this.url, destination(), this.sha1, this.size, context.listener());
    }

}
