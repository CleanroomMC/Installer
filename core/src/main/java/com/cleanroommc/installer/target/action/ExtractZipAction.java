package com.cleanroommc.installer.target.action;

import com.cleanroommc.installer.target.ExitCode;
import com.cleanroommc.installer.target.InstallContext;
import com.cleanroommc.installer.target.InstallException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Unpack a zip into a directory.
 * Entries are checked against the destination root before being written.
 * A malformed archive cannot write outside it.
 */
public final class ExtractZipAction extends Action {

    private final Path archive;
    private final Set<String> keptNames;

    public ExtractZipAction(Path archive, Path destination) {
        this(archive, destination, Collections.<String>emptySet());
    }

    /**
     * @param keptNames file names to leave alone, matched on the entry's last path segment.
     */
    public ExtractZipAction(Path archive, Path destination, Set<String> keptNames) {
        super(destination);
        this.archive = archive;
        this.keptNames = Collections.unmodifiableSet(new LinkedHashSet<String>(keptNames));
    }

    @Override
    public String describe() {
        return "EXTRACT " + this.archive.getFileName() + " -> " + destination()
                + (this.keptNames.isEmpty() ? "" : " (keeping " + String.join(", ", this.keptNames) + ")");
    }

    @Override
    public void execute(InstallContext context) throws InstallException {
        Path root = destination().toAbsolutePath().normalize();
        try (ZipFile zip = new ZipFile(this.archive.toFile())) {
            Files.createDirectories(root);
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                Path target = root.resolve(entry.getName()).normalize();
                if (!target.startsWith(root)) {
                    throw new InstallException(ExitCode.VERIFICATION,
                            this.archive + " contains an entry that escapes the destination: " + entry.getName());
                }
                if (entry.isDirectory()) {
                    Files.createDirectories(target);
                    continue;
                }
                if (kept(entry.getName(), target)) {
                    continue;
                }
                if (target.getParent() != null) {
                    Files.createDirectories(target.getParent());
                }
                try (InputStream in = zip.getInputStream(entry)) {
                    Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        } catch (IOException e) {
            throw new InstallException(ExitCode.TARGET, "Unable to unpack " + this.archive + " into " + root, e);
        }
    }

    private boolean kept(String entryName, Path target) {
        int slash = entryName.lastIndexOf('/');
        String name = slash < 0 ? entryName : entryName.substring(slash + 1);
        return this.keptNames.contains(name) && Files.exists(target);
    }

}
