/*
 * Copyright 2016 Holger Schmidt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.holgis.colorthings;

import android.util.Log;

import com.google.android.things.pio.I2cDevice;
import com.google.android.things.pio.PeripheralManagerService;

import java.io.IOException;


public class PCA9685 implements AutoCloseable {

    //PI3: busName = "I2C1"

    public final static int DEFAULT_ADDESS = 0x40;

    private static final String TAG = PCA9685.class.getSimpleName();
    private I2cDevice mI2C = null;

    private final static int REGISTER_MODE1 = 0x00;
    private final static int REGISTER_LED0_ON_L = 0x06;

    private final static byte FLAG_AUTO_INCREMENT = 0x20;


    public PCA9685(String busName, int deviceAddress) throws IOException {

        PeripheralManagerService pioService = new PeripheralManagerService();
        mI2C = pioService.openI2cDevice(busName,deviceAddress);

        try {
            // enable auto increment of register address when writing a byte[]
            mI2C.writeRegByte(REGISTER_MODE1, FLAG_AUTO_INCREMENT);
        } catch (IOException|RuntimeException e) {
            Log.e(TAG, e.getMessage());
            try {
                close();
            } catch (IOException|RuntimeException ignored) {
            }
            throw e;
        }

    }


    @Override
    public void close() throws IOException {
        if(mI2C!=null) {
            mI2C.close();
            mI2C = null;
        }
    }

    /**
     *  Set PWM duty cycle
     * @param channel PWM channel index 0 - 15
     * @param dutyCycle PWM duty cycle 0.0 - 1.0; 0% -> 100% active
     * @throws IOException
     */
    //pwm index 0 -> 15;
    //duty 0-1
    public void setPWM(int channel, float dutyCycle) throws IOException {

        setPWM(channel,dutyCycle,0.0f);

    }

    /**
     *  Set PWM duty cycle with offset
     * @param channel PWM channel index 0 - 15
     * @param dutyCycle PWM duty cycle 0.0 - 1.0; 0% -> 100% active
     * @param dutyOffset PWM duty cycle offset 0.0 - 1.0; 0% -> 100% delay
     * @throws IOException
     */

    public void setPWM(int channel, float dutyCycle, float dutyOffset) throws IOException {

        channel = ensureInBounds(channel, 0, 15);
        dutyCycle = ensureInBounds(dutyCycle, 0.0f, 1.0f);
        dutyOffset = ensureInBounds(dutyOffset, 0.0f, 1.0f);

        int on = (int)Math.floor((4095.0f * dutyOffset)+0.5);
        int off = (int)Math.floor((4095.0f * dutyCycle)+0.5);

        off += on;
        off %= 4096;

        byte data[] = new byte[4];
        data[0] = (byte)(on&0xff);
        data[1] = (byte)((on>>8)&0xff);
        data[2] = (byte)(off&0xff);
        data[3] = (byte)((off>>8)&0xff);

        mI2C.writeRegBuffer(REGISTER_LED0_ON_L + (4 * channel), data, 4);

    }

    private int ensureInBounds(int value, int lowerBound, int upperBound){
        return  Math.max(lowerBound, Math.min(value, upperBound));
    }
    private float ensureInBounds(float value, float lowerBound, float upperBound){
        return  Math.max(lowerBound, Math.min(value, upperBound));
    }
}


