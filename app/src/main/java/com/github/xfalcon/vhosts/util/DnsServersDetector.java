package com.github.xfalcon.vhosts.util;

import android.annotation.TargetApi;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.RouteInfo;
import android.os.Build;
import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static android.content.Context.CONNECTIVITY_SERVICE;

/**
 * DNS servers detector
 *
 * IMPORTANT: don't cache the result.
 *
 * Or if you want to cache the result make sure you invalidate the cache
 * on any network change.
 *
 * It is always better to use a new instance of the detector when you need
 * current DNS servers otherwise you may get into troubles because of invalid/changed
 * DNS servers.
 *
 * This class combines various methods and solutions from:
 * Dnsjava http://www.xbill.org/dnsjava/
 * Minidns https://github.com/MiniDNS/minidns
 *
 * Unfortunately both libraries are not aware of Orero changes so new method was added to fix this.
 *
 * Created by Madalin Grigore-Enescu on 2/24/18.
 */

public class DnsServersDetector {

    private static final String TAG = "DnsServersDetector";
    private ArrayList<String> vpnDns4  = new ArrayList<>();
    private ArrayList<String> vpnDns6  = new ArrayList<>();

    /**
     * Holds some default DNS servers used in case all DNS servers detection methods fail.
     * Can be set to null if you want caller to fail in this situation.
     */
    private static final String[] FACTORY_DNS_SERVERS = {
            "8.8.8.8",
            "8.8.4.4"
    };

    /**
     * Properties delimiter used in exec method of DNS servers detection
     */
    private static final String METHOD_EXEC_PROP_DELIM = "]: [";

    /**
     * Holds context this was created under
     */
    private Context context;

    //region - public //////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Constructor
     */
    public DnsServersDetector(Context context) {
        this.context = context;
        String [] dnsServers = getServers();
        for (String dnsServer : dnsServers) {
            if (dnsServer.contains(":")) {
                vpnDns6.add(dnsServer);
            } else {
                vpnDns4.add(dnsServer);
            }
        }
    }

    public ArrayList<String> getVpnDns4() {
        return vpnDns4;
    }

    public ArrayList<String> getVpnDns6() {
        return vpnDns6;
    }

    //endregion

    //region - private /////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Returns android DNS servers used for current connected network
     * @return Dns servers array
     */
    private String [] getServers() {

        // Will hold the consecutive result
        String[] result;

        // METHOD 1: old deprecated system properties
        result = getServersMethodSystemProperties();
        if (result != null && result.length > 0) {

            return result;

        }

        // METHOD 2 - use connectivity manager
        result = getServersMethodConnectivityManager();
        if (result != null && result.length > 0) {

            return result;

        }

        // LAST METHOD: detect android DNS servers by executing getprop string command in a separate process
        // This method fortunately works in Oreo too but many people may want to avoid exec
        // so it's used only as a failsafe scenario
        result = getServersMethodExec();
        if (result != null && result.length > 0) {

            return result;

        }

        // Fall back on factory DNS servers
        return FACTORY_DNS_SERVERS;

    }

    /**
     * Detect android DNS servers by using connectivity manager
     *
     * This method is working in android LOLLIPOP or later
     *
     * @return Dns servers array
     */
    private String [] getServersMethodConnectivityManager() {

        // This code only works on LOLLIPOP and higher
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

            try {

                ArrayList<String> priorityServersArrayList  = new ArrayList<>();
                ArrayList<String> serversArrayList          = new ArrayList<>();

                ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(CONNECTIVITY_SERVICE);
                if (connectivityManager != null) {

                    // Iterate all networks
                    // Notice that android LOLLIPOP or higher allow iterating multiple connected networks of SAME type
                    for (Network network : connectivityManager.getAllNetworks()) {

                        NetworkInfo networkInfo = connectivityManager.getNetworkInfo(network);
                        if ((networkInfo != null) && (networkInfo.isConnected())) {

                            LinkProperties linkProperties    = connectivityManager.getLinkProperties(network);
                            List<InetAddress> dnsServersList = linkProperties.getDnsServers();

                            // Prioritize the DNS servers for link which have a default route
                            if (linkPropertiesHasDefaultRoute(linkProperties)) {
                                for (InetAddress element: dnsServersList) {
                                    String dnsHost = element.getHostAddress();
                                    // Prioritize wifi over mobile when both are active and default route (yep! that's android babe)
                                    if (networkInfo.getType()==ConnectivityManager.TYPE_WIFI) {
                                        priorityServersArrayList.add(0, dnsHost);
                                    } else {
                                        priorityServersArrayList.add(dnsHost);
                                    }
                                }
                            } else {

                                for (InetAddress element: dnsServersList) {

                                    String dnsHost = element.getHostAddress();
                                    serversArrayList.add(dnsHost);

                                }

                            }

                        }

                    }

                }

                // Append secondary arrays only if priority is empty
                if (priorityServersArrayList.isEmpty()) {

                    priorityServersArrayList.addAll(serversArrayList);

                }

                // Stop here if we have at least one DNS server
                if (priorityServersArrayList.size() > 0) {

                    return priorityServersArrayList.toArray(new String[0]);

                }

            } catch (Exception ex) {

                Log.d(TAG, "Exception detecting DNS servers using ConnectivityManager method", ex);

            }

        }

        // Failure
        return null;

    }

    /**
     * Detect android DNS servers by using old deprecated system properties
     *
     * This method is NOT working anymore in Android 8.0
     * Official Android documentation state this in the article Android 8.0 Behavior Changes.
     * The system properties net.dns1, net.dns2, net.dns3, and net.dns4 are no longer available,
     * a change that improves privacy on the platform.
     *
     * https://developer.android.com/about/versions/oreo/android-8.0-changes.html#o-pri
     * @return Dns servers array
     */
    private String [] getServersMethodSystemProperties() {


        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {

            // This originally looked for all lines containing .dns; but
            // http://code.google.com/p/android/issues/detail?id=2207#c73
            // indicates that net.dns* should always be the active nameservers, so
            // we use those.
            final String re1 = "^\\d+(\\.\\d+){3}$";
            final String re2 = "^[0-9a-f]+(:[0-9a-f]*)+:[0-9a-f]+$";
            ArrayList<String> serversArrayList = new ArrayList<>();
            try {

                Class SystemProperties = Class.forName("android.os.SystemProperties");
                Method method = SystemProperties.getMethod("get", new Class[]{String.class});
                final String[] netdns = new String[]{"net.dns1", "net.dns2", "net.dns3", "net.dns4"};
                for (int i = 0; i < netdns.length; i++) {

                    Object[] args = new Object[]{netdns[i]};
                    String v = (String) method.invoke(null, args);
                    if (v != null && (v.matches(re1) || v.matches(re2)) && !serversArrayList.contains(v)) {
                        serversArrayList.add(v);
                    }

                }

                // Stop here if we have at least one DNS server
                if (serversArrayList.size() > 0) {

                    return serversArrayList.toArray(new String[0]);

                }

            } catch (Exception ex) {

                Log.d(TAG, "Exception detecting DNS servers using SystemProperties method", ex);

            }

        }

        // Failed
        return null;

    }

    /**
     * Detect android DNS servers by executing getprop string command in a separate process
     *
     * Notice there is an android bug when Runtime.exec() hangs without providing a Process object.
     * This problem is fixed in Jelly Bean (Android 4.1) but not in ICS (4.0.4) and probably it will never be fixed in ICS.
     * https://stackoverflow.com/questions/8688382/runtime-exec-bug-hangs-without-providing-a-process-object/11362081
     *
     * @return Dns servers array
     */
    private String [] getServersMethodExec() {

        // We are on the safe side and avoid any bug
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {

            try {

                Process process = Runtime.getRuntime().exec("getprop");
                InputStream inputStream = process.getInputStream();
                LineNumberReader lineNumberReader = new LineNumberReader(new InputStreamReader(inputStream));
                Set<String> serversSet = methodExecParseProps(lineNumberReader);
                if (serversSet != null && serversSet.size() > 0) {

                    return serversSet.toArray(new String[0]);

                }

            } catch (Exception ex) {

                Log.d(TAG, "Exception in getServersMethodExec", ex);

            }

        }

        // Failed
        return null;

    }

    /**
     * Parse properties produced by executing getprop command
     * @param lineNumberReader
     * @return Set of parsed properties
     * @throws Exception
     */
    private Set<String> methodExecParseProps(BufferedReader lineNumberReader) throws Exception {

        String line;
        Set<String> serversSet = new HashSet<String>(10);

        while ((line = lineNumberReader.readLine()) != null) {
            int split = line.indexOf(METHOD_EXEC_PROP_DELIM);
            if (split == -1) {
                continue;
            }
            String property = line.substring(1, split);

            int valueStart  = split + METHOD_EXEC_PROP_DELIM.length();
            int valueEnd    = line.length() - 1;
            if (valueEnd < valueStart) {

                // This can happen if a newline sneaks in as the first character of the property value. For example
                // "[propName]: [\n…]".
                Log.d(TAG, "Malformed property detected: \"" + line + '"');
                continue;

            }

            String value = line.substring(valueStart, valueEnd);

            if (value.isEmpty()) {

                continue;

            }

            if (property.endsWith(".dns") || property.endsWith(".dns1") ||
                    property.endsWith(".dns2") || property.endsWith(".dns3") ||
                    property.endsWith(".dns4")) {

                // normalize the address
                InetAddress ip = InetAddress.getByName(value);
                if (ip == null) continue;
                value = ip.getHostAddress();

                if (value == null) continue;
                if (value.length() == 0) continue;

                serversSet.add(value);

            }

        }

        return serversSet;

    }

    /**
     * Returns true if the specified link properties have any default route
     * @param linkProperties
     * @return true if the specified link properties have default route or false otherwise
     */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private boolean linkPropertiesHasDefaultRoute(LinkProperties linkProperties) {

        for (RouteInfo route : linkProperties.getRoutes()) {
            if (route.isDefaultRoute()) {
                return true;
            }
        }
        return false;

    }

    //endregion

}