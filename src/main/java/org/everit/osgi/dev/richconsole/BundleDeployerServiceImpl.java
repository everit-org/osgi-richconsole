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

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleException;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.wiring.FrameworkWiring;
import org.osgi.util.tracker.BundleTracker;
import org.osgi.util.tracker.BundleTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BundleDeployerServiceImpl implements Closeable {

    private static final Logger LOGGER = LoggerFactory.getLogger(BundleDeployerServiceImpl.class);

    private class Tracker extends BundleTracker {

        private Map<String, List<Long>> bundleDataBySymbolicName = new ConcurrentHashMap<String, List<Long>>();

        public Tracker(BundleContext context, int stateMask, BundleTrackerCustomizer customizer) {
            super(context, stateMask, customizer);
        }

        @Override
        public Object addingBundle(Bundle bundle, BundleEvent event) {
            String symbolicName = bundle.getSymbolicName();
            List<Long> bundleIdList = bundleDataBySymbolicName.get(symbolicName);
            if (bundleIdList == null) {
                bundleIdList = new ArrayList<Long>();
                bundleDataBySymbolicName.put(symbolicName, bundleIdList);
            }
            bundleIdList.add(bundle.getBundleId());
            return super.addingBundle(bundle, event);
        }

        @Override
        public void removedBundle(Bundle bundle, BundleEvent event, Object object) {
            super.remove(bundle);
            String symbolicName = bundle.getSymbolicName();
            List<Long> list = bundleDataBySymbolicName.get(symbolicName);
            list.remove(bundle.getBundleId());
            if (list.size() == 0) {
                bundleDataBySymbolicName.remove(symbolicName);
            }
        }

        public List<Long> getBundleIdsBySymbolicName(String symbolicName) {
            return bundleDataBySymbolicName.get(symbolicName);
        }
    }

    private static class RefreshListener implements FrameworkListener {

        private final AtomicBoolean refreshFinished;

        public RefreshListener(AtomicBoolean refreshFinished) {
            super();
            this.refreshFinished = refreshFinished;
        }

        @Override
        public void frameworkEvent(FrameworkEvent event) {
            int eventType = event.getType();
            if (eventType == FrameworkEvent.ERROR || eventType == FrameworkEvent.PACKAGES_REFRESHED) {
                refreshFinished.set(true);
                LOGGER.info("Framework refresh finished with code "
                        + BundleUtil.convertFrameworkEventTypeCode(eventType));
            } else {
                StringBuilder sb = new StringBuilder("Event caught during refreshing packages with data:");
                if (event.getBundle() != null) {
                    sb.append("\n\tBundle: ").append(event.getBundle().toString());
                }
                if (event.getSource() != null) {
                    sb.append("\n\tSource object: ").append(event.getSource().toString());
                }
                sb.append("\n\tEvent type: ").append(BundleUtil.convertFrameworkEventTypeCode(event.getType()));
                if (event.getThrowable() != null) {
                    LOGGER.error(sb.toString(), event.getThrowable());
                } else {
                    LOGGER.info(sb.toString());
                }

            }
        }
    }

    private final Bundle systemBundle;

    private final Tracker tracker;

    public BundleDeployerServiceImpl(Bundle consoleBundle) {
        this.systemBundle = consoleBundle.getBundleContext().getBundle(0);
        this.tracker =
                new Tracker(consoleBundle.getBundleContext(), Bundle.ACTIVE | Bundle.INSTALLED | Bundle.RESOLVED
                        | Bundle.STARTING | Bundle.STOPPING, null);
        this.tracker.open();

    }

    private Bundle deployBundle(BundleData bundleData, File bundleLocationFile) {
        String bundleLocation;
        try {
            bundleLocation = bundleLocationFile.getCanonicalFile().toURI().toURL().toExternalForm();
        } catch (MalformedURLException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
            return null;
        } catch (IOException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
            return null;
        }

        String symbolicName = bundleData.getSymbolicName();
        List<Long> existingBundleIds = tracker.getBundleIdsBySymbolicName(symbolicName);
        Bundle selectedBundle = null;
        BundleContext systemBundleContext = systemBundle.getBundleContext();
        if (existingBundleIds != null) {
            if (existingBundleIds.size() == 1) {
                selectedBundle = systemBundleContext.getBundle(existingBundleIds.get(0));
            }
            Iterator<Long> iterator = existingBundleIds.iterator();
            while (iterator.hasNext() && selectedBundle == null) {
                Long existingBundleId = iterator.next();
                Bundle bundle = systemBundleContext.getBundle(existingBundleId);
                String existingBundleLocation = bundle.getLocation();

                if (existingBundleLocation.equals(bundleLocation)) {
                    selectedBundle = bundle;
                }

                if (selectedBundle == null) {
                    String existingBundleVersion = bundle.getVersion().toString();
                    if (existingBundleVersion.equals(bundleData.getVersion())) {
                        selectedBundle = bundle;
                    }
                }
            }
            if (selectedBundle == null && existingBundleIds.size() > 0) {
                selectedBundle = systemBundleContext.getBundle(existingBundleIds.get(0));
            }
        }

        if (selectedBundle != null) {
            if (selectedBundle.getLocation().equals(bundleLocation)) {
                try {
                    if (selectedBundle.getState() == Bundle.ACTIVE) {
                        LOGGER.info("Stopping already existing bundle " + selectedBundle.toString());
                        selectedBundle.stop();
                    }
                    LOGGER.info("Calling update on bundle " + selectedBundle.toString());
                    selectedBundle.update();
                    return selectedBundle;
                } catch (BundleException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            } else {
                try {
                    LOGGER.info("Uninstalling Bundle " + selectedBundle.getSymbolicName() + ":"
                            + selectedBundle.getVersion().toString());
                    selectedBundle.uninstall();
                    LOGGER.info("Installing bundle from '" + bundleLocation.toString() + "'");
                    Bundle installedBundle = systemBundleContext.installBundle(bundleLocation);
                    return installedBundle;
                } catch (BundleException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        } else {
            try {
                LOGGER.info("Installing bundle from folder '" + bundleLocation + "'");
                Bundle installedBundle = systemBundleContext.installBundle(bundleLocation);
                return installedBundle;
            } catch (BundleException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        return null;
    }

    public void deployBundles(List<File> fileObjects) {
        final AtomicBoolean refreshFinished = new AtomicBoolean(false);
        FrameworkListener frameworkListener = new RefreshListener(refreshFinished);

        FrameworkWiring frameworkWiring = (FrameworkWiring) systemBundle.adapt(FrameworkWiring.class);
        List<Bundle> installedBundles = new ArrayList<Bundle>();
        BundleData bundleData = null;

        for (File fileObject : fileObjects) {
            File bundleLocation = null;
            if (fileObject.isDirectory()) {
                bundleLocation = new File(fileObject, "target/classes");
                if (!bundleLocation.exists()) {
                    LOGGER.warn("Hot deployment failed. There is no target/classes child folder found under "
                            + fileObject.getPath());
                    return;
                }
                File manifestFile = new File(bundleLocation, "META-INF/MANIFEST.MF");
                if (!manifestFile.exists()) {
                    LOGGER.warn("Hot deployment failed. Manifest file could not be found: " + manifestFile.getPath());
                    return;
                }

                try {
                    bundleData = BundleUtil.readBundleDataFromManifestFile(manifestFile);
                } catch (IOException e) {
                    LOGGER.error("Could not deploy bundle from project location " + fileObject.toString(), e);
                    return;
                }
            } else {
                JarFile jarFile = null;
                try {
                    jarFile = new JarFile(fileObject);
                    Manifest manifest = jarFile.getManifest();
                    bundleData = BundleUtil.readBundleDataFromManifest(manifest);
                    bundleLocation = fileObject;
                } catch (IOException e) {
                    LOGGER.error("Unrecognized file type", e);
                    return;
                } finally {
                    if (jarFile != null) {
                        try {
                            jarFile.close();
                        } catch (IOException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }
                }
                
            }

            Bundle installedBundle = deployBundle(bundleData, bundleLocation);
            if (installedBundle != null) {
                installedBundles.add(installedBundle);
            }
        }

        LOGGER.info("Calling refresh on OSGi framework. All packages on uninstalled bundles should be re-wired");
        frameworkWiring.refreshBundles(null, new FrameworkListener[] { frameworkListener });
        while (!refreshFinished.get()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                refreshFinished.set(true);
                Thread.currentThread().interrupt();
            }
        }
        for (Bundle bundle : installedBundles) {
            try {
                LOGGER.info("Starting bundle " + bundle.toString());
                bundle.start();
            } catch (BundleException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    @Override
    public void close() throws IOException {
        tracker.close();
    }
}
