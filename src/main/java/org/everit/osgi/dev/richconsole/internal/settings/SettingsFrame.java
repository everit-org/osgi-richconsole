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

import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import org.everit.osgi.dev.richconsole.ConfigStore;
import org.everit.osgi.dev.richconsole.internal.ConfigStoreImpl;

import javax.swing.JInternalFrame;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.BoxLayout;

import java.awt.BorderLayout;
import java.awt.GridLayout;

import javax.swing.border.BevelBorder;
import javax.swing.JTabbedPane;

import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;

import javax.swing.SwingConstants;
import javax.swing.JTextPane;

import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.RowSpec;
import com.jgoodies.forms.factories.FormFactory;

import java.awt.Font;

public class SettingsFrame extends JFrame {

    public static final String DEPLOYER_WINDOW_LABEL = "DEPLOYER_WINDOW_LABEL";
    private JPanel contentPane;
    private JTextField labelTextField;
    private JTextField textField;

    /**
     * Launch the application.
     */
    public static void main(String[] args) {
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    SettingsFrame frame = new SettingsFrame(new ConfigStoreImpl(null));
                    frame.setVisible(true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * Create the frame.
     */
    public SettingsFrame(final ConfigStore configStore) {
        setTitle("Settings");
        setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        setBounds(100, 100, 499, 300);
        contentPane = new JPanel();
        contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
        setContentPane(contentPane);
        contentPane.setLayout(new GridLayout(0, 1, 0, 0));

        JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.TOP);
        contentPane.add(tabbedPane);

        JPanel panel_2 = new JPanel();
        tabbedPane.addTab("Decoration", null, panel_2, null);

        JLabel lblLabel = new JLabel("Label");
        panel_2.add(lblLabel);

        labelTextField = new JTextField();
        panel_2.add(labelTextField);
        labelTextField.setColumns(6);

        JButton btnStore = new JButton("Apply");
        panel_2.add(btnStore);

        final JPanel panel_1 = new JPanel();
        panel_1.setToolTipText("");
        tabbedPane.addTab("Store", null, panel_1, null);
        GridBagLayout gbl_panel_1 = new GridBagLayout();
        gbl_panel_1.columnWidths = new int[] { 140, 0, 0 };
        gbl_panel_1.rowHeights = new int[] { 0, 0, 0, 0, 0 };
        gbl_panel_1.columnWeights = new double[] { 0.0, 1.0, Double.MIN_VALUE };
        gbl_panel_1.rowWeights = new double[] { 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE };
        panel_1.setLayout(gbl_panel_1);

        JLabel lblNewLabel = new JLabel("Settings file path");
        lblNewLabel.setHorizontalAlignment(SwingConstants.LEFT);
        GridBagConstraints gbc_lblNewLabel = new GridBagConstraints();
        gbc_lblNewLabel.anchor = GridBagConstraints.WEST;
        gbc_lblNewLabel.insets = new Insets(0, 0, 5, 5);
        gbc_lblNewLabel.gridx = 0;
        gbc_lblNewLabel.gridy = 1;
        panel_1.add(lblNewLabel, gbc_lblNewLabel);

        textField = new JTextField();
        textField.setEditable(false);
        textField
                .setToolTipText("You can change this value by setting the org.everit.osgi.dev.richconsole.SettingsFile system property");
        GridBagConstraints gbc_textField = new GridBagConstraints();
        gbc_textField.insets = new Insets(0, 0, 5, 0);
        gbc_textField.fill = GridBagConstraints.HORIZONTAL;
        gbc_textField.gridx = 1;
        gbc_textField.gridy = 1;
        panel_1.add(textField, gbc_textField);
        textField.setColumns(18);

        JLabel lblNewLabel_1 =
                new JLabel(
                        "<html>You can change the path of the settings file by setting the \"org.everit.osgi.dev.richconsole.SettingsFile\" system property.</html>");
        lblNewLabel_1.setFont(new Font("Dialog", Font.BOLD, 11));
        lblNewLabel_1.setHorizontalAlignment(SwingConstants.LEFT);
        GridBagConstraints gbc_lblNewLabel_1 = new GridBagConstraints();
        gbc_lblNewLabel_1.insets = new Insets(0, 0, 5, 0);
        gbc_lblNewLabel_1.anchor = GridBagConstraints.NORTHWEST;
        gbc_lblNewLabel_1.gridx = 1;
        gbc_lblNewLabel_1.gridy = 2;
        panel_1.add(lblNewLabel_1, gbc_lblNewLabel_1);

        JPanel panel = new JPanel();
        GridBagConstraints gbc_panel = new GridBagConstraints();
        gbc_panel.fill = GridBagConstraints.BOTH;
        gbc_panel.gridx = 1;
        gbc_panel.gridy = 3;
        panel_1.add(panel, gbc_panel);

        JButton btnImport = new JButton("Import...");
        btnImport.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JFileChooser jFileChooser = new JFileChooser();
                int fileDialogResult = jFileChooser.showOpenDialog(panel_1);
                if (fileDialogResult == JFileChooser.APPROVE_OPTION) {
                    int clearResult = JOptionPane.showOptionDialog(panel_1,
                            "Do you want to clear current configuration before importing?", "Question",
                            JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, null, null);

                    if (clearResult == JOptionPane.YES_OPTION) {
                        
                    }
                }

            }
        });
        panel.add(btnImport);

        JButton btnExport = new JButton("Export...");
        panel.add(btnExport);
        btnStore.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                configStore.setProperty(DEPLOYER_WINDOW_LABEL, labelTextField.getText());
            }
        });
    }

}
