/*
 * RomRaider Open-Source Tuning, Logging and Reflashing
 * Copyright (C) 2006-2022 RomRaider.com
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

package com.romraider.logger.ecu.comms.io.connection;

import static com.romraider.util.HexUtil.asHex;
import static com.romraider.util.ParamChecker.checkNotNull;
import static org.apache.log4j.Logger.getLogger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.romraider.io.protocol.Protocol;
import com.romraider.io.protocol.ssm.iso9141.SSMProtocol;
import com.romraider.logger.ecu.comms.query.dimemod.DmInit;
import com.romraider.logger.ecu.comms.query.dimemod.DmInitCallback;
import org.apache.log4j.Logger;

import com.romraider.Settings;
import com.romraider.io.connection.ConnectionManager;
import com.romraider.io.protocol.ProtocolFactory;
import com.romraider.logger.ecu.comms.io.protocol.LoggerProtocol;
import com.romraider.logger.ecu.comms.manager.PollingState;
import com.romraider.logger.ecu.comms.manager.PollingStateImpl;
import com.romraider.logger.ecu.comms.query.EcuInitCallback;
import com.romraider.logger.ecu.comms.query.EcuQuery;
import com.romraider.logger.ecu.definition.Module;
import com.romraider.util.SettingsManager;

public final class SSMLoggerConnection implements LoggerConnection {
    private static final Logger LOGGER = getLogger(SSMLoggerConnection.class);
    private final LoggerProtocol protocol;
    private final ConnectionManager manager;
    private List<EcuQuery> tcuQueries = new ArrayList<EcuQuery>();
    private final Collection<EcuQuery> tcuSubQuery = new ArrayList<EcuQuery>();
    Settings settings = SettingsManager.getSettings();

    public SSMLoggerConnection(ConnectionManager manager) {
        checkNotNull(manager, "manager");
        this.manager = manager;

        this.protocol = ProtocolFactory.getProtocol(
                settings.getLoggerProtocol(),
                settings.getTransportProtocol());
    }

    @Override
    public void open(Module module) {
    }

    @Override
    public void ecuReset(Module module, int resetCode) {
        byte[] request = protocol.constructEcuResetRequest(module, resetCode);
        if (LOGGER.isDebugEnabled())
            LOGGER.debug(module + " Reset Request  ---> " + asHex(request));
        byte[] response = manager.send(request);
        byte[] processedResponse = protocol.preprocessResponse(request, response, new PollingStateImpl());
        if (LOGGER.isDebugEnabled())
            LOGGER.debug(module + " Reset Response <--- " + asHex(processedResponse));
        protocol.processEcuResetResponse(processedResponse);
    }

    @Override
    public void ecuInit(EcuInitCallback callback, Module module) {
        byte[] request = protocol.constructEcuInitRequest(module);
        if (LOGGER.isDebugEnabled())
            LOGGER.debug(module + " Init Request  ---> " + asHex(request));
        byte[] response = manager.send(request);
        byte[] processedResponse = protocol.preprocessResponse(request, response, new PollingStateImpl());
        if (LOGGER.isDebugEnabled())
            LOGGER.debug(module + " Init Response <--- " + asHex(processedResponse));
        protocol.processEcuInitResponse(callback, processedResponse);
    }

    public void dmInit(DmInitCallback callback, Module module) {
        final Protocol ssmProtocol = new SSMProtocol();
        DmInit dmInit = callback.getDmInit();
        if (dmInit == null) {
            byte[] request = ssmProtocol.constructWriteAddressRequest(module, new byte[]{0x00, 0x00, 0x00}, (byte) 0xDE);
            if (LOGGER.isDebugEnabled())
                LOGGER.debug(module + " Init DM Request  ---> " + asHex(request));
            byte[] response = manager.send(request);
            byte[] processedResponse = protocol.preprocessResponse(request, response, new PollingStateImpl());
            if (LOGGER.isDebugEnabled())
                LOGGER.debug(module + " Init DM Response <--- " + asHex(processedResponse));
            byte responseType = processedResponse[4];
            if (responseType != SSMProtocol.WRITE_ADDRESS_RESPONSE) {
                // error
                return;
            }
            if (processedResponse[5] == (byte) 0xAD) {
                request = ssmProtocol.constructReadAddressRequest(module, new byte[][]{new byte[]{0x00, 0x00, 0x00}});
                response = manager.send(request);
                processedResponse = protocol.preprocessResponse(request, response, new PollingStateImpl());
                responseType = processedResponse[4];
                if (responseType != SSMProtocol.READ_ADDRESS_RESPONSE) {
                    return;
                }
                int length = processedResponse[5] & 0xFF;
                int startAddress = 0x00;
                response = manager.send(request);
                processedResponse = protocol.preprocessResponse(request, response, new PollingStateImpl());
                responseType = processedResponse[4];
                if (responseType != SSMProtocol.READ_ADDRESS_RESPONSE) {
                    return;
                }
                startAddress |= (processedResponse[5] & 0xFF) << 16;
                response = manager.send(request);
                processedResponse = protocol.preprocessResponse(request, response, new PollingStateImpl());
                responseType = processedResponse[4];
                if (responseType != SSMProtocol.READ_ADDRESS_RESPONSE) {
                    return;
                }
                startAddress |= (processedResponse[5] & 0xFF) << 8;
                response = manager.send(request);
                processedResponse = protocol.preprocessResponse(request, response, new PollingStateImpl());
                responseType = processedResponse[4];
                if (responseType != SSMProtocol.READ_ADDRESS_RESPONSE) {
                    return;
                }
                startAddress |= processedResponse[5] & 0xFF;

                request = ssmProtocol.constructWriteAddressRequest(module, new byte[]{0x00, 0x00, 0x00}, (byte) 0x00);
                response = manager.send(request);
                processedResponse = protocol.preprocessResponse(request, response, new PollingStateImpl());
                responseType = processedResponse[4];
                if (responseType != SSMProtocol.WRITE_ADDRESS_RESPONSE) {
                    // error
                    return;
                }

                final int MAX_RESPONSE_SIZE = 96;
                int remaining = length;
                byte[] dmInitBytes = new byte[length];
                int addr = startAddress;
                while (remaining > 0) {
                    request = ssmProtocol.constructReadMemoryRequest(module,
                            new byte[]{(byte) (addr >> 16), (byte) (addr >> 8), (byte) addr},
                            Math.min(remaining, MAX_RESPONSE_SIZE)
                    );
                    response = manager.send(request);
                    processedResponse = protocol.preprocessResponse(request, response, new PollingStateImpl());
                    int readBytes = processedResponse.length - 6;
                    System.arraycopy(processedResponse, 5, dmInitBytes, dmInitBytes.length - remaining, readBytes);
                    remaining -= readBytes;
                    addr += readBytes;
                }

                dmInit = new DmInit(dmInitBytes);
            }
        }

        // read runtime params
        if (dmInit != null) {
            int afAddr = dmInit.getActiveFeaturesAddress();
            int cerrAddr = dmInit.getCurrentErrorCodesAddress();
            int merrAddr = dmInit.getMemorizedErrorCodesAddress();

            byte[] request = ssmProtocol.constructReadAddressRequest(module, new byte[][]{
                    getThreeByteAddr(afAddr),
                    getThreeByteAddr(afAddr + 1),
                    getThreeByteAddr(cerrAddr),
                    getThreeByteAddr(cerrAddr + 1),
                    getThreeByteAddr(cerrAddr + 2),
                    getThreeByteAddr(cerrAddr + 3),
                    getThreeByteAddr(merrAddr),
                    getThreeByteAddr(merrAddr + 1),
                    getThreeByteAddr(merrAddr + 2),
                    getThreeByteAddr(merrAddr + 3),
            });
            if (LOGGER.isDebugEnabled())
                LOGGER.debug(module + " Init DM Runtime Params Request  ---> " + asHex(request));
            byte[] response = manager.send(request);
            byte[] processedResponse = protocol.preprocessResponse(request, response, new PollingStateImpl());
            if (LOGGER.isDebugEnabled())
                LOGGER.debug(module + " Init DM Runtime Params Response <--- " + asHex(processedResponse));
            byte responseType = processedResponse[4];
            if (responseType != SSMProtocol.READ_ADDRESS_RESPONSE) {
                // error
                return;
            }
            dmInit.updateRuntimeData(getShortFromResponse(processedResponse, 5),
                    getIntFromResponse(processedResponse, 7),
                    getIntFromResponse(processedResponse, 11));
        }
        callback.callback(dmInit);
    }

    private static int getShortFromResponse(byte[] processedResponse, int offset) {
        return (processedResponse[offset] << 8) +
                processedResponse[offset + 1];
    }
    private static int getIntFromResponse(byte[] processedResponse, int offset) {
        return (processedResponse[offset] << 24) +
                (processedResponse[offset + 1] << 16) +
                (processedResponse[offset + 2] << 8) +
                processedResponse[offset + 3];
    }

    private static byte[] getThreeByteAddr(int afAddr) {
        return new byte[]{(byte) (afAddr >> 16), (byte) (afAddr >> 8), (byte) afAddr};
    }

    @Override
    public final void sendAddressReads(
            Collection<EcuQuery> queries,
            Module module,
            PollingState pollState) {

        // Determine if ISO15765 is selected and then if TCU is selected.  If
        // both are true then proceed to split queries so max CAN data packet
        // contains 8 or less bytes, otherwise don't split up the queries.
        if (settings.isCanBus() && module.getName().equalsIgnoreCase("TCU")) {
            tcuQueries = (ArrayList<EcuQuery>) queries;
            final int tcuQueryListLength = tcuQueries.size();
            for (int i = 0; i < tcuQueryListLength; i++) {
                tcuSubQuery.clear();
                tcuSubQuery.add(tcuQueries.get(i));
                final int addrLength = tcuQueries.get(i).getAddresses().length;
                final byte[] request = protocol.constructReadAddressRequest(
                        module, tcuSubQuery);
                byte[] response = new byte[0];
                if (addrLength == 1) {
                    if (LOGGER.isDebugEnabled())
                        LOGGER.debug(module + " CAN Request  ---> " + asHex(request));
                    response = protocol.constructReadAddressResponse(
                            tcuSubQuery, pollState);
                    manager.send(request, response, pollState);
                }
                if (addrLength > 1) {
                    response = SSMLoggerCANSubQuery.doSubQuery(
                            (ArrayList<EcuQuery>) tcuSubQuery, manager,
                            protocol, module, pollState);
                }
                final byte[] processedResponse = protocol.preprocessResponse(
                        request, response, pollState);
                if (LOGGER.isDebugEnabled())
                    LOGGER.debug(module + " CAN Response <--- " + asHex(processedResponse));
                protocol.processReadAddressResponses(
                        tcuSubQuery, processedResponse, pollState);
            }
        } else {
            final byte[] request = protocol.constructReadAddressRequest(
                    module, queries);
            if (pollState.getCurrentState() == PollingState.State.STATE_0) {
                if (LOGGER.isDebugEnabled())
                    LOGGER.debug("Mode:" + pollState.getCurrentState() + " " +
                            module + " Request  ---> " + asHex(request));
            }
            final byte[] response = protocol.constructReadAddressResponse(
                    queries, pollState);
            manager.send(request, response, pollState);
            final byte[] processedResponse = protocol.preprocessResponse(
                    request, response, pollState);
            if (LOGGER.isDebugEnabled())
                LOGGER.debug("Mode:" + pollState.getCurrentState() + " " +
                        module + " Response <--- " + asHex(processedResponse));
            protocol.processReadAddressResponses(
                    queries, processedResponse, pollState);
        }
    }

    @Override
    public void clearLine() {
        manager.clearLine();
    }

    @Override
    public void close() {
        manager.close();
    }

    @Override
    public final void sendAddressWrites(
            Map<EcuQuery, byte[]> writeQueries, Module module) {

        for (EcuQuery writeKey : writeQueries.keySet()) {
            if (writeKey.getBytes().length == 3) {
                final byte[] request =
                        protocol.constructWriteAddressRequest(
                                module,
                                writeKey.getBytes(),
                                writeQueries.get(writeKey)[0]);

                if (LOGGER.isDebugEnabled())
                    LOGGER.debug(module + " Write Request  ---> " + asHex(request));
                final byte[] response = manager.send(request);
                byte[] processedResponse =
                        protocol.preprocessResponse(
                                request,
                                response,
                                new PollingStateImpl());
                if (LOGGER.isDebugEnabled())
                    LOGGER.debug(module + " Write Response <--- " + asHex(processedResponse));
                protocol.processWriteResponse(
                        writeQueries.get(writeKey), processedResponse);
            }
        }
    }
}
