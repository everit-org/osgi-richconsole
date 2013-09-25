package org.everit.osgi.dev.richconsole.internal.settings;

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

import org.everit.osgi.dev.richconsole.ConfigStore;
import org.everit.osgi.dev.richconsole.MenuItemService;

public class SettingsMenuItemServiceImpl implements MenuItemService {

    private SettingsFrame settingsFrame;
    
    private synchronized SettingsFrame getOrCreateSettingsFrame(ConfigStore configStore) {
        if (settingsFrame == null) {
            settingsFrame = new SettingsFrame(configStore);
        }
        return settingsFrame;
    }

    @Override
    public String getLabel() {
        return "Global";
    }

    @Override
    public void itemFired(ConfigStore configStore) {
        SettingsFrame tmpSettingsFrame = getOrCreateSettingsFrame(configStore);
        tmpSettingsFrame.setVisible(true);
        tmpSettingsFrame.requestFocus();
    }

    public void close() {
        if (settingsFrame != null) {
            settingsFrame.dispose();
        }
    }
}