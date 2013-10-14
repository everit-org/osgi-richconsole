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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.swing.JDialog;
import javax.swing.JOptionPane;

import org.everit.osgi.dev.richconsole.ConfigPropertyChangeListener;
import org.everit.osgi.dev.richconsole.ConfigStore;
import org.osgi.framework.BundleContext;

public class ConfigStoreImpl implements ConfigStore {
    
    private static final String DEFAULT_SETTINGS_FILE_NAME = "richConsoleSettings.properties";

    private List<ConfigPropertyChangeListener> listeners = new ArrayList<ConfigPropertyChangeListener>();
    
    private final BundleContext richConsoleContext;

    private ReadWriteLock listenerLocker = new ReentrantReadWriteLock(false);

    private ReadWriteLock propertiesLocker = new ReentrantReadWriteLock(false);

    private Properties properties = new Properties();
    
    private final File settingsFile;

    
    public ConfigStoreImpl(BundleContext richConsoleContext) {
        this.richConsoleContext = richConsoleContext;
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
            } catch (FileNotFoundException e) {
                // TODO
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } finally {
                if (fin != null) {
                    try {
                        fin.close();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    @Override
    public String getProperty(String key) {
        Lock readLock = propertiesLocker.readLock();
        readLock.lock();
        String result = properties.getProperty(key);
        readLock.unlock();
        return result;
    }

    @Override
    public void setProperty(String key, String value) {
        Lock propertiesWriteLock = propertiesLocker.writeLock();
        propertiesWriteLock.lock();
        properties.setProperty(key, value);

        Lock listenerReadLock = listenerLocker.readLock();
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

    public void persist() {
        FileOutputStream fout = null;
        try {
            fout = new FileOutputStream(settingsFile);
            properties.store(fout, null);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fout != null) {
                try {
                    fout.close();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
        
    }

    @Override
    public void addPropertyChangeListener(ConfigPropertyChangeListener listener) {
        Lock writeLock = listenerLocker.writeLock();
        writeLock.lock();
        listeners.add(listener);
        writeLock.unlock();
    }

    @Override
    public void removePropertyChangeListener(ConfigPropertyChangeListener listener) {
        Lock writeLock = listenerLocker.writeLock();
        writeLock.lock();
        listeners.remove(listener);
        writeLock.unlock();
    }
    
    @Override
    public String getSettingsFilePath() {        
        return settingsFile.getAbsolutePath();
    }
}
