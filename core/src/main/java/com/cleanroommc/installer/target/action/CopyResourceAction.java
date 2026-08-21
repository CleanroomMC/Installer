package com.cleanroommc.installer.target.action;

import com.cleanroommc.installer.target.ExitCode;
import com.cleanroommc.installer.target.InstallContext;
import com.cleanroommc.installer.target.InstallException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Copy a file the installer holds.
 */
public final class CopyResourceAction extends Action {

    @FunctionalInterface
    public interface ResourceSupplier {

        InputStream open() throws IOException;

    }

    private final String description;
    private final ResourceSupplier source;

    public CopyResourceAction(String description, ResourceSupplier source, Path destination) {
        super(destination);
        this.description = description;
        this.source = source;
    }

    @Override
    public String describe() {
        return "COPY " + this.description + " -> " + destination();
    }

    @Override
    public void execute(InstallContext context) throws InstallException {
        try {
            Path parent = destination().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            try (InputStream in = this.source.open()) {
                if (in == null) {
                    throw new IOException("missing: " + this.description);
                }
                Files.copy(in, destination(), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new InstallException(ExitCode.TARGET, "Unable to write " + destination(), e);
        }
    }

}
