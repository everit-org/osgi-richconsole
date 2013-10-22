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
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.everit.osgi.dev.richconsole.ConfigPropertyChangeListener;
import org.everit.osgi.dev.richconsole.ConfigStore;
import org.osgi.framework.BundleContext;

public class ConfigStoreImpl implements ConfigStore {

    private static final String DEFAULT_SETTINGS_FILE_NAME = "richConsoleSettings.properties";

    private static final String SETTINGS_FILE_COMMENT = "Settings file of EOSGi Richconsole";

    private ReadWriteLock listenerLocker = new ReentrantReadWriteLock(false);

    private List<ConfigPropertyChangeListener> listeners = new ArrayList<ConfigPropertyChangeListener>();

    private Properties properties = new Properties();

    private ReadWriteLock propertiesLocker = new ReentrantReadWriteLock(false);

    private final File settingsFile;

    public ConfigStoreImpl(final BundleContext richConsoleContext) {
        String settingsFilePathSysProp = System.getProperty(ConfigStore.SYSPROP_SETTINGS_FILE_PATH);
        if (settingsFilePathSysProp == null) {
            settingsFile = richConsoleContext.getDataFile(DEFAULT_SETTINGS_FILE_NAME);
        } else {
            settingsFile = new File(settingsFilePathSysProp);
        }
        if (settingsFile.exists()) {
            FileInputStream fin = null;
            try {
                fin = new FileInputStream(settingsFile);
                properties.load(fin);
            } catch (IOException e) {
                Logger.error("Cannot load settings file: " + settingsFile.getAbsolutePath(), e);
            } finally {
                if (fin != null) {
                    try {
                        fin.close();
                    } catch (IOException e) {
                        Logger.error("Error closing settings file after loading: " + settingsFile.getAbsolutePath(), e);
                    }
                }
            }
        } else {
            File parentFile = settingsFile.getParentFile();
            parentFile.mkdirs();
            FileOutputStream fout = null;
            try {
                fout = new FileOutputStream(settingsFile);
                properties.store(fout, SETTINGS_FILE_COMMENT);
            } catch (IOException e) {
                Logger.error("Error saving settings file: " + settingsFile.getAbsolutePath(), e);
            } finally {
                if (fout != null) {
                    try {
                        fout.close();
                    } catch (IOException e) {
                        Logger.error("Error closing settings file after saving it: " + settingsFile.getAbsolutePath(),
                                e);
                    }
                }
            }
        }
    }

    @Override
    public void addPropertyChangeListener(final ConfigPropertyChangeListener listener) {
        Lock writeLock = listenerLocker.writeLock();
        writeLock.lock();
        listeners.add(listener);
        writeLock.unlock();
    }

    public void exportToFile(final File file) {
        Lock readLock = propertiesLocker.readLock();
        readLock.lock();
        FileOutputStream fout = null;
        try {
            fout = new FileOutputStream(file);
            properties.store(fout, SETTINGS_FILE_COMMENT);
        } catch (IOException e) {
            Logger.error("Error during exporting configuration to file " + file.toString(), e);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public String getProperty(final String key) {
        Lock readLock = propertiesLocker.readLock();
        readLock.lock();
        String result = properties.getProperty(key);
        readLock.unlock();
        return result;
    }

    @Override
    public String getSettingsFilePath() {
        return settingsFile.getAbsolutePath();
    }

    public void importFromFile(final File file, final boolean cleanImport) {
        Lock writeLock = propertiesLocker.writeLock();
        writeLock.lock();

        Set<Object> droppedProperties = new HashSet<Object>(properties.keySet());
        Map<Object, Object> modifiedProperties = new HashMap<Object, Object>();
        Map<Object, Object> addedProperties = new HashMap<Object, Object>();

        FileInputStream fin = null;
        try {
            fin = new FileInputStream(file);
            Properties newProperties = new Properties();
            newProperties.load(fin);

            if (cleanImport) {
                properties.clear();
            }

            for (Entry<Object, Object> newEntry : newProperties.entrySet()) {
                boolean modified = droppedProperties.remove(newEntry.getKey());
                Object newEntryKey = newEntry.getKey();
                Object newEntryValue = newEntry.getValue();
                if (modified) {
                    modifiedProperties.put(newEntryKey, newEntryValue);
                } else {
                    addedProperties.put(newEntryKey, newEntryValue);
                }
                properties.put(newEntryKey, newEntry.getValue());
            }
            persist();

            Lock listenerReadLock = listenerLocker.readLock();
            listenerReadLock.lock();

            for (ConfigPropertyChangeListener listener : listeners) {
                for (Entry<Object, Object> addedProperty : addedProperties.entrySet()) {
                    try {
                        listener.propertyChanged((String) addedProperty.getKey(), (String) addedProperty.getValue());
                    } catch (RuntimeException e) {
                        Logger.error("Error during calling configuration change listener with parameters key='"
                                + addedProperty.getKey() + "'; value='" + addedProperty.getValue() + "'", e);
                    }
                }
                for (Object droppedProperty : droppedProperties) {
                    try {
                        listener.propertyChanged((String) droppedProperty, null);
                    } catch (RuntimeException e) {
                        Logger.error("Error during calling configuration change listener with parameters key='"
                                + droppedProperty + "'; value=null", e);
                    }
                }
                for (Entry<Object, Object> entry : modifiedProperties.entrySet()) {
                    try {
                        listener.propertyChanged((String) entry.getKey(), (String) entry.getValue());
                    } catch (RuntimeException e) {
                        Logger.error(
                                "Error during calling configuration change listener with parameters key='"
                                        + entry.getKey() + "'; value='" + entry.getValue() + "'", e);
                    }
                }
            }
            listenerReadLock.unlock();

        } catch (IOException e) {
            Logger.error("Error during importing settings from file " + file.toString(), e);
        } finally {
            writeLock.unlock();
        }

    }

    public void persist() {
        FileOutputStream fout = null;
        try {
            fout = new FileOutputStream(settingsFile);
            properties.store(fout, null);
        } catch (IOException e) {
            Logger.error("Error storing settings in file: " + settingsFile.getAbsolutePath(), e);
        } finally {
            if (fout != null) {
                try {
                    fout.close();
                } catch (IOException e) {
                    Logger.error(
                            "Error during closing settings file after storing it: " + settingsFile.getAbsolutePath(), e);
                }
            }
        }
    }

    @Override
    public void removePropertyChangeListener(final ConfigPropertyChangeListener listener) {
        Lock writeLock = listenerLocker.writeLock();
        writeLock.lock();
        listeners.remove(listener);
        writeLock.unlock();
    }

    @Override
    public void setProperty(final String key, final String value) {
        Lock propertiesWriteLock = propertiesLocker.writeLock();
        propertiesWriteLock.lock();
        properties.setProperty(key, value);

        Lock listenerReadLock = listenerLocker.readLock();
        persist();
        listenerReadLock.lock();
        Iterator<ConfigPropertyChangeListener> iterator = listeners.iterator();
        try {
            while (iterator.hasNext()) {
                ConfigPropertyChangeListener listener = iterator.next();
                listener.propertyChanged(key, value);
            }
        } finally {
            listenerReadLock.unlock();
            propertiesWriteLock.unlock();
        }
    }
}
