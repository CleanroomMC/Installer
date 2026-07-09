/*
 * Installer
 * Copyright (c) 2016-2018.
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation version 2.1
 * of the License.
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
 */
package net.minecraftforge.installer.json;

import org.apache.commons.lang3.SystemUtils;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Version {
    private String id;
    private Map<String, Download> downloads;
    private Library[] libraries;

    public String getId() {
        return id;
    }

    public Download getDownload(String key) {
        return downloads == null ? null : downloads.get(key);
    }

    public Library[] getLibraries() {
        return libraries == null ? new Library[0] : libraries;
    }

    private static OsType currentOS;

    public static OsType getCurrentOS() {
        if (currentOS == null) {
            if (SystemUtils.IS_OS_WINDOWS) currentOS = OsType.windows;
            else if (SystemUtils.IS_OS_MAC) currentOS = OsType.osx;
            else if (SystemUtils.IS_OS_LINUX) currentOS = OsType.linux;
            else currentOS = OsType.UNKNOWN;
        }
        return currentOS;
    }

    public enum OsType {
        windows, osx, linux, UNKNOWN;
    }

    public static class Download {
        private String sha1;
        private int size;
        private String url;
        private boolean provided = false;

        public String getSha1() {
            return sha1;
        }

        public int getSize() {
            return size;
        }

        public String getUrl() {
            return url == null || provided ? "" : url;
        }

        public boolean getProvided() {
            return provided;
        }
    }

    public static class LibraryDownload extends Download {
        private String path;

        public String getPath() {
            return path;
        }

        public void setPath(String value) {
            this.path = value;
        }
    }

    public static class Library {
        private Artifact name;
        private Downloads downloads;
        private Rule[] rules;

        public Artifact getName() {
            return name;
        }

        public Downloads getDownloads() {
            return downloads;
        }

        public boolean shouldDownload() {
            if (rules == null || rules.length == 0)
                return true;

            boolean allow = true;
            OsType currentOS = getCurrentOS();
            for (Rule rule : rules) {
                if (rule.os == null || currentOS == rule.getOs().getName()) {
                    allow = rule.getAction() == Rule.Action.allow;
                }
            }
            return allow;
        }
    }

    public static class Rule {
        private Action action;
        private Os os;

        public Action getAction() {
            return action;
        }

        public Os getOs() {
            return os;
        }

        public enum Action {
            allow, disallow;
        }
    }

    public static class Os {
        private OsType name;

        public OsType getName() {
            return name;
        }
    }

    public static class Downloads {
        private LibraryDownload artifact;
        private Map<String, LibraryDownload> classifiers;

        public LibraryDownload getArtifact() {
            return artifact;
        }

        public Set<String> getClassifiers() {
            return classifiers == null ? new HashSet<>() : classifiers.keySet();
        }
    }
}
