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

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.util.tracker.BundleTracker;

public class UpgradeServiceImpl implements Closeable {

    private class Tracker extends BundleTracker<Bundle> {

        private Map<String, List<Long>> bundleIdsBySymbolicName = new ConcurrentHashMap<String, List<Long>>();

        public Tracker(final BundleContext context, final int stateMask) {
            super(context, stateMask, null);
        }

        @Override
        public Bundle addingBundle(final Bundle bundle, final BundleEvent event) {
            String symbolicName = bundle.getSymbolicName();
            List<Long> bundleIdList = bundleIdsBySymbolicName.get(symbolicName);
            if (bundleIdList == null) {
                bundleIdList = new ArrayList<Long>();
                bundleIdsBySymbolicName.put(symbolicName, bundleIdList);
            }
            bundleIdList.add(bundle.getBundleId());
            return super.addingBundle(bundle, event);
        }

        public List<Long> getBundleIdsBySymbolicName(final String symbolicName) {
            return bundleIdsBySymbolicName.get(symbolicName);
        }

        @Override
        public void removedBundle(final Bundle bundle, final BundleEvent event, final Bundle object) {
            super.remove(bundle);
            String symbolicName = bundle.getSymbolicName();
            List<Long> list = bundleIdsBySymbolicName.get(symbolicName);
            list.remove(bundle.getBundleId());
            if (list.size() == 0) {
                bundleIdsBySymbolicName.remove(symbolicName);
            }
        }
    }

    private boolean closed = false;

    private UpgradeProcess ongoingProcess = null;

    private final BundleContext systemBundleContext;

    private final Tracker tracker;

    public UpgradeServiceImpl(final Bundle consoleBundle) {
        systemBundleContext = consoleBundle.getBundleContext().getBundle(0).getBundleContext();
        tracker =
                new Tracker(consoleBundle.getBundleContext(), Bundle.ACTIVE | Bundle.INSTALLED | Bundle.RESOLVED
                        | Bundle.STARTING | Bundle.STOPPING);
        tracker.open();

    }

    @Override
    public synchronized void close() throws IOException {
        closed = true;
        if (ongoingProcess == null) {
            tracker.close();
        }
    }

    public synchronized void deployBundles(final List<File> fileObjects) {
        UpgradeProcess deploymentProcess = newUpgradeProcess();
        try {
            for (File file : fileObjects) {
                deploymentProcess.installBundle(file, true, null);
            }
        } finally {
            deploymentProcess.finish();
        }
    }

    synchronized void finishOngoingProcess() {
        ongoingProcess = null;
        if (closed) {
            tracker.close();
        }
    }

    Bundle getExistingBundleBySymbolicName(final String symbolicName, final String version,
            final String bundleLocation) {
        List<Long> existingBundleIds = tracker.getBundleIdsBySymbolicName(symbolicName);
        Bundle selectedBundle = null;
        if (existingBundleIds != null) {
            if (existingBundleIds.size() == 1) {
                selectedBundle = systemBundleContext.getBundle(existingBundleIds.get(0));
            }
            Iterator<Long> iterator = existingBundleIds.iterator();
            while (iterator.hasNext() && (selectedBundle == null)) {
                Long existingBundleId = iterator.next();
                Bundle bundle = systemBundleContext.getBundle(existingBundleId);
                String existingBundleLocation = bundle.getLocation();

                if (existingBundleLocation.equals(bundleLocation)) {
                    selectedBundle = bundle;
                }

                if (selectedBundle == null) {
                    String existingBundleVersion = bundle.getVersion().toString();
                    if (existingBundleVersion.equals(version)) {
                        selectedBundle = bundle;
                    }
                }
            }
            if ((selectedBundle == null) && (existingBundleIds.size() > 0)) {
                selectedBundle = systemBundleContext.getBundle(existingBundleIds.get(0));
            }
        }
        return selectedBundle;
    }

    public synchronized UpgradeProcess newUpgradeProcess() {
        if (ongoingProcess != null) {
            throw new IllegalStateException("There is already an ongoing deployment process");
        }
        if (closed) {
            throw new IllegalStateException("The deployer service is already closed");
        }

        return new UpgradeProcess(this, systemBundleContext);
    }
}
