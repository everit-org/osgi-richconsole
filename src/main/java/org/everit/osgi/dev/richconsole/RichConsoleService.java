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
