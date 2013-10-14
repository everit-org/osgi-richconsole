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

import java.awt.GraphicsEnvironment;
import java.util.Hashtable;

import org.everit.osgi.dev.richconsole.MenuItemService;
import org.everit.osgi.dev.richconsole.internal.settings.SettingsMenuItemServiceImpl;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

public class Activator implements BundleActivator {

    private BundleDeployerFrame bundleManager = null;

    private BundleDeployerServiceImpl bundleService;

    private ServiceRegistration<MenuItemService> settingsServiceRegistration;

    private SettingsMenuItemServiceImpl settingsMenuItemService;

    @Override
    public void start(BundleContext context) throws Exception {
        String stopAfterTests = System.getenv("EOSGI_STOP_AFTER_TESTS");
        if (Boolean.valueOf(stopAfterTests)) {
            return;
        }
        if (!GraphicsEnvironment.isHeadless()) {
            bundleService = new BundleDeployerServiceImpl(context.getBundle());
            bundleManager = new BundleDeployerFrame(bundleService);
            bundleManager.start(context);
            settingsMenuItemService = new SettingsMenuItemServiceImpl();
            settingsServiceRegistration =
                    context.registerService(MenuItemService.class, settingsMenuItemService,
                            new Hashtable<String, Object>());
        }
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        if (settingsMenuItemService != null) {
            settingsMenuItemService.close();
        }

        if (settingsServiceRegistration != null) {
            settingsServiceRegistration.unregister();
        }

        if (bundleManager != null) {
            bundleManager.close();
        }
        if (bundleService != null) {
            bundleService.close();
        }

    }

}
