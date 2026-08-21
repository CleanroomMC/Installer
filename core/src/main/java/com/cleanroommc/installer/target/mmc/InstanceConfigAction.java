package com.cleanroommc.installer.target.mmc;

import com.cleanroommc.installer.target.action.Action;
import com.cleanroommc.installer.target.ExitCode;
import com.cleanroommc.installer.target.InstallContext;
import com.cleanroommc.installer.target.InstallException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class InstanceConfigAction extends Action {

    private final Map<String, String> values;

    public InstanceConfigAction(Path config, Map<String, String> values) {
        super(config);
        this.values = new LinkedHashMap<>(values);
    }

    @Override
    public String describe() {
        StringBuilder builder = new StringBuilder("CONFIGURE ").append(destination()).append(" (");
        boolean first = true;
        for (Map.Entry<String, String> value : this.values.entrySet()) {
            if (!first) {
                builder.append(", ");
            }
            builder.append(value.getKey()).append('=').append(value.getValue());
            first = false;
        }
        return builder.append(')').toString();
    }

    @Override
    public void execute(InstallContext context) throws InstallException {
        Path config = destination();
        try {
            List<String> lines = Files.isRegularFile(config) ? Files.readAllLines(config, StandardCharsets.UTF_8) : new ArrayList<>();
            List<String> updated = new ArrayList<>(lines.size() + this.values.size());
            Set<String> written = new LinkedHashSet<>();
            for (String line : lines) {
                int separator = line.indexOf('=');
                String key = separator > 0 ? line.substring(0, separator).trim() : null;
                if (key != null && this.values.containsKey(key)) {
                    // Only the first occurrence survives
                    if (written.add(key)) {
                        updated.add(key + "=" + this.values.get(key));
                    }
                    continue;
                }
                updated.add(line);
            }
            for (Map.Entry<String, String> value : this.values.entrySet()) {
                if (!written.contains(value.getKey())) {
                    updated.add(value.getKey() + "=" + value.getValue());
                }
            }
            Path parent = config.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.write(config, join(updated).getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new InstallException(ExitCode.TARGET, "Unable to update " + config, e);
        }
    }

    private static String join(List<String> lines) {
        StringBuilder builder = new StringBuilder();
        for (String line : lines) {
            builder.append(line).append(System.lineSeparator());
        }
        return builder.toString();
    }

}
