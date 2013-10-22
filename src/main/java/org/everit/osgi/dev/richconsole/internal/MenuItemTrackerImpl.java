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

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;

import org.everit.osgi.dev.richconsole.MenuItemService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

public class MenuItemTrackerImpl implements ServiceTrackerCustomizer<MenuItemService, MenuItemService> {

    private ServiceTracker<MenuItemService, MenuItemService> menuItemServiceTracker;

    private final JPopupMenu popupMenu;

    private final BundleContext context;

    private final MainFrame mainFrame;

    private Map<ServiceReference<MenuItemService>, JMenuItem> menuItemByService =
            new HashMap<ServiceReference<MenuItemService>, JMenuItem>();

    public MenuItemTrackerImpl(final MainFrame mainFrame, final JPanel origin, final BundleContext context) {
        this.mainFrame = mainFrame;

        menuItemServiceTracker =
                new ServiceTracker<MenuItemService, MenuItemService>(context, MenuItemService.class, this);
        popupMenu = new JPopupMenu();
        origin.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                showMenu(origin, e.getX(), e.getY());

            }
        });
        this.context = context;
    }

    public void start() {
        menuItemServiceTracker.open();
    }

    public void showMenu(Component origin, int x, int y) {
        popupMenu.show(origin, x, y);
    }

    public void stop() {
        menuItemServiceTracker.close();
    }

    @Override
    public MenuItemService addingService(ServiceReference<MenuItemService> reference) {
        final MenuItemService menuItemService = context.getService(reference);
        String label = menuItemService.getLabel();
        JMenuItem menuItem = new JMenuItem(label);

        menuItem.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                menuItemService.itemFired(mainFrame.getConfigStore());
            }
        });
        popupMenu.add(menuItem);
        menuItemByService.put(reference, menuItem);
        return menuItemService;
    }

    @Override
    public void modifiedService(ServiceReference<MenuItemService> reference, MenuItemService service) {
        // Do nothing
    }

    @Override
    public void removedService(ServiceReference<MenuItemService> reference, MenuItemService service) {
        JMenuItem menuItem = menuItemByService.remove(reference);
        popupMenu.remove(menuItem);
        context.ungetService(reference);
    }
}
