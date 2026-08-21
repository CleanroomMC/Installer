package com.cleanroommc.installer.version;

public final class RemoteVersion {

    private final String id;
    private final String installerUrl;

    public RemoteVersion(String id, String installerUrl) {
        this.id = id;
        this.installerUrl = installerUrl;
    }

    public String id() {
        return this.id;
    }

    public String installerUrl() {
        return this.installerUrl;
    }

    @Override
    public String toString() {
        return this.id;
    }

}
