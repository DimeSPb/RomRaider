/*
 * RomRaider Open-Source Tuning, Logging and Reflashing
 * Copyright (C) 2006-2012 RomRaider.com
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.romraider.ramtune.test.command.executor;

import com.romraider.Settings;
import com.romraider.io.connection.ConnectionManager;
import static com.romraider.io.connection.ConnectionManagerFactory.getManager;
import com.romraider.io.connection.ConnectionProperties;
import com.romraider.util.SettingsManager;

import static com.romraider.util.ParamChecker.checkNotNull;
import static com.romraider.util.ParamChecker.checkNotNullOrEmpty;

public final class CommandExecutorImpl implements CommandExecutor {
    private final ConnectionManager connectionManager;

    public CommandExecutorImpl(ConnectionProperties connectionProperties, String port) {
        checkNotNull(connectionProperties);
        Settings settings = SettingsManager.getSettings();
        if (!settings.isCanBus()) {
            checkNotNullOrEmpty(port, "port");
        }
        this.connectionManager = getManager(port, connectionProperties);
    }

    public byte[] executeCommand(byte[] command) {
        return connectionManager.send(command);
    }

    public void close() {
        connectionManager.close();
    }
}
