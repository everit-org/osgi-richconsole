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

import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Point;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.List;
import java.util.TooManyListenersException;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;

import org.everit.osgi.dev.richconsole.ConfigPropertyChangeListener;
import org.everit.osgi.dev.richconsole.ConfigStore;
import org.everit.osgi.dev.richconsole.RichConsoleConstants;
import org.everit.osgi.dev.richconsole.RichConsoleService;
import org.everit.osgi.dev.richconsole.internal.settings.SettingsFrame;
import org.everit.osgi.dev.richconsole.internal.upgrade.UpgradeServiceImpl;
import org.osgi.framework.BundleContext;

public class BundleDeployerFrame implements RichConsoleService {

    private static Point point = new Point();

    private final UpgradeServiceImpl bundleServiceImpl;

    private ConfigStore configStore;

    private final Runnable disposerAction;

    private final JPopupMenu jPopupMenu = new JPopupMenu();

    private JFrame smallFrame;

    private JLabel tcpPortLabel = null;

    public BundleDeployerFrame(final UpgradeServiceImpl bundleServiceImpl) {
        this.bundleServiceImpl = bundleServiceImpl;
        disposerAction = new Runnable() {

            @Override
            public void run() {
                smallFrame.dispose();
            }
        };
    }

    @Override
    public void addMenuItemToContextMenu(final JMenuItem menuItem) {
        jPopupMenu.add(menuItem);
    }

    public void close() {
        EventQueue.invokeLater(disposerAction);
    }

    @Override
    public ConfigStore getConfigStore() {
        return configStore;
    }

    @Override
    public void removeMenuItemFromContextMenu(final JMenuItem menuItem) {
        jPopupMenu.remove(menuItem);
    }

    void setTCPPort(final int localPort) {
        tcpPortLabel.setText(String.valueOf(localPort));
    }

    public void start(final BundleContext context) {
        URL imageResource = this.getClass().getResource("/images/everit_OSGi_deployer.png");
        configStore = new ConfigStoreImpl(context);

        Image backgroundImage = null;
        try {
            backgroundImage = ImageIO.read(imageResource);
        } catch (IOException e) {
            Logger.error("Could not read background image for deployer window", e);
        }
        final Image finalImage = backgroundImage;
        smallFrame = new JFrame("OSGi Bundle Deployer");
        smallFrame.setResizable(false);
        smallFrame.setAlwaysOnTop(true);
        smallFrame.setUndecorated(true);
        final int panelWidth = 200;
        final int panelHeight = 56;

        smallFrame.setSize(panelWidth, panelHeight);

        JPanel panel = new JPanel(new GridBagLayout(), true) {
            /**
             *
             */
            private static final long serialVersionUID = 1L;

            @Override
            public void paintComponent(final Graphics g) {
                g.drawImage(finalImage, 1, 1, panelWidth - 2, panelHeight - 2, 0, 0, finalImage.getWidth(smallFrame),
                        finalImage.getHeight(smallFrame), smallFrame);
                g.setColor(Color.BLACK);
                g.drawRect(0, 0, panelWidth - 1, panelHeight - 1);
            }
        };
        panel.setSize(panelWidth, panelHeight);
        smallFrame.add(panel);

        final JLabel jlabel = new JLabel();
        GridBagConstraints jLabelC = new GridBagConstraints();
        jLabelC.gridx = 0;
        jLabelC.gridy = 0;
        jLabelC.insets = new Insets(-2, 40, 0, 10);
        panel.add(jlabel, jLabelC);
        String label = configStore.getProperty(SettingsFrame.DEPLOYER_WINDOW_LABEL);
        if (label != null) {
            jlabel.setText(label);
        }
        configStore.addPropertyChangeListener(new ConfigPropertyChangeListener() {

            @Override
            public void propertyChanged(final String key, final String value) {
                if (SettingsFrame.DEPLOYER_WINDOW_LABEL.equals(key)) {
                    jlabel.setText(value);
                }
            }
        });

        tcpPortLabel = new JLabel();
        GridBagConstraints tcpPortLabelC = new GridBagConstraints();
        tcpPortLabelC.gridx = 0;
        tcpPortLabelC.gridy = 1;
        tcpPortLabelC.insets = new Insets(14, 40, 0, 10);
        panel.add(tcpPortLabel, tcpPortLabelC);

        String javaSpecVersion = System.getProperty("java.vm.specification.version");

        if ("1.6".compareTo(javaSpecVersion) < 0) {
            try {
                Method method = smallFrame.getClass().getMethod("setOpacity", float.class);
                method.invoke(smallFrame, (float) 0.9);
            } catch (Exception e1) {
                Logger.info("Opacity for frames are not supported. OSGi deployer window"
                        + " will appear with no transparency option.");
            }
        }

        //
        // The mouse listener and mouse motion listener we add here is to simply
        // make our frame dragable.
        //
        panel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(final MouseEvent e) {
                jPopupMenu.show(e.getComponent(), e.getX(), e.getY());
            }

            @Override
            public void mousePressed(final MouseEvent e) {
                Point location = smallFrame.getLocation();
                point.x = e.getXOnScreen() - (int) location.getX();
                point.y = e.getYOnScreen() - (int) location.getY();
            }
        });

        panel.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(final MouseEvent e) {
                smallFrame.setLocation(e.getXOnScreen() - point.x, e.getYOnScreen() - point.y);
            }
        });

        DropTarget dropTarget = new DropTarget();
        panel.setDropTarget(dropTarget);
        try {
            dropTarget.addDropTargetListener(new DropTargetAdapter() {

                @Override
                public void dragEnter(DropTargetDragEvent dtde) {
                    if (selectDataFlavor(dtde.getTransferable().getTransferDataFlavors()) == null) {
                        dtde.rejectDrag();
                    } else {
                        dtde.acceptDrag(DnDConstants.ACTION_COPY_OR_MOVE);
                    }
                }

                @Override
                public void drop(final DropTargetDropEvent dtde) {
                    Logger.info("Drop event caught on OSGi deployer");
                    Transferable transferable = dtde.getTransferable();
                    DataFlavor selectedDataFlavor = selectDataFlavor(transferable.getTransferDataFlavors());

                    if (selectedDataFlavor != null) {
                        dtde.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
                        try {
                            @SuppressWarnings("unchecked")
                            final List<File> transferDataList =
                                    (List<File>) transferable.getTransferData(selectedDataFlavor);
                            dtde.dropComplete(true);
                            Logger.info("Analyzing files if they can be deployed: " + transferDataList.toString());
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    bundleServiceImpl.deployBundles(transferDataList);
                                };
                            }).start();
                        } catch (UnsupportedFlavorException e) {
                            Logger.error("Unsupported drop flavor on Deployer window", e);
                        } catch (IOException e) {
                            Logger.error("Error during dropping to deployer window", e);
                        }

                    } else {
                        Logger.warn("Not supported format found in dropped content. "
                                + "Supported format is 'File List Type'.");
                        dtde.dropComplete(false);
                    }
                }

                @Override
                public void dropActionChanged(final DropTargetDragEvent dtde) {

                }

                private DataFlavor selectDataFlavor(DataFlavor[] dataFlavors) {
                    DataFlavor selectedDataFlavor = null;
                    for (int i = 0, n = dataFlavors.length; (i < n) && (selectedDataFlavor == null); i++) {
                        if (dataFlavors[i].isFlavorJavaFileListType()) {
                            selectedDataFlavor = dataFlavors[i];
                        }
                    }
                    return selectedDataFlavor;
                }
            });
        } catch (TooManyListenersException e1) {
            Logger.error("Too many listeners during dropping to deployer window", e1);
        }

        String deployerWindowLabel = configStore.getProperty(SettingsFrame.DEPLOYER_WINDOW_LABEL);
        if (deployerWindowLabel == null) {
            String environmentIdSysProp = System.getProperty(RichConsoleConstants.SYSPROP_ENVIRONMENT_ID);
            if (environmentIdSysProp != null) {
                configStore.setProperty(SettingsFrame.DEPLOYER_WINDOW_LABEL, environmentIdSysProp);
            }
        }
        smallFrame.setVisible(true);
    }
}
