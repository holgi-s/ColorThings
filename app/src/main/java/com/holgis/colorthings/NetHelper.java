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

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.util.Log;

public class NetHelper {

    private static final String TAG = NetHelper.class.getSimpleName();

    static public boolean isConnected(Context context) {

        Log.d(TAG,"isConnected...");

        ConnectivityManager connManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        Network[] nets = connManager.getAllNetworks();
        if(nets!=null) {
            for (Network net : nets) {
                NetworkInfo info = connManager.getNetworkInfo(net);
                if (info != null) {

                    Log.d(TAG,"found network: " + info.getTypeName() + ", " + info.getState().name());

                    if (info.isConnectedOrConnecting()) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

}
