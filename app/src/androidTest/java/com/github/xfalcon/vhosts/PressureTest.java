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

import static android.support.test.espresso.Espresso.onView;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import android.util.Log;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static org.junit.Assert.*;

/**
 * Instrumentation test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class PressureTest {

    @Rule
    public ActivityTestRule<VhostsActivity> mActivityRule = new ActivityTestRule<>(
            VhostsActivity.class);

    @Test
    public void useAppContext() throws Exception {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getTargetContext();
        assertEquals("com.github.xfalcon.vhosts", appContext.getPackageName());
        onView(withId(R.id.activity_main)).perform(click());
        Thread.sleep(1000*3);
        ArrayList<Future> list=new ArrayList<Future>();
        ExecutorService pool   =   Executors.newFixedThreadPool(100);
        for(int i=10;i<10000;i++){
            final String domain= i+".com";
            list.add(pool.submit(new Thread(){
                public void run() {
                    try {
                        InetAddress ip=InetAddress.getByName(domain);
                        Log.d("TEST",ip+" : "+domain);
                    }catch (Exception e){

                    }
                }
            }));
        }
        Iterator<Future> it=list.iterator();
        while (it.hasNext()){
            it.next().get();
        }

    }

    private static String readStream(InputStream is) throws IOException {
        StringBuilder sb = new StringBuilder();
        BufferedReader r = new BufferedReader(new InputStreamReader(is),1000);
        for (String line = r.readLine(); line != null; line =r.readLine()){
            sb.append(line);
        }
        is.close();
        return sb.toString();
    }
    public static String get_html(String url_path) {
        // TODO Auto-generated method stub
        try{
            URL url = new URL(url_path);
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.addRequestProperty("User-Agent","Mozilla/5.0 (Windows NT 6.3; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/46.0.2490.80 Safari/537.36");
            InputStream in = new BufferedInputStream(urlConnection.getInputStream());
            return readStream(in);
        } catch (MalformedURLException e) {
            // TODO Auto-generated catch block
        } catch (IOException e) {
            // TODO Auto-generated catch block
        }finally{

        };
        return "";

    }
}
