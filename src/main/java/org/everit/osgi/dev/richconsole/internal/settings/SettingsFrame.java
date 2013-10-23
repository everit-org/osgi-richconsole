package org.everit.osgi.dev.richconsole.internal.settings;

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

import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;

import org.everit.osgi.dev.richconsole.ConfigPropertyChangeListener;
import org.everit.osgi.dev.richconsole.ConfigStore;
import org.everit.osgi.dev.richconsole.internal.ConfigStoreImpl;

public class SettingsFrame extends JFrame {

    public static final String DEPLOYER_WINDOW_LABEL = "DEPLOYER_WINDOW_LABEL";

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    private JPanel contentPane;

    private JTextField labelTextField;

    private JTextField settingsFilePathTextField;

    /**
     * Create the frame.
     */
    public SettingsFrame(final ConfigStore configStore) {
        setTitle("Settings");
        setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
        setBounds(100, 100, 499, 300);
        contentPane = new JPanel();
        contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
        setContentPane(contentPane);
        contentPane.setLayout(new GridLayout(0, 1, 0, 0));

        JTabbedPane tabbedPane = new JTabbedPane(SwingConstants.TOP);
        contentPane.add(tabbedPane);

        JPanel decorationPanel = new JPanel();
        FlowLayout fl_decorationPanel = (FlowLayout) decorationPanel.getLayout();
        fl_decorationPanel.setVgap(10);
        fl_decorationPanel.setHgap(10);
        fl_decorationPanel.setAlignment(FlowLayout.LEFT);
        tabbedPane.addTab("Decoration", null, decorationPanel, null);

        JLabel lblLabel = new JLabel("Deployer window label");
        decorationPanel.add(lblLabel);

        labelTextField = new JTextField();
        decorationPanel.add(labelTextField);
        labelTextField.setColumns(6);
        configStore.addPropertyChangeListener(new ConfigPropertyChangeListener() {

            @Override
            public void propertyChanged(final String key, final String value) {
                if (key.equals(DEPLOYER_WINDOW_LABEL)) {
                    System.out.println("////// SETtING " + value);
                    labelTextField.setText(value);
                }
            }
        });

        JButton btnStore = new JButton("Apply");
        decorationPanel.add(btnStore);

        final JPanel storePanel = new JPanel();
        storePanel.setToolTipText("");
        tabbedPane.addTab("Store", null, storePanel, null);
        GridBagLayout gbl_storePanel = new GridBagLayout();
        gbl_storePanel.columnWidths = new int[] { 140, 0, 0 };
        gbl_storePanel.rowHeights = new int[] { 0, 0, 0, 0, 0 };
        gbl_storePanel.columnWeights = new double[] { 0.0, 1.0, Double.MIN_VALUE };
        gbl_storePanel.rowWeights = new double[] { 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE };
        storePanel.setLayout(gbl_storePanel);

        JLabel lblNewLabel = new JLabel("Settings file path");
        lblNewLabel.setHorizontalAlignment(SwingConstants.LEFT);
        GridBagConstraints gbc_lblNewLabel = new GridBagConstraints();
        gbc_lblNewLabel.anchor = GridBagConstraints.WEST;
        gbc_lblNewLabel.insets = new Insets(5, 5, 5, 5);
        gbc_lblNewLabel.gridx = 0;
        gbc_lblNewLabel.gridy = 1;
        storePanel.add(lblNewLabel, gbc_lblNewLabel);

        settingsFilePathTextField = new JTextField();
        settingsFilePathTextField.setEditable(false);
        settingsFilePathTextField.setText(configStore.getSettingsFilePath());

        GridBagConstraints gbc_settingsFilePathTextField = new GridBagConstraints();
        gbc_settingsFilePathTextField.insets = new Insets(0, 0, 5, 0);
        gbc_settingsFilePathTextField.fill = GridBagConstraints.HORIZONTAL;
        gbc_settingsFilePathTextField.gridx = 1;
        gbc_settingsFilePathTextField.gridy = 1;
        storePanel.add(settingsFilePathTextField, gbc_settingsFilePathTextField);
        settingsFilePathTextField.setColumns(18);

        JTextPane lblNewLabel_1 = new JTextPane();
        lblNewLabel_1.setText("You can change the path of the settings file by setting the \""
                + ConfigStore.SYSPROP_SETTINGS_FILE_PATH + "\" system property.");

        lblNewLabel_1.setFont(new Font("Dialog", Font.BOLD, 11));
        lblNewLabel_1.setBackground(null);
        lblNewLabel_1.setBorder(null);
        GridBagConstraints gbc_lblNewLabel_1 = new GridBagConstraints();
        gbc_lblNewLabel_1.insets = new Insets(0, 0, 5, 0);
        gbc_lblNewLabel_1.anchor = GridBagConstraints.NORTHWEST;
        gbc_lblNewLabel_1.gridx = 1;
        gbc_lblNewLabel_1.gridy = 2;
        storePanel.add(lblNewLabel_1, gbc_lblNewLabel_1);

        JPanel panel = new JPanel();
        GridBagConstraints gbc_panel = new GridBagConstraints();
        gbc_panel.fill = GridBagConstraints.BOTH;
        gbc_panel.gridx = 1;
        gbc_panel.gridy = 3;
        storePanel.add(panel, gbc_panel);

        JButton btnImport = new JButton("Import...");
        btnImport.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                JFileChooser jFileChooser = new JFileChooser();
                int fileDialogResult = jFileChooser.showOpenDialog(storePanel);
                if (fileDialogResult == JFileChooser.APPROVE_OPTION) {
                    int clearResult =
                            JOptionPane.showOptionDialog(storePanel,
                                    "Do you want to clear current configuration before importing?", "Question",
                                    JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, null, null);

                    File selectedFile = jFileChooser.getSelectedFile();

                    ConfigStoreImpl configStoreImpl = (ConfigStoreImpl) configStore;
                    configStoreImpl.importFromFile(selectedFile, clearResult == JOptionPane.YES_OPTION);
                }
            }
        });
        panel.add(btnImport);

        JButton btnExport = new JButton("Export...");
        btnExport.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                JFileChooser jFileChooser = new JFileChooser();
                int fileDialogResult = jFileChooser.showSaveDialog(storePanel);
                if (fileDialogResult == JFileChooser.APPROVE_OPTION) {
                    File selectedFile = jFileChooser.getSelectedFile();

                    ConfigStoreImpl configStoreImpl = (ConfigStoreImpl) configStore;
                    configStoreImpl.exportToFile(selectedFile);
                }
            }
        });
        panel.add(btnExport);
        btnStore.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                configStore.setProperty(DEPLOYER_WINDOW_LABEL, labelTextField.getText());
            }
        });
    }
    
    
}
