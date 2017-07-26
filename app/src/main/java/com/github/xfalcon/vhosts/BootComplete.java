/*
**Copyright (C) 2017  xfalcon
**
**This program is free software: you can redistribute it and/or modify
**it under the terms of the GNU General Public License as published by
**the Free Software Foundation, either version 3 of the License, or
**(at your option) any later version.
**
**This program is distributed in the hope that it will be useful,
**but WITHOUT ANY WARRANTY; without even the implied warranty of
**MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
**GNU General Public License for more details.
**
**You should have received a copy of the GNU General Public License
**along with this program.  If not, see <http://www.gnu.org/licenses/>.
**
*/
package com.github.xfalcon.vhosts;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import com.github.xfalcon.vhosts.vservice.VhostsService;


public class BootComplete extends BroadcastReceiver {

    public static final String PREFS_NAME = MainActivity.class.getName();
    public static final String RECONNECT_ON_REBOOT = "RECONNECT_ON_REBOOT";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (checkRebootStatus(context)) {
            VhostsService.checkStartVpnOnBoot(context);
        }
    }

    private boolean checkRebootStatus(Context context) {
        SharedPreferences settings = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return settings.getBoolean(RECONNECT_ON_REBOOT, false);
    }
}
