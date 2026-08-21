package com.cleanroommc.installer.target.action;

import com.cleanroommc.installer.target.ExitCode;
import com.cleanroommc.installer.target.InstallContext;
import com.cleanroommc.installer.target.InstallException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;

/**
 * Write generated text: a version json, an args file, a launch script.
 */
public final class WriteFileAction extends Action {

    private final String content;
    private final boolean executable;

    public WriteFileAction(Path destination, String content) {
        this(destination, content, false);
    }

    public WriteFileAction(Path destination, String content, boolean executable) {
        super(destination);
        this.content = content;
        this.executable = executable;
    }

    public String content() {
        return this.content;
    }

    @Override
    public String describe() {
        return "WRITE " + destination() + " (" + this.content.length() + " chars"
                + (this.executable ? ", executable)" : ")");
    }

    @Override
    public void execute(InstallContext context) throws InstallException {
        try {
            Path parent = destination().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.write(destination(), this.content.getBytes(StandardCharsets.UTF_8));
            if (this.executable) {
                makeExecutable(destination());
            }
        } catch (IOException e) {
            throw new InstallException(ExitCode.TARGET, "Unable to write " + destination(), e);
        }
    }

    private static void makeExecutable(Path path) throws IOException {
        try {
            Set<PosixFilePermission> permissions = Files.getPosixFilePermissions(path);
            permissions.add(PosixFilePermission.OWNER_EXECUTE);
            permissions.add(PosixFilePermission.GROUP_EXECUTE);
            permissions.add(PosixFilePermission.OTHERS_EXECUTE);
            Files.setPosixFilePermissions(path, permissions);
        } catch (UnsupportedOperationException windows) { }
    }

}
