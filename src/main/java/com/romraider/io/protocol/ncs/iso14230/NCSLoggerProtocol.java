/*
 * RomRaider Open-Source Tuning, Logging and Reflashing
 * Copyright (C) 2006-2021 RomRaider.com
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

package com.romraider.io.protocol.ncs.iso14230;

import static com.romraider.io.protocol.ncs.iso14230.NCSResponseProcessor.extractResponseData;
import static com.romraider.io.protocol.ncs.iso14230.NCSResponseProcessor.filterRequestFromResponse;
import static com.romraider.util.ParamChecker.checkNotNull;
import static com.romraider.util.ParamChecker.checkNotNullOrEmpty;
import static java.lang.System.arraycopy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.romraider.io.protocol.Protocol;
import com.romraider.io.protocol.ProtocolNCS;
import com.romraider.logger.ecu.comms.io.protocol.LoggerProtocolNCS;
import com.romraider.logger.ecu.comms.manager.PollingState;
import com.romraider.logger.ecu.comms.query.EcuInit;
import com.romraider.logger.ecu.comms.query.EcuInitCallback;
import com.romraider.logger.ecu.comms.query.EcuQuery;
import com.romraider.logger.ecu.comms.query.EcuQueryData;
import com.romraider.logger.ecu.definition.Module;

public final class NCSLoggerProtocol implements LoggerProtocolNCS {
    private final ProtocolNCS protocol = new NCSProtocol();

    @Override
    public byte[] constructEcuFastInitRequest(Module module) {
        return protocol.constructEcuFastInitRequest(module);
    }

    @Override
    public byte[] constructStartDiagRequest(Module module) {
        return protocol.constructStartDiagRequest(module);
    }

    @Override
    public byte[] constructElevatedDiagRequest(Module module) {
        return protocol.constructElevatedDiagRequest(module);
    }

    @Override
    public byte[] constructEcuStopRequest(Module module) {
        return protocol.constructEcuStopRequest(module);
    }

    @Override
    public byte[] constructEcuInitRequest(Module module) {
        return protocol.constructEcuInitRequest(module);
    }

    @Override
    public byte[] constructEcuIdRequest(Module module) {
        return protocol.constructEcuIdRequest(module);
    }

    @Override
    public byte[] constructEcuResetRequest(Module module, int resetCode) {
        return protocol.constructEcuResetRequest(module, resetCode);
    }

    @Override
    public byte[] constructReadAddressRequest(Module module,
            Collection<EcuQuery> queries) {
    return protocol.constructReadAddressRequest(
            module, new byte[0][0]);
    }

    @Override
    public byte[] constructReadAddressRequest(
            Module module, Collection<EcuQuery> queries, PollingState pollState) {
        return protocol.constructReadAddressRequest(
                module, new byte[0][0], pollState);
    }

    @Override
    public byte[] constructReadSidPidRequest(Module module, byte sid, byte[] pid) {
        final byte[][] request = new byte[1][pid.length];
        arraycopy(pid, 0, request[0], 0, pid.length);
        return protocol.constructReadSidPidRequest(module, sid, request);
    }

    @Override
    public byte[] constructLoadAddressRequest(Collection<EcuQuery> queries) {
        Collection<EcuQuery> filteredQueries = filterDuplicates(queries);
        return protocol.constructLoadAddressRequest(
                convertToByteAddresses(filteredQueries));
    }

    @Override
    public byte[] constructReadMemoryRequest(Module module,
            Collection<EcuQuery> queries, int length) {
        return null;
    }

    @Override
    public byte[] constructReadMemoryResponse(int requestSize, int length) {
        return null;
    }

    @Override
    public void validateLoadAddressResponse(byte[] response) {
        protocol.validateLoadAddressResponse(response);
    }

    @Override
    public byte[] constructReadAddressResponse(
            Collection<EcuQuery> queries, PollingState pollState) {

        checkNotNullOrEmpty(queries, "queries");
        // length
        // one byte  - Response sid
        // one byte  - option
        // variable bytes of data defined for pid
        // checksum
        Collection<EcuQuery> filteredQueries = filterDuplicates(queries);
        int numAddresses = 0;
        for (EcuQuery ecuQuery : filteredQueries) {
            numAddresses += EcuQueryData.getDataLength(ecuQuery); 
        }
        return new byte[(numAddresses + 4)];
    }

    @Override
    public byte[] preprocessResponse(
            byte[] request, byte[] response, PollingState pollState) {

        return filterRequestFromResponse(request, response, pollState);
    }

    @Override
    public void processEcuInitResponse(EcuInitCallback callback, byte[] response) {
        checkNotNull(callback, "callback");
        checkNotNullOrEmpty(response, "response");
        EcuInit ecuInit = protocol.parseEcuInitResponse(response);
        callback.callback(ecuInit);
    }

    @Override
    public byte[] processEcuIdResponse(byte[] response) {
        checkNotNullOrEmpty(response, "response");
        return protocol.parseResponseData(response);
    }

    @Override
    public byte[] processReadSidPidResponse(byte[] response) {
        checkNotNullOrEmpty(response, "response");
        return protocol.checkValidSidPidResponse(response);
    }

    @Override
    public void processEcuResetResponse(byte[] response) {
        checkNotNullOrEmpty(response, "response");
        protocol.checkValidEcuResetResponse(response);
    }

    // processes the response bytes and sets individual responses on corresponding query objects
    @Override
    public void processReadAddressResponses(
            Collection<EcuQuery> queries, byte[] response, PollingState pollState) {

        checkNotNullOrEmpty(queries, "queries");
        checkNotNullOrEmpty(response, "response");
        final byte[] responseData = extractResponseData(response);
        final Collection<EcuQuery> filteredQueries = filterDuplicates(queries);
        final Map<String, byte[]> addressResults = new HashMap<String, byte[]>();
        int i = 0;
        for (EcuQuery filteredQuery : filteredQueries) {
            final int dataLength = EcuQueryData.getDataLength(filteredQuery);
            final byte[] data = new byte[dataLength];
            arraycopy(responseData, i, data, 0, dataLength);
            addressResults.put(filteredQuery.getHex(), data);
            i += dataLength;
        }
        for (EcuQuery query : queries) {
            query.setResponse(addressResults.get(query.getHex()));
        }
    }

    @Override
    public void processReadMemoryResponses(Collection<EcuQuery> queries, byte[] response) {
    }

    @Override
    public Protocol getProtocol() {
        return protocol;
    }

    @Override
    public byte[] constructWriteAddressRequest(
            Module module, byte[] writeAddress, byte value) {

        return protocol.constructWriteAddressRequest(module, writeAddress, value);
    }

    @Override
    public void processWriteResponse(byte[] data, byte[] response) {
        checkNotNullOrEmpty(data, "data");
        checkNotNullOrEmpty(response, "response");
        protocol.checkValidWriteResponse(data, response);
    }

    @Override
    public Collection<EcuQuery> filterDuplicates(Collection<EcuQuery> queries) {
        Collection<EcuQuery> filteredQueries = new ArrayList<EcuQuery>();
        for (EcuQuery query : queries) {
            if (!filteredQueries.contains(query)) {
                filteredQueries.add(query);
            }
        }
        return filteredQueries;
    }

    private byte[][] convertToByteAddresses(Collection<EcuQuery> queries) {
        int byteCount = 0;
        for (EcuQuery query : queries) {
            byteCount += query.getAddresses().length;
        }
        byte[][] addresses = new byte[byteCount][];
        int i = 0;
        for (EcuQuery query : queries) {
            byte[] bytes = query.getBytes();
            int addrCount = query.getAddresses().length;
            int addrLen = bytes.length / addrCount;
            for (int j = 0; j < addrCount; j++) {
                final byte[] addr = new byte[addrLen];
                arraycopy(bytes, j * addrLen, addr, 0, addr.length);
                addresses[i++] = addr;
            }
        }
        return addresses;
    }
}
