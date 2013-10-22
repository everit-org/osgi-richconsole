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
 * interface. By clicking on the deployer window, a context menu will appear that will contain each MenuItem service.
 */
public interface MenuItemService {

    /**
     * Providing the label that will appear on the menu item.
     * 
     * @return The label that appears on the menu item.
     */
    String getLabel();

    void itemFired(ConfigStore configStore);
}
