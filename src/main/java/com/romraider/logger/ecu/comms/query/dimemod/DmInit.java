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

package com.romraider.logger.ecu.comms.query.dimemod;

import com.romraider.logger.ecu.definition.EcuParameter;
import com.sun.javafx.binding.StringFormatter;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class DmInit {
    private final byte[] dmInitBytes;
    private int currentErrorCodesAddress;
    private int memorizedErrorCodesAddress;
    private int activeFeaturesAddress;
    private int afrAddress;
    private int egtAddress;
    private int fuelPressAddress;
    private int fuelDiffPressAddress;
    private int backPressAddress;
    private int ethanolContentAddress;
    private int majorVer;
    private int minorVer;
    private int buildNum;
    private int runtimeCurrentErrors;
    private int runtimeMemErrors;
    private int runtimeActiveFeatures;

    public DmInit(byte[] dmInitBytes) {
        this.dmInitBytes = dmInitBytes;
        ByteBuffer buf = ByteBuffer.wrap(dmInitBytes);
        buf.position(0);
        majorVer = buf.get() & 0xFF;
        minorVer = buf.get() & 0xFF;
        buildNum = buf.getShort() & 0xFFFF;
        int ramSize = buf.getInt();
        byte featuresConfig0 = buf.get();
        byte featuresConfig1 = buf.get();
        byte featuresConfig2 = buf.get();
        byte featuresConfig3 = buf.get();

        boolean isRamTuneEnabled = (featuresConfig0 & 0x80) != 0;
        boolean isCruiseButtonImmediateHacksEnabled = (featuresConfig0 & 0x40) != 0;
        boolean isCorrectionsByGearsEnabled = (featuresConfig0 & 0x20) != 0;
        boolean isCelFlashEnabled = (featuresConfig0 & 0x10) != 0;
        boolean isKnockLightEnabled = (featuresConfig0 & 0x08) != 0;
        boolean isKsByCylsEnabled = (featuresConfig0 & 0x04) != 0;
        boolean isMapSwitchEnabled = (featuresConfig0 & 0x02) != 0;
        boolean isSparkCutEnabled = (featuresConfig0 & 0x01) != 0;
        boolean isSpeedDensityEnabled = (featuresConfig1 & 0x80) != 0;
        boolean isAlsEnabled = (featuresConfig1 & 0x40) != 0;
        boolean isCanSenderEnabled = (featuresConfig1 & 0x20) != 0;
        boolean isVinLockEnabled = (featuresConfig1 & 0x10) != 0;
        boolean isPwmControlEnabled = (featuresConfig1 & 0x08) != 0;
        boolean isValetModeEnabled = (featuresConfig1 & 0x04) != 0;

        // INPUTS_CONFIG
        int signature = buf.getInt();
        if (signature != 0xDEAD0001) {
            throw new IllegalStateException("DimeMod params reading failure at INPUTS_CONFIG");
        }
        currentErrorCodesAddress = buf.getInt();
        memorizedErrorCodesAddress = buf.getInt();
        activeFeaturesAddress = buf.getInt();
        afrAddress = buf.getInt();
        egtAddress = buf.getInt();
        fuelPressAddress = buf.getInt();
        fuelDiffPressAddress = buf.getInt();
        backPressAddress = buf.getInt();
        ethanolContentAddress = buf.getInt();

        if (isRamTuneEnabled) {
            signature = buf.getInt();
            if (signature != 0xDEAD0020) {
                throw new IllegalStateException("DimeMod params reading failure at RAM_TUNE");
            }
            int ramTuneSignatureAddress = buf.getInt();
            int ramTuneLutSize = buf.getInt();
        }

        if (isCelFlashEnabled) {
            signature = buf.getInt();
            if (signature != 0xDEAD0004) {
                throw new IllegalStateException("DimeMod params reading failure at CEL_FLASH");
            }
            int celOverrideStateAddress = buf.getInt();
        }

        if (isKsByCylsEnabled) {
            signature = buf.getInt();
            if (signature != 0xDEAD0006) {
                throw new IllegalStateException("DimeMod params reading failure at KS_BY_CYLS");
            }
            int knockSumCyl1Address = buf.getInt();
            int knockSumCyl3Address = knockSumCyl1Address + 1;
            int knockSumCyl2Address = knockSumCyl1Address + 2;
            int knockSumCyl4Address = knockSumCyl1Address + 3;
        }

        if (isMapSwitchEnabled) {
            signature = buf.getInt();
            if (signature != 0xDEAD0007) {
                throw new IllegalStateException("DimeMod params reading failure at MAP_SWITCH");
            }
            int msNumberOfSets = buf.getInt();
            int msCurrentSetNumberAddress = buf.getInt();
            int flexFuelBoostSetBlendAddress = buf.getInt();
            int flexFuelFuelingSetBlendAddress = buf.getInt();
            int flexFuelIgnitionSetBlendAddress = buf.getInt();
            int flexFuelOtherSetBlendAddress = buf.getInt();
            int flexFuelInjFlowValueAddress = buf.getInt();
        }

        if (isSpeedDensityEnabled) {
            signature = buf.getInt();
            if (signature != 0xDEAD0009) {
                throw new IllegalStateException("DimeMod params reading failure at SPEED_DENSITY");
            }
            int sdPortTempAddress = buf.getInt();
            int sdIatCompensationAddress = buf.getInt();
            int sdTipInCompensationAddress = buf.getInt();
            int sdAtmPressCompensationAddress = buf.getInt();
            int sdBlendingRatioAddress = buf.getInt();
            int sdBaseVeAddress = buf.getInt();
            int sdFinalVeAddress = buf.getInt();
            int alphaNIatCompensationAddress = buf.getInt();
            int alphaNAtmPressCompensationAddress = buf.getInt();
            int alphaNBaseMassAirflowAddress = buf.getInt();
            int alphaNFinalMassAirflowAddress = buf.getInt();
            int sensorMassAirflowAddress = buf.getInt();
        }

        if (isVinLockEnabled) {
            signature = buf.getInt();
            if (signature != 0xDEAD000C) {
                throw new IllegalStateException("DimeMod params reading failure at VIN_LOCK");
            }
            buf.getInt();
            buf.getInt();
            buf.getInt();
            buf.getInt();
            buf.getInt();
            buf.getInt();
            buf.getInt();
            buf.getInt();
            buf.getInt();
            buf.getInt();
            buf.getInt();
        }

        if (isPwmControlEnabled) {
            signature = buf.getInt();
            if (signature != 0xDEAD000D) {
                throw new IllegalStateException("DimeMod params reading failure at PWM_CONTROL");
            }
            int pwmControlTargetDutyAddress = buf.getInt();
        }

        if (isAlsEnabled) {
            signature = buf.getInt();
            if (signature != 0xDEAD000A) {
                throw new IllegalStateException("DimeMod params reading failure at ALS");
            }
            buf.getInt();
        }

        if (isValetModeEnabled) {
            signature = buf.getInt();
            if (signature != 0xDEAD000E) {
                throw new IllegalStateException("DimeMod params reading failure at VALET_MODE");
            }
            buf.getInt();
        }
    }

    public void updateRuntimeData(int activeFeatures, int currentErrors, int memErrors) {
        this.runtimeActiveFeatures = activeFeatures;
        this.runtimeCurrentErrors = currentErrors;
        this.runtimeMemErrors = memErrors;
    }

    /**
     * Get the DimeMod ID string.
     * @return ID string
     */
    public String getDimeModVersion() {
        return majorVer + "." + minorVer + " build " + String.format("%03d", buildNum);
    }

    public byte[] getDmInitBytes() {
        return dmInitBytes;
    }

    public Collection<? extends EcuParameter> getEcuParams() {
        List<EcuParameter> params = new ArrayList<>();

        return params;
    }

    public int getCurrentErrorCodesAddress() {
        return currentErrorCodesAddress;
    }

    public int getMemorizedErrorCodesAddress() {
        return memorizedErrorCodesAddress;
    }

    public int getActiveFeaturesAddress() {
        return activeFeaturesAddress;
    }
}
