package org.everit.osgi.dev.richconsole.internal;

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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.startlevel.FrameworkStartLevel;

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
        return file.getCanonicalFile().toURI().toString();
    }

    public static BundleData readBundleDataFromManifest(final Manifest manifest) {
        Attributes mainAttributes = manifest.getMainAttributes();
        String symbolicName = mainAttributes.getValue("Bundle-SymbolicName");
        String version = mainAttributes.getValue("Bundle-Version");
        return new BundleData(symbolicName, version);
    }

    public static BundleData readBundleDataFromManifestFile(final File manifestFile) throws IOException {
        FileInputStream manifestIS = null;
        try {
            manifestIS = new FileInputStream(manifestFile);
            return BundleUtil.readBundleDataFromManifest(new Manifest(manifestIS));
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

    public static void setFrameworkStartLevel(final FrameworkStartLevel frameworkStartLevel, final int startLevel) {
        Logger.info("Setting framework startlevel to " + startLevel);
        final AtomicBoolean startLevelReached = new AtomicBoolean(false);
        final Lock lock = new ReentrantLock();
        final Condition startLevelReachedCondition = lock.newCondition();

        frameworkStartLevel.setStartLevel(startLevel, new FrameworkListener() {

            @Override
            public void frameworkEvent(final FrameworkEvent event) {
                lock.lock();
                int eventType = event.getType();
                if ((eventType == FrameworkEvent.STARTLEVEL_CHANGED) || (eventType == FrameworkEvent.ERROR)) {
                    if (eventType == FrameworkEvent.ERROR) {
                        Logger.error("Setting framework startlevel to " + startLevel + " finished with error: ", event.getThrowable());
                    } else {
                        Logger.info("Setting framework startlevel to " + startLevel + " finished with success");
                    }
                    startLevelReached.set(true);
                    startLevelReachedCondition.signal();
                }
                lock.unlock();
            }
        });
        lock.lock();
        try {
            while (!startLevelReached.get()) {
                startLevelReachedCondition.await();
            }
        } catch (InterruptedException e) {
            Logger.error("Startlevel reaching wait interrupted", e);
        } finally {
            lock.unlock();
        }
    }
}
