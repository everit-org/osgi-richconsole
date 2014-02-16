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

import java.io.File;

public class BundleData {

    private String symbolicName;

    private String version;

    private File evaluatedLocationFile;

    public BundleData() {
    }

    public BundleData(final File locationFile, final String symbolicName, final String version) {
        this.symbolicName = symbolicName;
        this.version = version;
        this.evaluatedLocationFile = locationFile;
    }

    public String getSymbolicName() {
        return symbolicName;
    }

    public String getVersion() {
        return version;
    }

    public File getEvaluatedLocationFile() {
        return evaluatedLocationFile;
    }
}
