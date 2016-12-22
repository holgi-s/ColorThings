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

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.AppIdentifier;
import com.google.android.gms.nearby.connection.AppMetadata;
import com.google.android.gms.nearby.connection.Connections;


import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;


public class ColorActivity extends Activity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        Connections.ConnectionRequestListener, Connections.MessageListener {

    private static final String TAG = ColorActivity.class.getSimpleName();

    private GoogleApiClient mGoogleApiClient;
    private int mColor = Color.BLACK;

    private TextView mContentView;
    private View mRootView;

    private PCA9685 pwmController = null;

    private static int PWM_RED = 0;
    private static int PWM_GREEN = 1;
    private static int PWM_BLUE  = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_color);

        mRootView = findViewById(R.id.root_view);
        mContentView = (TextView) findViewById(R.id.fullscreen_content);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Nearby.CONNECTIONS_API)
                .build();

        try {

            pwmController = new PCA9685("I2C1", PCA9685.DEFAULT_ADDESS);

            pwmController.setPWM(PWM_RED, 0.0f);
            pwmController.setPWM(PWM_GREEN, 0.0f);
            pwmController.setPWM(PWM_BLUE, 0.0f);

        } catch (IOException e){
            Log.e(TAG, "Could not open PCA9685 device: " + e.getMessage());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(pwmController!=null){
            try {
                pwmController.close();
            } catch (Exception e){
                Log.e(TAG, e.getMessage());
            }
        }

    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

    }


    @Override
    protected void onResume() {
        super.onResume();

        mGoogleApiClient.connect();
    }

    @Override
    public void onConnected(Bundle bundle) {

        startAdvertising();
    }

    @Override
    public void onConnectionSuspended(int i) {
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            stopAdvertising();
            Nearby.Connections.stopAllEndpoints(mGoogleApiClient);
            mGoogleApiClient.disconnect();
        }
    }

    private void startAdvertising() {

        if(NetHelper.isConnected(this)) {
            // Identify that this device is the host

            // Advertising with an AppIdentifer lets other devices on the
            // network discover this application and prompt the user to
            // install the application.
            List<AppIdentifier> appIdentifierList = new ArrayList<>();
            appIdentifierList.add(new AppIdentifier(getPackageName()));
            AppMetadata appMetadata = new AppMetadata(appIdentifierList);

            // The advertising timeout is set to run indefinitely
            // Positive values represent timeout in milliseconds
            long NO_TIMEOUT = 0L;

            String name = null;
            Nearby.Connections.startAdvertising(mGoogleApiClient, name, appMetadata, NO_TIMEOUT, this)
                    .setResultCallback(new ResultCallback<Connections.StartAdvertisingResult>() {
                        @Override
                        public void onResult(Connections.StartAdvertisingResult result) {
                            if (result.getStatus().isSuccess()) {

                                Log.d(TAG, "Advertising success");
                                // Device is advertising
                            } else {
                                int statusCode = result.getStatus().getStatusCode();

                                // Advertising failed - see statusCode for more details
                                Log.e(TAG, "Advertising failed: " + statusCode +
                                        " - " + result.getStatus().getStatusMessage());
                            }
                        }
                    });

            Log.d(TAG, "Start advertising ...");
        }
        else
        {
            Log.d(TAG, "No network :(");
            finish();
        }
    }

    private void stopAdvertising() {

        Log.d(TAG, "Stop advertising");
        Nearby.Connections.stopAdvertising(mGoogleApiClient);
    }

    @Override
    public void onConnectionRequest(final String remoteEndpointId, String remoteDeviceId,
                                    String remoteEndpointName, final byte[] payload) {

        byte[] myPayload = null;

        if(mColor != Color.BLACK) {
            myPayload = buildPayload(mColor);
        }
        // Automatically accept all requests

        Log.d(TAG, "onConnectionRequest");

        Nearby.Connections.acceptConnectionRequest(mGoogleApiClient, remoteEndpointId,
                myPayload, this).setResultCallback(new ResultCallback<Status>() {

            @Override
            public void onResult(Status status) {
                if (status.isSuccess()) {

                    Log.d(TAG, "acceptConnectionRequest - success");

                    if (checkColor(payload) && mColor == Color.BLACK) {
                        mColor = parseColor(payload);
                        mRootView.setBackgroundColor(mColor);
                    }
                    mContentView.setText("");
                    stopAdvertising();
                } else {
                    Log.d(TAG, "acceptConnectionRequest - failed");
                }
            }
        });
    }

    @Override
    public void onDisconnected(String remoteEndpointId) {
        Log.d(TAG, "onDisconnected");
        mContentView.setText(R.string.dummy_content);
        startAdvertising();
    }

    @Override
    public void onMessageReceived(String endpointId, byte[] payload, boolean isReliable) {
        Log.d(TAG, "onMessageReceived");

        if(checkColor(payload)){
            mColor = parseColor(payload);
            mRootView.setBackgroundColor(mColor);
        }
    }

    private boolean checkColor(byte[] payload) {
        return (payload!=null && payload.length >= 4 * 3);
    }

    private int parseColor(byte[] payload) {
        if (payload.length >= 4 * 3) {

            ByteBuffer bb = ByteBuffer.wrap(payload);
            int red = bb.getInt();
            int green = bb.getInt();
            int blue = bb.getInt();

            if(pwmController!=null) {
                 try {
                     //adjust for brightness of the different color LEDs
                     pwmController.setPWM(PWM_RED, ((float) red / 255.f) * 1.0f);
                     pwmController.setPWM(PWM_GREEN, ((float) green / 255.f) * 0.55f);
                     pwmController.setPWM(PWM_BLUE, ((float) blue / 255.f) * 0.95f);
                 } catch(IOException e){
                     Log.e(TAG, e.getMessage());
                 }
            }
            return Color.rgb(red, green, blue);
        }
        return 0;
    }



    private byte[] buildPayload(int color){

        ByteBuffer bb = ByteBuffer.allocate(4*3);
        bb.putInt(Color.red(color));
        bb.putInt(Color.green(color));
        bb.putInt(Color.blue(color));

        return bb.array();
    }
}
