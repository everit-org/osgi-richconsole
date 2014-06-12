/**
 * This file is part of Everit - OSGi Rich Console.
 *
 * Everit - OSGi Rich Console is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Everit - OSGi Rich Console is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Everit - OSGi Rich Console.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.everit.osgi.dev.richconsole.internal;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.osgi.framework.FrameworkEvent;

public class BundleUtil {

    public static String convertFrameworkEventTypeCode(final int code) {
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

    public static String getBundleLocationByFile(final File file) throws IOException {
        return "reference:" + file.getAbsoluteFile().toURI().toString();
    }

    public static BundleData readBundleDataFromManifest(final File bundleLocationFile, final Manifest manifest) {
        Attributes mainAttributes = manifest.getMainAttributes();
        String symbolicName = mainAttributes.getValue("Bundle-SymbolicName");
        if (symbolicName != null) {
            int semicolonIndex = symbolicName.indexOf(';');
            if (semicolonIndex > 0) {
                symbolicName = symbolicName.substring(0, semicolonIndex);
            }
        }
        String version = mainAttributes.getValue("Bundle-Version");

        return new BundleData(bundleLocationFile, symbolicName, version);
    }

    public static BundleData readBundleDataFromManifestFile(final File bundleLocationFile, final File manifestFile)
            throws IOException {
        FileInputStream manifestIS = null;
        try {
            manifestIS = new FileInputStream(manifestFile);
            return BundleUtil.readBundleDataFromManifest(bundleLocationFile, new Manifest(manifestIS));
        } finally {
            if (manifestIS != null) {
                try {
                    manifestIS.close();
                } catch (IOException e) {
                    Logger.error("Could not close manifest file " + manifestFile.toString(), e);
                }
            }
        }
    }
}
