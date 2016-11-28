package org.learn.android.myhue;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.http.entity.StringEntity;
import org.json.hue.JSONObject;

import com.lidroid.xutils.HttpUtils;
import com.lidroid.xutils.exception.HttpException;
import com.lidroid.xutils.http.RequestParams;
import com.lidroid.xutils.http.ResponseInfo;
import com.lidroid.xutils.http.callback.RequestCallBack;
import com.lidroid.xutils.http.client.HttpRequest.HttpMethod;
import com.philips.lighting.hue.listener.PHBridgeConfigurationListener;
import com.philips.lighting.hue.listener.PHLightListener;
import com.philips.lighting.hue.sdk.PHAccessPoint;
import com.philips.lighting.hue.sdk.PHBridgeSearchManager;
import com.philips.lighting.hue.sdk.PHHueSDK;
import com.philips.lighting.hue.sdk.PHMessageType;
import com.philips.lighting.hue.sdk.PHSDKListener;
import com.philips.lighting.hue.sdk.utilities.PHUtilities;
import com.philips.lighting.model.PHBridge;
import com.philips.lighting.model.PHBridgeConfiguration;
import com.philips.lighting.model.PHBridgeResource;
import com.philips.lighting.model.PHGroup;
import com.philips.lighting.model.PHHueError;
import com.philips.lighting.model.PHHueParsingError;
import com.philips.lighting.model.PHLight;
import com.philips.lighting.model.PHLightState;
import org.learn.android.myhue.event.DevListUpdateByDeviceStatusEvent;
import org.learn.android.myhue.event.DevListUpdateEvent;
import org.learn.android.myhue.event.hue.HueAccessPointsFoundEvent;
import org.learn.android.myhue.event.hue.HueAuthRequiredEvent;
import org.learn.android.myhue.event.hue.HueBridgeConnectedEvent;
import org.learn.android.myhue.event.hue.HueBridgeConntectFailEvent;

import android.annotation.SuppressLint;
import android.util.Log;
import android.util.SparseBooleanArray;
import de.greenrobot.event.EventBus;

@SuppressLint("DefaultLocale")
public class HueManager {

    private static final String TAG       = "HueMgr";
    private static HueManager   mInstance = null;
    List<PHLight>               allLights = null;
    List<PHGroup>               allGroups = null;
    // private static final int MAX_HUE=65535;

    private String mBridgeIP, mBridgeUsername;
    boolean        mbOnline = false;

    public boolean isOnline() {
        return mbOnline;
    }

    public void setOnline(boolean mbOnline) {
        if (this.mbOnline != mbOnline) {
            this.mbOnline = mbOnline;
            EventBus.getDefault().post(new DevListUpdateByDeviceStatusEvent());
            if (mbOnline) {
                // from offline to online
            } else {
                // from online to offline
            }
        }
    }

    private boolean mbInSearching = false;

    public boolean isInSearching() {
        return mbInSearching;
    }

    public void setInSearching(boolean inSearching) {
        this.mbInSearching = inSearching;
    }

    private boolean lastSearchWasIPScan = false;

    private HueManager() {
        PHHueSDK hueSDK = PHHueSDK.getInstance();
        // Set the Device Name (name of your app). This will be stored in your
        // bridge whitelist entry.
        hueSDK.setAppName("MyHueApp");
        hueSDK.setDeviceName("MyHue");

        hueSDK.getNotificationManager().registerSDKListener(gSDKListener);
    }

    private void searchBridge(boolean byUPNP) {
    	PHBridgeSearchManager sm = (PHBridgeSearchManager) PHHueSDK.getInstance().getSDKService(PHHueSDK.SEARCH_BRIDGE);
    	if (byUPNP) {
    		Log.d(TAG, "Searching Hue bridge by upnp...");
    		lastSearchWasIPScan = false;
    		sm.search(true, true);
    	} else {
    		Log.d(TAG, "Searching Hue bridge by ip scan...");
    		lastSearchWasIPScan = true;
    		sm.search(false, false, true);
    	}
    }
    /**
     * search bridge
     */
    public void searchBridge() {
        searchBridge(true);
    }

    public void notifyStateChanged(boolean isFull) {
        if (true) {
            EventBus.getDefault().post(new DevListUpdateEvent());
        } /*
           * else { EventBus.getDefault().post(new
           * PropReportReceivedEvent("",(byte)0)); }
           */
    }

    private PHSDKListener gSDKListener = new PHSDKListener() {

        @Override
        public void onAccessPointsFound(List<PHAccessPoint> accessPoint) {
            Log.w(TAG, "Access Points Found. " + accessPoint.size());
            for (PHAccessPoint pp: accessPoint) {
            	Log.d(TAG, "bridgeID: " + (pp.getBridgeId() == null ? "<null>" : pp.getBridgeId()));
            	Log.d(TAG, "bridgeIP: " + (pp.getIpAddress() == null ? "<null>" : pp.getIpAddress()));
            	Log.d(TAG, "bridgeMAC: " + (pp.getMacAddress() == null ? "<null>" : pp.getMacAddress()));
            	Log.d(TAG, "bridgeUser: " + (pp.getUsername() == null ? "<null>" : pp.getUsername()));
            }
            Log.d(TAG, "==== End access points ====");
            setOnline(true);
            if (accessPoint != null && accessPoint.size() > 0) {
                PHHueSDK hueSDK = PHHueSDK.getInstance();
                hueSDK.getAccessPointsFound().clear();
                hueSDK.getAccessPointsFound().addAll(accessPoint);
                EventBus.getDefault().post(new HueAccessPointsFoundEvent(hueSDK.getAccessPointsFound()));
            }

        }

        @Override
        public void onCacheUpdated(List<Integer> arg0, PHBridge bridge) {
            Log.w(TAG, "On CacheUpdated");
            boolean isDirty = false;
            if (arg0.contains(PHMessageType.LIGHTS_CACHE_UPDATED)) {
                // lights are updated
                allLights = bridge.getResourceCache().getAllLights();
                refreshStatesCache("onCacheUpdated");
            }
            if (arg0.contains(PHMessageType.GROUPS_CACHE_UPDATED)) {
                // lights are updated
                allGroups = bridge.getResourceCache()
                        .getAllGroups();/*
                                         * List<HueLight> lstHueLights =
                                         * DeviceManager.getInstance().
                                         * getAllHueLight(); for (int i=0;
                                         * i<lstHueLights.size(); i++) {
                                         * HueLight hl = lstHueLights.get(i);
                                         * PHLight light =
                                         * getLight(hl.getEndPoint()); if (light
                                         * == null ) { if (hl.isPrevOnline()) {
                                         * isDirty = true; } } else { if
                                         * (!hl.isPrevOnline()) { isDirty =
                                         * true; } PHLightState state =
                                         * light.getLastKnownLightState(); byte
                                         * status = (byte)(state.isOn() ? 1 :
                                         * 0); if (hl.getPrevStatus() != status)
                                         * { isDirty = true; } } }
                                         */
            }
            if (true) {
                notifyStateChanged(false);
            }
        }

        @Override
        public void onBridgeConnected(PHBridge b, String username) {
            Log.d(TAG, "On onBridgeConnected");
            Log.d(TAG, username);
            PHHueSDK hueSDK = PHHueSDK.getInstance();
            hueSDK.setSelectedBridge(b);
            hueSDK.enableHeartbeat(b, 5 * 1000);// PHHueSDK.HB_INTERVAL);
            hueSDK.getLastHeartbeat().put(b.getResourceCache().getBridgeConfiguration().getIpAddress(),
                    System.currentTimeMillis());
            allLights = b.getResourceCache().getAllLights();
            allGroups = b.getResourceCache().getAllGroups();
            refreshStatesCache("onBridgeConnected");
            setOnline(true);
            EventBus.getDefault()
                    .post(new HueBridgeConnectedEvent(b.getResourceCache().getBridgeConfiguration().getIpAddress(), username));
        }

        @Override
        public void onAuthenticationRequired(PHAccessPoint accessPoint) {
            Log.w(TAG, "Authentication Required.");
            PHHueSDK.getInstance().startPushlinkAuthentication(accessPoint);
            // startActivity(new Intent(PHHomeActivity.this,
            // PHPushlinkActivity.class));
            setOnline(true);
            EventBus.getDefault().post(new HueAuthRequiredEvent());
        }

        @Override
        public void onConnectionResumed(PHBridge bridge) {
            PHHueSDK hueSDK = PHHueSDK.getInstance();
            Log.v(TAG, "onConnectionResumed" + bridge.getResourceCache().getBridgeConfiguration().getIpAddress());
            setOnline(true);
            hueSDK.getLastHeartbeat().put(bridge.getResourceCache().getBridgeConfiguration().getIpAddress(),
                    System.currentTimeMillis());
            for (int i = 0; i < hueSDK.getDisconnectedAccessPoint().size(); i++) {

                if (hueSDK.getDisconnectedAccessPoint().get(i).getIpAddress()
                        .equals(bridge.getResourceCache().getBridgeConfiguration().getIpAddress())) {
                    hueSDK.getDisconnectedAccessPoint().remove(i);
                }
            }

        }

        @Override
        public void onConnectionLost(PHAccessPoint accessPoint) {
            Log.v(TAG, "onConnectionLost : " + accessPoint.getIpAddress());
            setOnline(false);
            PHHueSDK hueSDK = PHHueSDK.getInstance();
            if (!hueSDK.getDisconnectedAccessPoint().contains(accessPoint)) {
                hueSDK.getDisconnectedAccessPoint().add(accessPoint);
            }
        }

        /**
         * on Error Called : 27:You can not connect to a bridge which is already
         * connected
         */
        @Override
        public void onError(int code, final String message) {
            Log.e(TAG, "on Error Called : " + code + ":" + message);

            if (code == PHHueError.NO_CONNECTION) {
                setOnline(false);
                Log.w(TAG, "On No Connection");
            } else if (code == PHHueError.AUTHENTICATION_FAILED || code == 1158) {
            	Log.w(TAG, "Bridge AUTHENTICATION_FAILED . . . ");
                EventBus.getDefault().post(new HueBridgeConntectFailEvent(HueBridgeConntectFailEvent.ERR_AUTHENTICATION_FAILED));
            } else if (code == PHHueError.BRIDGE_NOT_RESPONDING) {
                Log.w(TAG, "Bridge Not Responding . . . ");
                EventBus.getDefault().post(new HueBridgeConntectFailEvent(HueBridgeConntectFailEvent.ERR_NO_RESPONSE));
            } else if (code == PHMessageType.BRIDGE_NOT_FOUND) {

                if (!lastSearchWasIPScan) { // Perform an IP Scan (backup
                                            // mechanism) if UPNP and Portal
                                            // Search fails.
                	searchBridge(false);
                } else {
                    Log.d(TAG, "BRIDGE_NOT_FOUND");
                    setOnline(false);
                	EventBus.getDefault().post(new HueBridgeConntectFailEvent(HueBridgeConntectFailEvent.ERR_NO_BRIDGE_FOUND));
                }
            } else if (code == PHHueError.BRIDGE_ALREADY_CONNECTED) {
                PHBridge b = PHHueSDK.getInstance().getSelectedBridge();
                if (b == null) {
                    Log.d(TAG, "BRIDGE_ALREADY_CONNECTE, but null");
                    setOnline(false);
                } else {
                    Log.d(TAG, "BRIDGE_ALREADY_CONNECTE");
                    setOnline(true);
                    allLights = b.getResourceCache().getAllLights();
                    allGroups = b.getResourceCache().getAllGroups();
                    refreshStatesCache("BRIDGE_ALREADY_CONNECTED");
                    EventBus.getDefault().post(new HueBridgeConnectedEvent(null, null));
                }
            }
        }

        @Override
        public void onParsingErrors(List<PHHueParsingError> parsingErrorsList) {
            for (PHHueParsingError parsingError : parsingErrorsList) {
                Log.e(TAG, "ParsingError : " + parsingError.getMessage());
            }
        }
    };

    private void removeBridge() {
        PHHueSDK hueSDK = PHHueSDK.getInstance();
        PHBridge bridge = hueSDK.getSelectedBridge();
        if (bridge != null) {
	        if (hueSDK.isHeartbeatEnabled(bridge)) {
	            hueSDK.disableHeartbeat(bridge);
	        }
	        hueSDK.removeBridge(bridge);
        }
        setOnline(false);
    }

    public void removeUser() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                PHHueSDK hueSDK = PHHueSDK.getInstance();
                PHBridge bridge = hueSDK.getSelectedBridge();
                if (bridge != null) {
                    if (mBridgeUsername == null || mBridgeUsername.isEmpty() || mBridgeIP == null
                            || mBridgeIP.isEmpty()) {
                        List<PHAccessPoint> accepAccessPoints = hueSDK.getAccessPointsFound();
                        if (accepAccessPoints != null && accepAccessPoints.size() > 0) {
                            mBridgeUsername = accepAccessPoints.get(0).getUsername();
                            mBridgeIP = accepAccessPoints.get(0).getIpAddress();
                        }
                    }
                }
                if (mBridgeUsername == null || mBridgeUsername.isEmpty() || mBridgeIP == null || mBridgeIP.isEmpty()) {
                    return;
                }
                //mBridgeUsername = "MyHueBridge";
                Log.d(TAG, "Remove user" + mBridgeUsername);
                bridge.removeUsername(mBridgeUsername, new PHBridgeConfigurationListener() {
                    @Override
                    public void onSuccess() {
                        Log.d(TAG, "Hue RmUser onSuccess");
                        removeBridge();
                    }

                    @Override
                    public void onStateUpdate(Map<String, String> arg0, List<PHHueError> arg1) {
                        Log.d(TAG, "Hue RmUser onStateUpdate");
                        removeBridge();
                    }

                    @Override
                    public void onError(int arg0, String arg1) {
                        Log.d(TAG, String.format("Hue RmUser fail %d %s",  arg0, arg1));
                    }

                    @Override
                    public void onReceivingConfiguration(PHBridgeConfiguration arg0) {
                        Log.d(TAG, "Hue RmUser onReceivingConfiguration");
                        removeBridge();
                    }
                });
                /*
                String url = String.format("http://%s/api/%s/config/whitelist/%s", mBridgeIP, mBridgeUsername,
                        mBridgeUsername);
                HttpUtils http = HueManager.getHttpClient();
                Log.d(TAG, url);
                http.send(HttpMethod.DELETE, url, null, new RequestCallBack<String>() {
                    @Override
                    public void onFailure(HttpException arg0, String arg1) {
                        Log.d(TAG, "Hue RmUser fail");
                        removeBridge();
                    }

                    @Override
                    public void onSuccess(ResponseInfo<String> arg0) {
                        Log.d(TAG, "Hue RmUser OK");
                        Log.d(TAG, arg0.result);
                        removeBridge();
                    }
                });
                */
            }
        }).start();
    }

    public void remove() {
        removeUser();
        
    }

    public void stop() {
        PHHueSDK hueSDK = PHHueSDK.getInstance();
        PHBridge bridge = hueSDK.getSelectedBridge();
        if (bridge != null) {
            if (hueSDK.isHeartbeatEnabled(bridge)) {
                hueSDK.disableHeartbeat(bridge);
            }

            hueSDK.disconnect(bridge);
        }
        Log.d(TAG, "Stop");
        setOnline(false);
    }

    public static HueManager getInstance() {
        if (mInstance == null)
            mInstance = new HueManager();

        return mInstance;
    }
    public boolean isFoundByIPScan() {
    	return lastSearchWasIPScan;
    }
    PHLightListener listener = new PHLightListener() {

        @Override
        public void onSuccess() {}

        @Override
        public void onStateUpdate(Map<String, String> arg0, List<PHHueError> arg1) {
            Log.w(TAG, "Light has updated");
        }

        @Override
        public void onError(int arg0, String arg1) {}

        @Override
        public void onReceivingLightDetails(PHLight arg0) {}

        @Override
        public void onReceivingLights(List<PHBridgeResource> arg0) {}

        @Override
        public void onSearchComplete() {}
    };

    public void invokeToConnectBridge(String ip, String user) {
        mBridgeIP = ip;
        mBridgeUsername = user;
        new Thread(new Runnable() {

            @Override
            public void run() {
                PHHueSDK hueSDK = PHHueSDK.getInstance();
                PHAccessPoint lastAccessPoint = new PHAccessPoint();
                lastAccessPoint.setIpAddress(mBridgeIP);
                Log.d(TAG, "InvokeConnect IP " + (mBridgeIP==null ? "<null>" : mBridgeIP));
                if (mBridgeUsername == null) {
                	Log.d(TAG, "InvokeConnect user null");
                } else if (mBridgeUsername.isEmpty()) {
                	Log.d(TAG, "InvokeConnect user empty");
                } else {
                	lastAccessPoint.setUsername(mBridgeUsername);
                	Log.d(TAG, "InvokeConnect user " + mBridgeUsername);
                }
                if (lastAccessPoint.getUsername() == null) {
                	Log.d(TAG, "getUsername null");
                } else {
                	Log.d(TAG, "getUsername " + lastAccessPoint.getUsername());
                }
                Log.d(TAG, "Above is getUsername");
                if (!hueSDK.isAccessPointConnected(lastAccessPoint)) {
                    Log.d(TAG, "Hue: Try to connecting...");
                    hueSDK.getNotificationManager().registerSDKListener(gSDKListener);
                    hueSDK.connect(lastAccessPoint);
                } else {
                    Log.d(TAG, "AccessPointConnected");
                    setOnline(true);
                    PHBridge b = PHHueSDK.getInstance().getSelectedBridge();
		    		if (b==null) {
		    			Log.d(TAG, "Hue(nullBridge): Try to connecting...");
		    			hueSDK.getNotificationManager().registerSDKListener(gSDKListener);
		    			hueSDK.connect(lastAccessPoint);
		    		} else {
		            allLights = b.getResourceCache().getAllLights();
		            allGroups = b.getResourceCache().getAllGroups();
                    refreshStatesCache("ConnectAlready");

		            List<PHAccessPoint> accepAccessPoints = hueSDK.getAccessPointsFound();
		            if (accepAccessPoints!=null && accepAccessPoints.size() > 0 ) {
		                mBridgeUsername = accepAccessPoints.get(0).getUsername();
		                Log.d(TAG, "re-set username " + mBridgeUsername);
		              }
                    EventBus.getDefault().post(new HueBridgeConnectedEvent(mBridgeIP, mBridgeUsername));
                }
                Log.d(TAG, "invokeToConnectBridge done.");
            }
            }
        }).start();
    }


    private PHLight getLight(int idLight) {
        if (allLights == null)
            return null;
        for (PHLight light : allLights) {
            String idString = light.getIdentifier();
            int idInt = 0;
            try {
                idInt = (Integer) Integer.parseInt(idString);
            } catch (NumberFormatException nfe) {
                continue;
            }
            if (idInt == idLight) {
                return light;
            }
        }
        return null;
    }

    private PHLight getLight(String id) {
        if (allLights == null)
            return null;
        for (PHLight light : allLights) {
            String idString = light.getIdentifier();
            if (idString.equals(id)) {
                return light;
            }
        }
        return null;
    }

    public boolean isLightOnline(int idLight) {
        if (!isOnline())
            return false;
        PHLight light = getLight(idLight);
        if (light != null) {
            PHLightState state = light.getLastKnownLightState();
            return state.isReachable();
        }
        return (false);
    }

    public PHLightState getLightStatus(int idLight) {
        PHLight light = getLight(idLight);
        if (light != null) {
            return light.getLastKnownLightState();
        }
        return null;
    }

    public void updateLightStatus(int idLight, PHLightState st) {
        PHBridge bridge = PHHueSDK.getInstance().getSelectedBridge();
        if (bridge != null) {
            PHLight light = getLight(idLight);
            if (light != null) {
                bridge.updateLightState(light, st);
            }
        }
    }

    public void toggleLight(int idLight) {
        Log.d(TAG, "Want toggle HUE light id " + idLight);
        PHLight light = getLight(idLight);
        if (light != null) {
            Log.d(TAG, "Found light " + light.getIdentifier());
            PHLightState state = light.getLastKnownLightState();
            boolean isPrevOn = state.isOn();
            state.setOn(!isPrevOn);
            Log.d(TAG, " Set it to " + (!isPrevOn));
            PHBridge b = PHHueSDK.getInstance().getSelectedBridge();
            b.updateLightState(light, state);
        }
    }

    private PHGroup getGroup(int idGroup) {
        if (allGroups == null)
            return null;
        for (PHGroup group : allGroups) {
            String idString = group.getIdentifier();
            int idInt = 0;
            try {
                idInt = (Integer) Integer.parseInt(idString);
            } catch (NumberFormatException nfe) {
                continue;
            }
            if (idInt == idGroup) {
                return group;
            }
        }
        return null;
    }

    private List<PHLight> getLighsFromGroups(PHGroup group) {
        ArrayList<PHLight> lights = new ArrayList<PHLight>();
        List<String> idLights = group.getLightIdentifiers();
        for (String id : idLights) {
            PHLight light = getLight(id);
            if (light != null) {
                lights.add(light);
            }
        }
        return lights;
    }

    private List<Integer> getLighsIDIntFromGroups(PHGroup group) {
        ArrayList<Integer> idIntLights = new ArrayList<Integer>();
        List<String> idLights = group.getLightIdentifiers();
        for (String id : idLights) {
            try {
                int idInt = (Integer) Integer.parseInt(id);
                idIntLights.add(idInt);
            } catch (NumberFormatException nfe) {
                continue;
            }
        }
        return idIntLights;
    }

    /** any light member online, it should be online */
    public boolean isGroupOnline(int idGroup) {
        if (!isOnline())
            return false;
        PHGroup group = getGroup(idGroup);
        if (group != null) {
            List<PHLight> lights = getLighsFromGroups(group);
            boolean isOnline = false;
            for (PHLight light : lights) {
                PHLightState state = light.getLastKnownLightState();
                if (state.isReachable()) {
                    isOnline = true;
                    break;
                }
            }
            return isOnline;
        } else {
            return false;
        }
    }

    public void toggleGroup(int idGroup) {
        Log.d(TAG, "Want toggle HUE Group id " + idGroup);
        PHGroup group = getGroup(idGroup);
        if (group == null) {
            return;
        }
        Log.d(TAG, "Found group " + group.getIdentifier());
        boolean old = isGroupOn(idGroup);
        boolean want = !old;
        List<String> idLights = group.getLightIdentifiers();
        PHBridge b = PHHueSDK.getInstance().getSelectedBridge();
        for (String idLight : idLights) {
            PHLight pl = getLight(idLight);
            PHLightState st = pl.getLastKnownLightState();
            if (st.isOn() != want) {
                st.setOn(want);
                b.updateLightState(pl, st);
                Log.d(TAG, String.format("Try set light %s to ", idLight) + want);
            }
        }
    }

    public void dump() {}

    public static HttpUtils getHttpClient() {
        return new HttpUtils(3 * 1000);
    }

    public boolean setLightOn(final HueLight hl, final boolean state) {
        HueBridge hb = hl.getBridge();
        if (hb == null) {
            Log.d(TAG, "No bridge");
            return false;
        }
//        fake_setLightOn(hl, state);
        Log.d(TAG, "hb ip " + hb.getIp());
        Log.d(TAG, " username " + hb.getUsername());
        String url_prefix = String.format("http://%s/api/%s", hb.getIp(), hb.getUsername());
        HttpUtils http = HueManager.getHttpClient();

        JSONObject param = new JSONObject();
        param.put("on", state);
        RequestParams params = new RequestParams();
        try {
            params.setBodyEntity(new StringEntity(param.toString()));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        String url = String.format("%s/%s/%d/%s", url_prefix,
                (hl.getAppDevId() == HueBridge.APPID_DEV_HUE_LIGHT) ? "lights" : "groups", hl.getEndPoint(),
                (hl.getAppDevId() == HueBridge.APPID_DEV_HUE_LIGHT) ? "state" : "action");
        Log.d(TAG, url);
        http.send(HttpMethod.PUT, url, params, new RequestCallBack<String>() {

            @Override
            public void onFailure(HttpException arg0, String arg1) {}

            @Override
            public void onSuccess(ResponseInfo<String> arg0) {
                Log.d(TAG, arg0.result);
            }

        });
        return true;
    }

    public boolean setLightRGB(HueLight hl, int r, int g, int b) {
        float[] xy = PHUtilities.calculateXYFromRGB(r, g, b, "");
        /*
         * PHLightState stateHueLight =
         * HueManager.getInstance().getLightStatus(mDevice.getEndPoint());
         * stateHueLight.setX(xy[0]); stateHueLight.setY(xy[1]);
         * HueManager.getInstance().updateLightStatus(mDevice.getEndPoint(),
         * stateHueLight);
         */

        HueBridge hb = hl.getBridge();
        if (hb == null) {
            Log.d(TAG, "No bridge");
            return false;
        }
        Log.d(TAG, "hb ip " + hb.getIp());
        Log.d(TAG, " username " + hb.getUsername());
		String url_prefix = String.format("http://%s/api/%s", hb.getIp().trim(), hb.getUsername().trim());
        HttpUtils http = HueManager.getHttpClient();

        JSONObject param = new JSONObject();
        param.put("xy", xy);
        RequestParams params = new RequestParams();
        try {
            params.setBodyEntity(new StringEntity(param.toString()));
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        String url = String.format("%s/%s/%d/%s", url_prefix,
                (hl.getAppDevId() == HueBridge.APPID_DEV_HUE_LIGHT) ? "lights" : "groups", hl.getEndPoint(),
                (hl.getAppDevId() == HueBridge.APPID_DEV_HUE_LIGHT) ? "state" : "action");
        Log.d(TAG, url);
        http.send(HttpMethod.PUT, url, params, new RequestCallBack<String>() {

            @Override
            public void onFailure(HttpException arg0, String arg1) {}

            @Override
            public void onSuccess(ResponseInfo<String> arg0) {
                Log.d(TAG, arg0.result);
            }

        });
        return true;
    }

    public boolean setLightBrightness(HueLight hl, int brightness) {
        HueBridge hb = hl.getBridge();
        if (hb == null) {
            Log.d(TAG, "No bridge");
            return false;
        }
        Log.d(TAG, "hb ip " + hb.getIp());
        Log.d(TAG, " username " + hb.getUsername());
		String url_prefix = String.format("http://%s/api/%s", hb.getIp().trim(), hb.getUsername().trim());
        HttpUtils http = HueManager.getHttpClient();

        JSONObject param = new JSONObject();
        param.put("bri", brightness);
        param.put("on", brightness != 0);
        RequestParams params = new RequestParams();
        try {
            params.setBodyEntity(new StringEntity(param.toString()));
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        String url = String.format("%s/%s/%d/%s", url_prefix,
                (hl.getAppDevId() == HueBridge.APPID_DEV_HUE_LIGHT) ? "lights" : "groups", hl.getEndPoint(),
                (hl.getAppDevId() == HueBridge.APPID_DEV_HUE_LIGHT) ? "state" : "action");
        Log.d(TAG, url);
        http.send(HttpMethod.PUT, url, params, new RequestCallBack<String>() {

            @Override
            public void onFailure(HttpException arg0, String arg1) {}

            @Override
            public void onSuccess(ResponseInfo<String> arg0) {
                Log.d(TAG, arg0.result);
            }

        });
        return true;
    }

    public static float constrain(float in, float min, float max) {
        if (in < min)
            in = min;
        else if (in > max)
            in = max;
        return in;
    }

    public static int HSBtoColor(float h, float s, float b) {
        h = constrain(h, 0.0f, 1.0f);
        s = constrain(s, 0.0f, 1.0f);
        b = constrain(b, 0.0f, 1.0f);

        float red = 0.0f;
        float green = 0.0f;
        float blue = 0.0f;

        final float hf = (h - (int) h) * 6.0f;
        final int ihf = (int) hf;
        final float f = hf - ihf;
        final float pv = b * (1.0f - s);
        final float qv = b * (1.0f - s * f);
        final float tv = b * (1.0f - s * (1.0f - f));

        switch (ihf) {
        case 0: // Red is the dominant color
            red = b;
            green = tv;
            blue = pv;
            break;
        case 1: // Green is the dominant color
            red = qv;
            green = b;
            blue = pv;
            break;
        case 2:
            red = pv;
            green = b;
            blue = tv;
            break;
        case 3: // Blue is the dominant color
            red = pv;
            green = qv;
            blue = b;
            break;
        case 4:
            red = tv;
            green = pv;
            blue = b;
            break;
        case 5: // Red is the dominant color
            red = b;
            green = pv;
            blue = qv;
            break;
        }

        return 0xFF000000 | (((int) (red * 255.0f)) << 16) | (((int) (green * 255.0f)) << 8) | ((int) (blue * 255.0f));
    }

    /* ----------state of light & group------------ */
    SparseBooleanArray mStateLights = new SparseBooleanArray();

    private void refreshStatesCache(String log) {
        if (allLights == null)
            return;
        Log.d(TAG, "Hue refresh state: " + log);
        mStateLights.clear();
        for (PHLight light : allLights) {
            PHLightState state = light.getLastKnownLightState();
            String idString = light.getIdentifier();
            int idInt = 0;
            try {
                idInt = (Integer) Integer.parseInt(idString);
                mStateLights.put(idInt, state.isOn());
            } catch (NumberFormatException nfe) {
                continue;
            }
        }
    }

    public boolean isLightOn(int idLight) {
        boolean state = false;
        Object ret = mStateLights.get(Integer.valueOf(idLight));
        if (ret != null && ret instanceof Boolean) {
            state = (Boolean) ret;
        }
        return state;
    }

    public boolean isLightOn(String idStrLight) {
        try {
            int idInt = 0;
            idInt = (Integer) Integer.parseInt(idStrLight);
            return isLightOn(idInt);
        } catch (NumberFormatException nfe) {
            return false;
        }
    }

    /** any light member on, it should be on */
    public boolean isGroupOn(int idGroup) {
        PHGroup group = getGroup(idGroup);
        if (group == null) {
            return false;
        }
        boolean state = false;
        List<String> idLights = group.getLightIdentifiers();
        for (String idLight : idLights) {
            if (isLightOn(idLight)) {
                Log.d(TAG, String.format("Light %s in group %s is on.", idLight, group.getIdentifier()));
                state = true;
                break;
            }
        }
        return state;
    }

    private void fake_setLightOn(HueLight hl, boolean state) {
        boolean isGroup = (hl.getAppDevId() == HueBridge.APPID_DEV_HUE_GROUP);
        int idInt = hl.getEndPoint();
        Log.d(TAG, String.format("Fake set HUE %s(%d) to ", isGroup ? "Group" : "Light", idInt) + state);
        if (isGroup) {
            PHGroup grp = getGroup(idInt);
            if (grp != null) {
                List<Integer> idLights = getLighsIDIntFromGroups(grp);
                for (Integer idLight : idLights) {
                    Log.d(TAG, String.format("Fake hueGroup(%d) Light(%d)", idInt, idLight));
                    mStateLights.put(idLight, state);
                }
                notifyStateChanged(true);
            }
        } else {
            Log.d(TAG, String.format("Fake hueLight(%d)", idInt));
            mStateLights.put(idInt, state);
            notifyStateChanged(true);
        }
    }
}

/*
 * HttpPost request = new HttpPost(String.format("http://%s/%s/groups/%s",
 * hb.getIp(), hb.getUsername(), group.getIdentifier())); // 鍏堝皝瑁呬竴涓� JSON 瀵硅薄
 * JSONObject param = new JSONObject(); param.put("name", "rarnu");
 * param.put("password", "123456"); // 缁戝畾鍒拌姹� Entry StringEntity se = new
 * StringEntity(param.toString()); request.setEntity(se); // 鍙戦�佽姹� HttpResponse
 * httpResponse = new DefaultHttpClient().execute(request); // 寰楀埌搴旂瓟鐨勫瓧绗︿覆锛岃繖涔熸槸涓�涓�
 * JSON 鏍煎紡淇濆瓨鐨勬暟鎹� String retSrc = EntityUtils.toString(httpResponse.getEntity());
 * // 鐢熸垚 JSON 瀵硅薄 JSONObject result = new JSONObject( retSrc); String token =
 * result.get("token");
 * 
 */
