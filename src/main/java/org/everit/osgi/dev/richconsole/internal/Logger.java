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

public class Logger {

    public static void error(final String message, final Throwable e) {
        System.err.println("Richconsole ERROR: " + message);
        if (e != null) {
            e.printStackTrace(System.err);
        }
        System.err.flush();
    }

    public static void info(final String message) {
        System.out.println("Richconsole INFO: " + message);
        System.out.flush();
    }

    public static void warn(final String message) {
        System.out.println("Richconsole WARN: " + message);
        System.out.flush();
    }
}