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

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.List;
import java.util.TooManyListenersException;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BundleDeployerFrame implements Closeable {

    private BundleServiceImpl bundleServiceImpl;

    private JFrame smallFrame;

    private static final Logger LOGGER = LoggerFactory.getLogger(BundleDeployerFrame.class);

    private static Point point = new Point();

    public BundleDeployerFrame(BundleServiceImpl bundleServiceImpl) {
        super();
        this.bundleServiceImpl = bundleServiceImpl;
    }

    public void start() {
        URL imageResource = this.getClass().getResource("/images/everit_OSGi_deployer.png");
        Image backgroundImage = null;
        try {
            backgroundImage = ImageIO.read(imageResource);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
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
            @Override
            public void paintComponent(Graphics g) {
                g.drawImage(finalImage, 1, 1, panelWidth - 2, panelHeight - 2, 0, 0,
                        finalImage.getWidth(smallFrame), finalImage.getHeight(smallFrame), smallFrame);
                g.setColor(Color.BLACK);
                g.drawRect(0, 0, panelWidth - 1, panelHeight - 1);
            } 
        };
        panel.setSize(panelWidth, panelHeight);
        smallFrame.add(panel);

        String javaSpecVersion = System.getProperty("java.vm.specification.version");

        if ("1.6".compareTo(javaSpecVersion) < 0) {
            try {
                Method method = smallFrame.getClass().getMethod("setOpacity", float.class);
                method.invoke(smallFrame, (float) 0.9);
            } catch (SecurityException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            } catch (NoSuchMethodException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            } catch (IllegalArgumentException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            } catch (IllegalAccessException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            } catch (InvocationTargetException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
        }

        //
        // The mouse listener and mouse motion listener we add here is to simply
        // make our frame dragable.
        //
        panel.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                point.x = e.getX();
                point.y = e.getY();
            }
        });

        panel.addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseDragged(MouseEvent e) {
                Point p = smallFrame.getLocation();
                smallFrame.setLocation(p.x + e.getX() - point.x,
                        p.y + e.getY() - point.y);
            }
        });

        DropTarget dropTarget = new DropTarget();
        panel.setDropTarget(dropTarget);
        try {
            dropTarget.addDropTargetListener(new DropTargetListener() {

                public void dropActionChanged(DropTargetDragEvent dtde) {
                    System.out.println("dropActionChanged: " + dtde.toString());

                }

                public void drop(DropTargetDropEvent dtde) {
                    LOGGER.info("Drop event caught on OSGi deployer");
                    Transferable transferable = dtde.getTransferable();
                    DataFlavor[] transferDataFlavors = transferable.getTransferDataFlavors();
                    DataFlavor selectedDataFlavor = null;
                    for (int i = 0, n = transferDataFlavors.length; i < n && selectedDataFlavor == null; i++) {
                        if (transferDataFlavors[i].isFlavorJavaFileListType()) {
                            selectedDataFlavor = transferDataFlavors[i];
                        }
                    }
                    dtde.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
                    if (selectedDataFlavor != null) {

                        try {
                            @SuppressWarnings("unchecked")
                            List<File> transferDataList = (List<File>) transferable.getTransferData(selectedDataFlavor);
                            LOGGER.info("Analyzing files if they can be deployed: " + transferDataList.toString());
                            bundleServiceImpl.deployBundles(transferDataList);
                        } catch (UnsupportedFlavorException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        } catch (IOException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }

                    } else {
                        LOGGER.warn("No supported format found in dropped content. " +
                        		"Supported format is 'File List Type'");
                    }
                }

                public void dragOver(DropTargetDragEvent dtde) {
                }

                public void dragExit(DropTargetEvent dte) {
                }

                public void dragEnter(DropTargetDragEvent dtde) {
                }
            });
        } catch (TooManyListenersException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        smallFrame.setVisible(true);
    }

    @Override
    public void close() {
        smallFrame.dispose();
    }
}
