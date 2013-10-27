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

/**
 * In case someone wants to extend the richconsole with new functionality an OSGi service should be provided with this
 * interface.
 */
public interface ExtensionService {

    /**
     * Closing the extension. This is called normally when the richconsole bundle stops. Every resources should be
     * released here.
     */
    void close();

    /**
     * Providing the label that will appear on the context menu.
     * 
     * @return The label that appears on the menu item or null if the extension does not appear in the context menu.
     */
    String getMenuItemLabel();

    /**
     * Initializing the extension.
     * 
     * @param configStore
     *            The current configuration
     */
    void init(ConfigStore configStore);

    /**
     * The menu item was fired.
     */
    void menuItemFired();
}
