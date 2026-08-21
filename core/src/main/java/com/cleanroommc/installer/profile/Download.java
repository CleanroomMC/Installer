package com.cleanroommc.installer.profile;

import com.cleanroommc.installer.maven.Coordinate;

public final class Download {

    public String path;
    public String url;
    public String sha1;
    public long size;

    public String path(String coordinate) {
        if (this.path != null && !this.path.isEmpty()) {
            return this.path;
        }
        return Coordinate.parse(coordinate).path();
    }

    /** An empty url means "this artifact is embedded in the installer jar under /maven". */
    public boolean embedded() {
        return this.url == null || this.url.trim().isEmpty();
    }

}
