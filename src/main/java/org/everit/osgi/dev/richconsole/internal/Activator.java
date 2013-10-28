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

import org.everit.osgi.dev.richconsole.RichConsoleService;
import org.everit.osgi.dev.richconsole.internal.settings.SettingsExtensionImpl;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

public class Activator implements BundleActivator {

    private BundleDeployerFrame bundleManager = null;

    private ServiceRegistration<RichConsoleService> richConsoleSR;

    private BundleDeployerServiceImpl bundleService;
    
    private SettingsExtensionImpl settingsExtension;

    @Override
    public void start(final BundleContext context) throws Exception {
        String stopAfterTests = System.getenv("EOSGI_STOP_AFTER_TESTS");
        if (Boolean.valueOf(stopAfterTests)) {
            return;
        }
        if (!GraphicsEnvironment.isHeadless()) {
            bundleService = new BundleDeployerServiceImpl(context.getBundle());
            bundleManager = new BundleDeployerFrame(bundleService);
            bundleManager.start(context);

            richConsoleSR =
                    context.registerService(RichConsoleService.class, bundleManager, new Hashtable<String, Object>());

            settingsExtension = new SettingsExtensionImpl();
            settingsExtension.init(bundleManager);
        }
    }

    @Override
    public void stop(final BundleContext context) throws Exception {
        if (settingsExtension != null) {
            settingsExtension.close();
            settingsExtension = null;
        }
        
        if (richConsoleSR != null) {
            richConsoleSR.unregister();
            richConsoleSR = null;
        }

        if (bundleManager != null) {
            bundleManager.close();
            bundleManager = null;
        }
        if (bundleService != null) {
            bundleService.close();
            bundleService = null;
        }

    }

}
