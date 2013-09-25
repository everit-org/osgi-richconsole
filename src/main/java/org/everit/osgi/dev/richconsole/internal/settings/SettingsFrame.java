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
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import org.everit.osgi.dev.richconsole.ConfigStore;
import org.everit.osgi.dev.richconsole.internal.ConfigStoreImpl;

public class SettingsFrame extends JFrame {

    public static final String DEPLOYER_WINDOW_LABEL = "DEPLOYER_WINDOW_LABEL";
    private JPanel contentPane;
    private JTextField textField;
    private final ConfigStore configStore;

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
        this.configStore = configStore;
        setTitle("Settings");
        setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        setBounds(100, 100, 450, 300);
        contentPane = new JPanel();
        contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
        setContentPane(contentPane);
        contentPane.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
        
        JLabel lblLabel = new JLabel("Label");
        contentPane.add(lblLabel);
        
        textField = new JTextField();
        contentPane.add(textField);
        textField.setColumns(6);
        
        JButton btnStore = new JButton("Apply");
        btnStore.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                configStore.setProperty(DEPLOYER_WINDOW_LABEL, textField.getText());
            }
        });
        contentPane.add(btnStore);
    }

}
