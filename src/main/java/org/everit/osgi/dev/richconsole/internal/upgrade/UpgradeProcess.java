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
package org.everit.osgi.dev.richconsole.internal.upgrade;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.everit.osgi.dev.richconsole.internal.BundleData;
import org.everit.osgi.dev.richconsole.internal.BundleUtil;
import org.everit.osgi.dev.richconsole.internal.Logger;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.startlevel.BundleStartLevel;
import org.osgi.framework.startlevel.FrameworkStartLevel;
import org.osgi.framework.wiring.FrameworkWiring;

public class UpgradeProcess {

    private final UpgradeServiceImpl bundleDeployerService;

    private LinkedHashSet<Bundle> bundlesToStart = new LinkedHashSet<Bundle>();

    private int currentFrameworkStartLevelValue;

    private final FrameworkStartLevel frameworkStartLevel;

    private final FrameworkWiring frameworkWiring;

    private final int originalFrameworkStartLevelValue;

    private final FrameworkRefreshListener refreshListener;

    private boolean stateChanged = false;

    private final BundleContext systemBundleContext;

    public UpgradeProcess(final UpgradeServiceImpl bundleDeployerService,
            final BundleContext systemBundleContext) {
        this.bundleDeployerService = bundleDeployerService;
        this.systemBundleContext = systemBundleContext;

        // Refresh classes must be initialized first because they will be not available if the richconsole re-deploys
        // itself
        final AtomicBoolean refreshFinished = new AtomicBoolean(false);

        Lock refreshFinishLock = new ReentrantLock();
        Condition refreshFinishCondition = refreshFinishLock.newCondition();
        refreshListener = new FrameworkRefreshListener(refreshFinished, refreshFinishLock, refreshFinishCondition);

        Bundle systemBundle = systemBundleContext.getBundle();
        frameworkWiring = systemBundle.adapt(FrameworkWiring.class);

        frameworkStartLevel = systemBundle.adapt(FrameworkStartLevel.class);
        originalFrameworkStartLevelValue = frameworkStartLevel.getStartLevel();
        currentFrameworkStartLevelValue = originalFrameworkStartLevelValue;
    }

    public synchronized void finish() {
        if (!stateChanged) {
            bundleDeployerService.finishOngoingProcess();
            return;
        }
        Logger.info("Calling refresh on OSGi framework. All packages on uninstalled bundles should be re-wired");

        Lock refreshFinishLock = refreshListener.getRefreshFinishLock();
        AtomicBoolean refreshFinished = refreshListener.getRefreshFinished();
        Condition refreshFinishCondition = refreshListener.getRefreshFinishCondition();
        frameworkWiring.refreshBundles(null, new FrameworkListener[] { refreshListener });

        refreshFinishLock.lock();
        try {
            while (!refreshFinished.get()) {
                refreshFinishCondition.await();
            }
        } catch (InterruptedException e) {
            Logger.error("Interrupting waiting for framework refresh", e);
        } finally {
            refreshFinishLock.unlock();
        }

        if (currentFrameworkStartLevelValue != originalFrameworkStartLevelValue) {
            setFrameworkStartLevel(originalFrameworkStartLevelValue);
        }

        for (Bundle bundle : bundlesToStart) {
            try {
                if (bundle.getState() != Bundle.ACTIVE) {
                    bundle.start();
                }
            } catch (BundleException e) {
                Logger.error("Error during starting bundle " + bundle.toString(), e);
            }
        }

        bundleDeployerService.finishOngoingProcess();
    }

    private BundleData getBundleData(final File file) {
        File bundleLocation = null;
        if (file.isDirectory()) {
            bundleLocation = new File(file, "target/classes");
            if (!bundleLocation.exists()) {
                Logger.warn("Hot deployment failed. There is no target/classes child folder found under "
                        + file.getPath());
                return null;
            }
            File manifestFile = new File(bundleLocation, "META-INF/MANIFEST.MF");
            if (!manifestFile.exists()) {
                Logger.warn("Hot deployment failed. Manifest file could not be found: " + manifestFile.getPath());
                return null;
            }

            try {
                BundleData bundleData = BundleUtil.readBundleDataFromManifestFile(bundleLocation, manifestFile);
                if (bundleData.getSymbolicName() != null) {
                    return bundleData;
                } else {
                    Logger.warn("Location is not recognized as a maven bundle project: "
                            + bundleLocation.getAbsolutePath());
                }
            } catch (IOException e) {
                Logger.error("Could not deploy bundle from project location " + file.toString(), e);
                return null;
            }
        } else {
            JarFile jarFile = null;
            try {
                jarFile = new JarFile(file);
                Manifest manifest = jarFile.getManifest();
                BundleData bundleData = BundleUtil.readBundleDataFromManifest(file, manifest);
                if (bundleData.getSymbolicName() != null) {
                    return bundleData;
                } else {
                    Logger.warn("Jar file is not recognized as a bundle: " + file.getAbsolutePath());
                    return null;
                }
            } catch (IOException e) {
                Logger.error("Unrecognized file type", e);
                return null;
            } finally {
                if (jarFile != null) {
                    try {
                        jarFile.close();
                    } catch (IOException e) {
                        Logger.error("Cannot close jar file: " + bundleLocation.getAbsolutePath(), e);
                    }
                }
            }
        }
        return null;
    }

    /**
     * Deploys a bundle.
     * 
     * @param bundleLocationFile
     *            Location of a jar file or a maven project where target/classes contains every necessary entries.
     * @param startBundle
     *            Whether to try calling start on the installed bundle or not. If the bundle is a fragment bundle, start
     *            will not be called.
     * @param startLevel
     *            The new start level of the bundle. If null, the startlevel of the original bundle will be used or the
     *            startLevel of the framework when the process was started.
     * @return The deployed bundle.
     */
    public synchronized Bundle installBundle(final File bundleLocationFile, final boolean startBundle,
            final Integer startLevel) {
        stateChanged = true;
        if (startLevel != null && startLevel < currentFrameworkStartLevelValue) {
            setFrameworkStartLevel(startLevel);
        }
        Bundle installedBundle = null;
        BundleData bundleData = getBundleData(bundleLocationFile);

        String bundleBaseLocation;
        try {
            bundleBaseLocation = BundleUtil.getBundleLocationByFile(bundleData.getEvaluatedLocationFile());
        } catch (IOException e) {
            Logger.error("Could not convert bundle location to reference uri: " + bundleLocationFile, e);
            return null;
        }

        Bundle originalBundle = bundleDeployerService.getExistingBundleBySymbolicName(bundleData.getSymbolicName(),
                bundleData.getVersion(), bundleBaseLocation);
        if (originalBundle != null) {
            bundlesToStart.remove(originalBundle);

            BundleStartLevel originalBundleStartLevel = originalBundle.adapt(BundleStartLevel.class);
            int originalBundleStartLevelValue = originalBundleStartLevel.getStartLevel();
            if (originalBundleStartLevelValue < currentFrameworkStartLevelValue) {
                setFrameworkStartLevel(originalBundleStartLevelValue);
            }

            if (originalBundle.getLocation().equals(bundleBaseLocation)) {
                try {
                    if (originalBundle.getState() == Bundle.ACTIVE) {
                        Logger.info("Stopping already existing bundle " + originalBundle.toString());
                        originalBundle.stop();
                    }
                    Logger.info("Calling update on bundle " + originalBundle.toString());

                    originalBundle.update();
                    installedBundle = originalBundle;
                    BundleStartLevel installedBundleStartLevel = installedBundle.adapt(BundleStartLevel.class);
                    if (startLevel != null && !startLevel.equals(installedBundleStartLevel.getStartLevel())) {
                        installedBundleStartLevel.setStartLevel(startLevel);
                    }
                } catch (BundleException e) {
                    Logger.error("Error during deploying bundle: " + bundleBaseLocation, e);
                }
            } else {
                try {
                    Logger.info("Uninstalling Bundle " + originalBundle.getSymbolicName() + ":"
                            + originalBundle.getVersion().toString());

                    originalBundle.uninstall();
                    Logger.info("Installing bundle from '" + bundleBaseLocation.toString() + "'");
                    installedBundle = systemBundleContext.installBundle(bundleBaseLocation);
                    BundleStartLevel newBundleStartLevel = installedBundle.adapt(BundleStartLevel.class);
                    if (startLevel == null) {
                        newBundleStartLevel.setStartLevel(originalBundleStartLevelValue);
                    } else {
                        newBundleStartLevel.setStartLevel(startLevel);
                    }

                } catch (BundleException e) {
                    Logger.error("Error during deploying bundle: " + bundleBaseLocation, e);
                }
            }
        } else {
            try {
                Integer startLevelToUse = startLevel;
                if (startLevelToUse == null) {
                    startLevelToUse = originalFrameworkStartLevelValue;
                }
                Logger.info("Installing new bundle from folder '" + bundleBaseLocation + "' with startLevel "
                        + startLevelToUse);
                installedBundle = systemBundleContext.installBundle(bundleBaseLocation);
                BundleStartLevel bundleStartLevel = installedBundle.adapt(BundleStartLevel.class);
                bundleStartLevel.setStartLevel(startLevelToUse);
            } catch (BundleException e) {
                Logger.error("Error during deploying bundle: " + bundleBaseLocation, e);
            }
        }
        if (startBundle) {
            String fragmentHostHeader = installedBundle.getHeaders().get(Constants.FRAGMENT_HOST);
            if (fragmentHostHeader == null) {
                bundlesToStart.add(installedBundle);
            }
        }
        return installedBundle;
    }

    public void setFrameworkStartLevel(final int startLevel) {
        Logger.info("Setting framework startlevel to " + startLevel);
        final AtomicBoolean startLevelReached = new AtomicBoolean(false);
        final Lock lock = new ReentrantLock();
        final Condition startLevelReachedCondition = lock.newCondition();

        frameworkStartLevel.setStartLevel(startLevel, new FrameworkListener() {

            @Override
            public void frameworkEvent(final FrameworkEvent event) {
                lock.lock();
                int eventType = event.getType();
                if ((eventType == FrameworkEvent.STARTLEVEL_CHANGED) || (eventType == FrameworkEvent.ERROR)) {
                    if (eventType == FrameworkEvent.ERROR) {
                        Logger.error("Setting framework startlevel to " + startLevel + " finished with error: ",
                                event.getThrowable());
                    } else {
                        Logger.info("Setting framework startlevel to " + startLevel + " finished with success");
                    }
                    startLevelReached.set(true);
                    startLevelReachedCondition.signal();
                }
                lock.unlock();
            }
        });
        lock.lock();
        try {
            while (!startLevelReached.get()) {
                startLevelReachedCondition.await();
            }
        } catch (InterruptedException e) {
            Logger.error("Startlevel reaching wait interrupted", e);
        } finally {
            lock.unlock();
        }
        currentFrameworkStartLevelValue = startLevel;
    }

    /**
     * Uninstalling an existing bundle
     * 
     * @param symbolicName
     *            The symbolicName of the bundle
     * @param version
     *            The version of the bundle, optional. In case this parameter is null, the first bundle with the given
     *            symbolic name will be uninstalled.
     */
    public void uninstallBundle(final String symbolicName, final String version) {
        Bundle bundle = bundleDeployerService.getExistingBundleBySymbolicName(symbolicName, version, null);
        if (bundle != null) {
            stateChanged = true;
            Logger.info("Uninstalling bundle: " + bundle);
            BundleStartLevel bundleStartLevel = bundle.adapt(BundleStartLevel.class);
            if (bundleStartLevel.getStartLevel() < currentFrameworkStartLevelValue) {
                setFrameworkStartLevel(bundleStartLevel.getStartLevel());
            }
            try {
                bundle.uninstall();
            } catch (BundleException e) {
                Logger.error("Error during uninstalling bundle: " + bundle.toString(), e);
            }
        }
    }
}
