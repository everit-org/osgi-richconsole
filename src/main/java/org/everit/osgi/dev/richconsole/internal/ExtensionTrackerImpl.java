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

import org.everit.osgi.dev.richconsole.ExtensionService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

public class ExtensionTrackerImpl implements ServiceTrackerCustomizer<ExtensionService, ExtensionService> {

    private final BundleContext context;

    private ServiceTracker<ExtensionService, ExtensionService> extensionServiceTracker;

    private final MainFrame mainFrame;

    private Map<ServiceReference<ExtensionService>, JMenuItem> menuItemByService =
            new HashMap<ServiceReference<ExtensionService>, JMenuItem>();

    private final JPopupMenu popupMenu;

    public ExtensionTrackerImpl(final MainFrame mainFrame, final JPanel origin, final BundleContext context) {
        this.mainFrame = mainFrame;

        extensionServiceTracker =
                new ServiceTracker<ExtensionService, ExtensionService>(context, ExtensionService.class, this);
        popupMenu = new JPopupMenu();
        origin.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(final MouseEvent e) {
                showMenu(origin, e.getX(), e.getY());

            }
        });
        this.context = context;
    }

    @Override
    public ExtensionService addingService(final ServiceReference<ExtensionService> reference) {
        final ExtensionService extensionService = context.getService(reference);
        extensionService.init(mainFrame.getConfigStore());
        String label = extensionService.getMenuItemLabel();
        JMenuItem menuItem = new JMenuItem(label);

        menuItem.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent e) {
                extensionService.menuItemFired();
            }
        });
        popupMenu.add(menuItem);
        menuItemByService.put(reference, menuItem);
        return extensionService;
    }

    @Override
    public void modifiedService(final ServiceReference<ExtensionService> reference, final ExtensionService service) {
        // Do nothing
    }

    @Override
    public void removedService(final ServiceReference<ExtensionService> reference, final ExtensionService service) {

        service.close();
        JMenuItem menuItem = menuItemByService.remove(reference);
        popupMenu.remove(menuItem);
        context.ungetService(reference);
    }

    public void showMenu(final Component origin, final int x, final int y) {
        popupMenu.show(origin, x, y);
    }

    public void start() {
        extensionServiceTracker.open();
    }

    public void stop() {
        extensionServiceTracker.close();
    }
}
