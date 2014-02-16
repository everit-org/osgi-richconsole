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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

import org.everit.osgi.dev.richconsole.RichConsoleConstants;
import org.everit.osgi.dev.richconsole.internal.Logger;

public class TCPServer implements AutoCloseable {

    private class ServerThread extends Thread {
        @Override
        public void run() {
            while (!stopped.get()) {
                Socket socket = null;
                try {
                    socket = serverSocket.accept();
                } catch (IOException e) {
                    if (!stopped.get()) {
                        Logger.error("Error in deployment server, stopping", e);
                        stopServer();
                    }
                }
                if (socket != null) {
                    handleIncomingSocket(socket);
                }
            }
        }
    }

    private UpgradeProcess ongoingProcess = null;

    private ServerSocket serverSocket = null;

    private AtomicBoolean stopped = new AtomicBoolean(false);

    private ReentrantLock stoppingLock = new ReentrantLock(true);

    private final UpgradeServiceImpl upgradeService;

    public TCPServer(final UpgradeServiceImpl bundleDeployerService, final int port) {
        this.upgradeService = bundleDeployerService;

        InetAddress localAddress;
        try {
            localAddress = InetAddress.getLocalHost();

        } catch (UnknownHostException e) {
            Logger.error("Deployment server could not be started. Address of localhost is not available", e);
            return;
        }
        try {
            serverSocket = new ServerSocket();
            serverSocket.setReuseAddress(true);
            SocketAddress socketAddress = new InetSocketAddress(localAddress, port);
            serverSocket.bind(socketAddress);

            ServerThread serverThread = new ServerThread();
            serverThread.start();
        } catch (IOException e) {
            Logger.error("Deployment server could not have been binded to address " + localAddress.toString()
                    + " on port " + port, null);
            return;
        }

    }

    @Override
    public void close() throws Exception {
        stoppingLock.lock();
        stopped.set(true);
        if (ongoingProcess == null) {
            stopServer();
        }
        stoppingLock.unlock();
    }

    public int getLocalPort() {
        return serverSocket.getLocalPort();
    }

    private void handleCommand(final String line, final OutputStream outputStream) throws IOException {
        if (line.startsWith(RichConsoleConstants.TCPCOMMAND_DEPLOY_BUNDLE)) {
            handleDeployCommand(line);
            outputStream.write((RichConsoleConstants.TCPRESPONSE_OK + "\n").getBytes(Charset.defaultCharset()));
            outputStream.flush();
        } else if (line.startsWith(RichConsoleConstants.TCPCOMMAND_UNINSTALL)) {
            handleUninstallCommand(line);
            outputStream.write((RichConsoleConstants.TCPRESPONSE_OK + "\n").getBytes(Charset.defaultCharset()));
            outputStream.flush();
        } else if (line.startsWith(RichConsoleConstants.TCPCOMMAND_GET_ENVIRONMENT_ID)) {
            String environmentId = System.getProperty(RichConsoleConstants.SYSPROP_ENVIRONMENT_ID);
            if (environmentId != null) {
                outputStream.write((environmentId + "\n").getBytes(Charset.defaultCharset()));
            } else {
                outputStream.write("\n".getBytes(Charset.defaultCharset()));
            }
        }
    }

    private void handleCommandsFromSocket(final Socket socket) throws IOException {
        InputStream inputStream = socket.getInputStream();
        OutputStream outputStream = socket.getOutputStream();
        InputStreamReader reader = new InputStreamReader(inputStream, Charset.defaultCharset());
        BufferedReader br = new BufferedReader(reader);
        try {
            String line = br.readLine();
            while (line != null) {
                handleCommand(line, outputStream);
                line = br.readLine();
            }
        } catch (IOException e) {
            Logger.error("Error during reading from Upgrade Socket", e);
        }
    }

    private void handleDeployCommand(String command) {
        command = command.substring(RichConsoleConstants.TCPCOMMAND_DEPLOY_BUNDLE.length() + 1);
        String[] commandParts = command.split("\\@");
        String bundleLocationString = commandParts[0];
        Integer startLevel = null;
        boolean start = false;
        if (commandParts.length > 1) {
            String[] startParts = commandParts[1].split("\\:");
            if (startParts.length == 1) {
                if (startParts[0].equals("start")) {
                    start = true;
                } else {
                    try {
                        startLevel = Integer.valueOf(startParts[0]);
                    } catch (NumberFormatException e) {
                        Logger.error("(Skipping) Invalid install command syntax: " + command, null);
                        return;
                    }
                }
            } else {
                try {
                    startLevel = Integer.valueOf(startParts[0]);
                } catch (NumberFormatException e) {
                    Logger.error("(Skipping) Invalid install command syntax: " + command, null);
                    return;
                }
                if (!"start".equals(startParts[1])) {
                    Logger.error("(Skipping) Invalid install command syntax: " + command, null);
                    return;
                }
                start = true;
            }
        }

        try {
            URI bundleLocation = new URI(bundleLocationString);
            ongoingProcess.deployBundle(bundleLocation, start, startLevel);
        } catch (URISyntaxException e) {
            Logger.error("(Skipping) Could not process bundle install command: " + command, e);
        }
    }

    private synchronized void handleIncomingSocket(final Socket socket) {
        stoppingLock.lock();
        if (!stopped.get()) {
            ongoingProcess = upgradeService.newUpgradeProcess();
        } else {
            stoppingLock.unlock();
            return;
        }
        stoppingLock.unlock();

        try {
            handleCommandsFromSocket(socket);
        } catch (IOException e) {
            Logger.error("Error during receiving deployment commands", e);
        } finally {
            stoppingLock.lock();
            try {
                socket.close();
            } catch (IOException e) {
                Logger.error("Error during closing incoming upgrade socket", e);
            }
            final UpgradeProcess finishingProcess = ongoingProcess;
            ongoingProcess = null;
            new Thread(new Runnable() {

                @Override
                public void run() {
                    stoppingLock.lock();
                    try {
                        finishingProcess.finish();
                    } finally {
                        stoppingLock.unlock();
                    }
                }
            }).start();
            if (stopped.get()) {
                stopServer();
            }
            stoppingLock.unlock();
        }
    }

    private void handleUninstallCommand(String command) {
        command = command.substring(RichConsoleConstants.TCPCOMMAND_UNINSTALL.length() + 1);
        String[] commandParts = command.split(":");
        String symbolicName = commandParts[0];
        String version = null;
        if (commandParts.length > 1) {
            version = commandParts[1];
        }
        ongoingProcess.uninstallBundle(symbolicName, version);
    }

    private synchronized void stopServer() {
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException e) {
            Logger.error("Cannot stop deployment server", e);
        }
        serverSocket = null;
    }
}
