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
package org.everit.osgi.dev.richconsole;

public final class RichConsoleConstants {

    /**
     * In case the "EOSGI_STOP_AFTER_TESTS" environment variable has the value "true", the OSGi container is started as
     * part of a compilation process. The deployer window is not shown if this variable is set.
     */
    public static final String ENV_EOSGI_STOP_AFTER_TESTS = "EOSGI_STOP_AFTER_TESTS";

    /**
     * With the "EOSGI_UPGRADE_SERVICE_PORT", it is possible to specify the port where TCP Server listen on.
     */
    public static final String ENV_EOSGI_UPGRADE_SERVICE_PORT = "EOSGI_UPGRADE_SERVICE_PORT";

    /**
     * System property that tells the id of the OSGi environment. The id is useful if more than one OSGi container is
     * started.
     */
    public static final String SYSPROP_ENVIRONMENT_ID = "eosgi.environment.id";

    /**
     * Command of deploying a bundle. The command has one parameter that is the location of the bundle with the
     * following syntax: [reference:]file:fileURI[@[2:][start]] <br>
     * The expression is similar to the one that Equinox accepts via the osgi.bundles system property. The reference,
     * start level and the start flag is optional. After the command is processed, the server answers an "ok".
     */
    public static final String TCPCOMMAND_DEPLOY_BUNDLE = "deployBundle";

    /**
     * The TCP server answers the value of the {@link #SYSPROP_ENVIRONMENT_ID} system property.
     */
    public static final String TCPCOMMAND_GET_ENVIRONMENT_ID = "getEnvironmentId";

    /**
     * Uninstalls a bundle. The parameter of the command is the symbolic name and optionally the version of the bundle
     * separated with double point. E.g.: "uninstallBundle com.myBundle:2.0.0"
     */
    public static final String TCPCOMMAND_UNINSTALL = "uninstallBundle";

    /**
     * The answer of several TCP commands.
     */
    public static final String TCPRESPONSE_OK = "ok";

    private RichConsoleConstants() {
    }
}
