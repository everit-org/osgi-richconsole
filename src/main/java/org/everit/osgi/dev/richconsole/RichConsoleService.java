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
import javax.swing.JMenuItem;

/**
 * A service that richconsole registers after starting up. Via the service it is possible to extend the functionality of
 * richconsole.
 */
public interface RichConsoleService {

    void addMenuItemToContextMenu(JMenuItem menuItem);
    
    void removeMenuItemFromContextMenu(JMenuItem menuItem);
    
    ConfigStore getConfigStore();

}
