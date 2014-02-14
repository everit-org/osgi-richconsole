/**
 * This file is part of Everit - OSGi Rich Console.
 *
 * Everit - OSGi Rich Console is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Everit - OSGi Rich Console is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Everit - OSGi Rich Console.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.everit.osgi.dev.richconsole.internal;

import java.awt.GraphicsEnvironment;
import java.util.Hashtable;

import org.everit.osgi.dev.richconsole.RichConsoleConstants;
import org.everit.osgi.dev.richconsole.RichConsoleService;
import org.everit.osgi.dev.richconsole.internal.settings.SettingsExtensionImpl;
import org.everit.osgi.dev.richconsole.internal.upgrade.TCPServer;
import org.everit.osgi.dev.richconsole.internal.upgrade.UpgradeServiceImpl;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

public class Activator implements BundleActivator {

    private BundleDeployerFrame bundleManager = null;

    private ServiceRegistration<RichConsoleService> richConsoleSR = null;

    private SettingsExtensionImpl settingsExtension = null;

    private TCPServer tcpServer = null;

    private UpgradeServiceImpl upgradeService = null;

    private int getTCPPort() {
        int tcpPort = 0;
        String tcpPortEnv = System.getenv(RichConsoleConstants.ENV_EOSGI_UPGRADE_SERVICE_PORT);
        if (tcpPortEnv != null) {
            try {
                tcpPort = Integer.valueOf(tcpPortEnv);
            } catch (NumberFormatException e) {
                Logger.error("Invalid TCP Port environment variable value: " + tcpPortEnv, e);
                tcpPort = -1;
            }
        }
        return tcpPort;
    }

    @Override
    public void start(final BundleContext context) throws Exception {
        String stopAfterTests = System.getenv(RichConsoleConstants.ENV_EOSGI_STOP_AFTER_TESTS);
        if (Boolean.valueOf(stopAfterTests)) {
            return;
        }
        upgradeService = new UpgradeServiceImpl(context.getBundle());

        if (!GraphicsEnvironment.isHeadless()) {
            bundleManager = new BundleDeployerFrame(upgradeService);
            bundleManager.start(context);

            richConsoleSR =
                    context.registerService(RichConsoleService.class, bundleManager, new Hashtable<String, Object>());

            settingsExtension = new SettingsExtensionImpl();
            settingsExtension.init(bundleManager);
        }

        int tcpPort = getTCPPort();
        if (tcpPort >= 0) {
            tcpServer = new TCPServer(upgradeService, tcpPort);
            int localPort = tcpServer.getLocalPort();
            Logger.info("Richconsole is listening on port " + localPort);
            if (bundleManager != null) {
                bundleManager.setTCPPort(localPort);
            }
        }
    }

    @Override
    public void stop(final BundleContext context) throws Exception {
        if (tcpServer != null) {
            tcpServer.close();
        }

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
        if (upgradeService != null) {
            upgradeService.close();
            upgradeService = null;
        }

    }

}
