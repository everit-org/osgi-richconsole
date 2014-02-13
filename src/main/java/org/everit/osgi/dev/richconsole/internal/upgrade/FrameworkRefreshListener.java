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

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

import org.everit.osgi.dev.richconsole.internal.BundleUtil;
import org.everit.osgi.dev.richconsole.internal.Logger;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;

class FrameworkRefreshListener implements FrameworkListener {

    private final Condition refreshFinishCondition;

    private final AtomicBoolean refreshFinished;

    private final Lock refreshFinishLock;

    public FrameworkRefreshListener(final AtomicBoolean refreshFinished, final Lock refreshFinishLock,
            final Condition refreshFinishCondition) {
        this.refreshFinished = refreshFinished;
        this.refreshFinishLock = refreshFinishLock;
        this.refreshFinishCondition = refreshFinishCondition;
    }

    @Override
    public void frameworkEvent(final FrameworkEvent event) {
        int eventType = event.getType();
        if ((eventType == FrameworkEvent.ERROR) || (eventType == FrameworkEvent.PACKAGES_REFRESHED)) {
            refreshFinishLock.lock();
            try {
                refreshFinished.set(true);
                Logger.info("Framework refresh finished with code "
                        + BundleUtil.convertFrameworkEventTypeCode(eventType));
                refreshFinishCondition.signal();
            } finally {
                refreshFinishLock.unlock();
            }
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
                Logger.error(sb.toString(), event.getThrowable());
            } else {
                Logger.info(sb.toString());
            }
        }
    }

    public Condition getRefreshFinishCondition() {
        return refreshFinishCondition;
    }

    public AtomicBoolean getRefreshFinished() {
        return refreshFinished;
    }

    public Lock getRefreshFinishLock() {
        return refreshFinishLock;
    }
}
