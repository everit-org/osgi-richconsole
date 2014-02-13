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
package org.everit.osgi.dev.richconsole.internal.upgrade;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TCPServer implements AutoCloseable {

    private class ServerThread extends Thread {
        @Override
        public void run() {
            while (!stopped.get()) {
                Socket socket = null;
                try {
                    socket = serverSocket.accept();
                } catch (IOException e) {
                    LOGGER.log(Level.SEVERE, "Error in deployment server, stopping", e);
                    stopServer();
                }
                if (socket != null) {
                    try {
                        InputStream inputStream = socket.getInputStream();

                    } catch (IOException e) {
                        LOGGER.log(Level.SEVERE, "Error during receiving deployment commands", e);
                    } finally {
                        try {
                            socket.close();
                        } catch (IOException e) {
                            LOGGER.log(Level.SEVERE, "Error during closing deployment server socket", e);
                        }
                    }
                }
            }
        }
    }

    private final UpgradeServiceImpl bundleDeployerService;

    private Logger LOGGER = Logger.getLogger(TCPServer.class.getName());

    private ServerSocket serverSocket = null;

    private AtomicBoolean stopped = new AtomicBoolean(true);

    public TCPServer(final UpgradeServiceImpl bundleDeployerService) {
        this.bundleDeployerService = bundleDeployerService;
    }

    @Override
    public void close() throws Exception {
        stopped.set(true);
    }

    public void start(final int port) {
        stopped.set(false);
        InetAddress localAddress;
        try {
            localAddress = InetAddress.getLocalHost();

        } catch (UnknownHostException e) {
            LOGGER.log(Level.SEVERE, "Deployment server could not be started. Address of localhost is not available", e);
            return;
        }
        try {
            serverSocket = new ServerSocket(port, 1, localAddress);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE,
                    "Deployment server could not have been binded to address " + localAddress.toString() + " on port "
                            + port);
            return;
        }
    }

    private void stopServer() {
        stopped.set(true);
        try {
            serverSocket.close();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Cannot stop deployment server", e);
        }
        serverSocket = null;
    }
}
