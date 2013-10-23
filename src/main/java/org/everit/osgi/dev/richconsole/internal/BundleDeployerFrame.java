package org.everit.osgi.dev.richconsole.internal;

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

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Image;
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
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.List;
import java.util.TooManyListenersException;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.everit.osgi.dev.richconsole.ConfigPropertyChangeListener;
import org.everit.osgi.dev.richconsole.ConfigStore;
import org.everit.osgi.dev.richconsole.internal.settings.SettingsFrame;
import org.osgi.framework.BundleContext;

public class BundleDeployerFrame implements MainFrame, Closeable {

    private static Point point = new Point();

    private BundleDeployerServiceImpl bundleServiceImpl;

    private ConfigStore configStore;

    private ExtensionTrackerImpl menuItemTracker;

    private JFrame smallFrame;

    public BundleDeployerFrame(final BundleDeployerServiceImpl bundleServiceImpl) {
        this.bundleServiceImpl = bundleServiceImpl;
    }

    @Override
    public void close() {
        menuItemTracker.stop();
        smallFrame.dispose();
    }

    @Override
    public ConfigStore getConfigStore() {
        return configStore;
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

        JPanel panel = new JPanel() {
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
        panel.add(jlabel);
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
                public void drop(final DropTargetDropEvent dtde) {
                    Logger.info("Drop event caught on OSGi deployer");
                    Transferable transferable = dtde.getTransferable();
                    DataFlavor[] transferDataFlavors = transferable.getTransferDataFlavors();
                    DataFlavor selectedDataFlavor = null;
                    for (int i = 0, n = transferDataFlavors.length; (i < n) && (selectedDataFlavor == null); i++) {
                        if (transferDataFlavors[i].isFlavorJavaFileListType()) {
                            selectedDataFlavor = transferDataFlavors[i];
                        }
                    }
                    dtde.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
                    if (selectedDataFlavor != null) {

                        try {
                            @SuppressWarnings("unchecked")
                            List<File> transferDataList = (List<File>) transferable.getTransferData(selectedDataFlavor);
                            Logger.info("Analyzing files if they can be deployed: " + transferDataList.toString());
                            bundleServiceImpl.deployBundles(transferDataList);
                        } catch (UnsupportedFlavorException e) {
                            Logger.error("Unsupported drop flavor on Deployer window", e);
                        } catch (IOException e) {
                            Logger.error("Error during dropping to deployer window", e);
                        }

                    } else {
                        Logger.warn("No supported format found in dropped content. "
                                + "Supported format is 'File List Type'");
                    }
                }

                @Override
                public void dropActionChanged(final DropTargetDragEvent dtde) {
                    System.out.println("dropActionChanged: " + dtde.toString());

                }
            });
        } catch (TooManyListenersException e1) {
            Logger.error("Too many listeners during dropping to deployer window", e1);
        }

        menuItemTracker = new ExtensionTrackerImpl(this, panel, context);
        menuItemTracker.start();
        smallFrame.setVisible(true);
    }
}
