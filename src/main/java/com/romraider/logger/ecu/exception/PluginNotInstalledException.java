/*
 * RomRaider Open-Source Tuning, Logging and Reflashing
 * Copyright (C) 2006-2020 RomRaider.com
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

package com.romraider.logger.ecu.exception;

public final class PluginNotInstalledException extends RuntimeException {

    private static final long serialVersionUID = -523838685805525387L;

    public PluginNotInstalledException() {
    }

    public PluginNotInstalledException(String string) {
        super(string);
    }

    public PluginNotInstalledException(String string, Throwable throwable) {
        super(string, throwable);
    }

    public PluginNotInstalledException(Throwable throwable) {
        super(throwable);
    }
}
