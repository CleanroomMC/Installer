package com.cleanroommc.installer.target;

/**
 * Process exit codes. Stable: pack scripts and CI depend on them.
 */
public enum ExitCode {

    SUCCESS(0),
    INTERNAL(1),
    USAGE(2),
    NETWORK(3),
    VERIFICATION(4),
    TARGET(5),
    JAVA(6),
    CANCELLED(7);

    private final int code;

    ExitCode(int code) {
        this.code = code;
    }

    public int code() {
        return this.code;
    }

}
