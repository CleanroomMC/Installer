package com.cleanroommc.installer.target;

/**
 * A failure the user can act on. Carries the exit code the process should end with.
 */
public class InstallException extends Exception {

    private static final long serialVersionUID = 1L;

    private final ExitCode exitCode;

    public InstallException(ExitCode exitCode, String message) {
        super(message);
        this.exitCode = exitCode;
    }

    public InstallException(ExitCode exitCode, String message, Throwable cause) {
        super(message, cause);
        this.exitCode = exitCode;
    }

    public ExitCode exitCode() {
        return this.exitCode;
    }

}
