package com.cleanroommc.installer.target;

import java.util.*;

/**
 * The registry. Mirrors {@link com.cleanroommc.javautils.spi.JavaLocator#locators} so the shape is familiar.
 */
public final class InstallTargets {

    private static final List<String> ORDER = Arrays.asList("client", "server", "mmc");

    private static List<InstallTarget> cached;

    public static synchronized List<InstallTarget> all() {
        if (cached == null) {
            List<InstallTarget> targets = new ArrayList<>();
            for (InstallTarget target : ServiceLoader.load(InstallTarget.class, InstallTargets.class.getClassLoader())) {
                targets.add(target);
            }
            targets.sort(Comparator.comparingInt(target -> {
                int index = ORDER.indexOf(target.id());
                return index < 0 ? ORDER.size() : index;
            }));
            cached = Collections.unmodifiableList(targets);
        }
        return cached;
    }

    public static InstallTarget byId(String id) throws InstallException {
        for (InstallTarget target : all()) {
            if (target.id().equals(id)) {
                return target;
            }
        }
        throw new InstallException(ExitCode.USAGE, "Unknown mode: " + id + ". Known modes: " + ids());
    }

    public static String ids() {
        StringBuilder builder = new StringBuilder();
        for (InstallTarget target : all()) {
            if (builder.length() > 0) {
                builder.append(", ");
            }
            builder.append(target.id());
        }
        return builder.toString();
    }

    private InstallTargets() { }

}
