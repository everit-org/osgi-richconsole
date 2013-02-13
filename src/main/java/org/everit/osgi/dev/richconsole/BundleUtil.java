package org.everit.osgi.dev.richconsole;

/*
 * Copyright (c) 2011, Everit Kft.
 *
 * All rights reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.osgi.framework.FrameworkEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BundleUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(BundleUtil.class);
    
    public static BundleData readBundleDataFromManifest(File manifestFile) throws IOException {
        FileInputStream manifestIS = null;
        try {
            manifestIS = new FileInputStream(manifestFile);
            Manifest manifest = new Manifest(manifestIS);
            Attributes mainAttributes = manifest.getMainAttributes();
            String symbolicName = mainAttributes.getValue("Bundle-SymbolicName");
            String version = mainAttributes.getValue("Bundle-Version");
            return new BundleData(symbolicName, version);
        } finally {
            if (manifestIS != null) {
                try {
                    manifestIS.close();
                } catch (IOException e) {
                    LOGGER.error("Could not close manifest file " + manifestFile.toString(), e);
                }
            }
        }
    }

    public static String convertFrameworkEventTypeCode(int code) {
        switch (code) {
        case FrameworkEvent.ERROR:
            return "ERROR";
        case FrameworkEvent.INFO:
            return "INFO";
        case FrameworkEvent.PACKAGES_REFRESHED:
            return "PACKAGES_REFRESHED";
        case FrameworkEvent.STARTED:
            return "STARTED";
        case FrameworkEvent.STARTLEVEL_CHANGED:
            return "STARTLEVEL_CHANGED";
        case FrameworkEvent.STOPPED:
            return "STOPPED";
        case FrameworkEvent.STOPPED_BOOTCLASSPATH_MODIFIED:
            return "STOPPED_BOOTCLASSPATH_MODIFIED";
        case FrameworkEvent.STOPPED_UPDATE:
            return "STOPPED_UPDATE";
        case FrameworkEvent.WAIT_TIMEDOUT:
            return "WAIT_TIMEDOUT";
        case FrameworkEvent.WARNING:
            return "WARNING";
        default:
            break;
        }
        return String.valueOf(code);
    }
}