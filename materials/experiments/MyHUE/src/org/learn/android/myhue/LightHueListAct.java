package org.learn.android.myhue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import com.philips.lighting.hue.listener.PHLightListener;
import com.philips.lighting.hue.sdk.PHAccessPoint;
import com.philips.lighting.hue.sdk.PHHueSDK;
import com.philips.lighting.model.PHBridge;
import com.philips.lighting.model.PHBridgeResource;
import com.philips.lighting.model.PHGroup;
import com.philips.lighting.model.PHHueError;
import com.philips.lighting.model.PHLight;
import org.learn.android.myhue.adapter.BaseAdapterHelper;
import org.learn.android.myhue.adapter.QuickAdapter;
import org.learn.android.myhue.event.DevListUpdateEvent;
import org.learn.android.myhue.event.NewDeviceAddedEvent;
import org.learn.android.myhue.event.hue.HueAccessPointsFoundEvent;
import org.learn.android.myhue.event.hue.HueAuthRequiredEvent;
import org.learn.android.myhue.event.hue.HueBridgeConnectedEvent;
import org.learn.android.myhue.event.hue.HueBridgeConntectFailEvent;
import org.learn.android.myhue.utils.DialogUtils;
import org.learn.android.myhue.utils.WindowUtil;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Intent;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import de.greenrobot.event.EventBus;

@SuppressLint("DefaultLocale")
public class LightHueListAct extends BaseActivity {

    private static final String TAG              = "HueTester";
    private static final int    MENUID_SEARCH    = Menu.FIRST + 2;
    private static final int    MENUID_UPDATE    = Menu.FIRST + 3;
    private static final int    MENUID_RE_SEARCH = Menu.FIRST + 4;
    private static final int    MENUID_RM_USER   = Menu.FIRST + 5;
    private static final int    MENUID_SIMU      = Menu.FIRST + 6;


    private Dialog   dlgSetHueGateway;
    private TextView mText, txtTitle, mOptionMenu;
    private ImageView imgBack;

    private HueBridge           mHueBridge;
    private ArrayList<HueLight> mLights;

    private GridView               mLightsGridView;
    private QuickAdapter<HueLight> mLightsAdapter;

    @Override
    protected int getContentViewId() {
        return R.layout.activity_light_hue_list;
    }

    @Override
    protected void findViews() {
        mText = getView(R.id.text_main);
        txtTitle = getView(R.id.tv_title);
        mLightsGridView = getView(R.id.gv_hue_light);
        imgBack = getView(R.id.iv_back);
        mOptionMenu = getView(R.id.tv_right);
    }

    private void tip(String txt) {
        StringBuffer sb = new StringBuffer(txt);
        if (mHueBridge == null || mHueBridge.getIp() == null || mHueBridge.getIp().isEmpty()) {
            // skip
        } else {
            sb.append("@").append(mHueBridge.getIp());
        }
        mText.setText(sb.toString());
    }

    @Override
    protected void initData() {
        txtTitle.setText(R.string.title_hue_list);
        mHueBridge = getValidHueBridge();
        tip("Select search or update");
        EventBus.getDefault().register(this);
        //searchBridge();
        mOptionMenu.setVisibility(View.VISIBLE);
        mOptionMenu.setText("...");
        imgBack.setVisibility(View.GONE);
    }

    @Override
    protected void setListener() {
        imgBack.setOnClickListener(new OnClickListener() {
            
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        mLightsGridView.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                click((HueLight) parent.getAdapter().getItem(position), position);
            }
        });

        mLightsGridView.setOnItemLongClickListener(new OnItemLongClickListener() {

            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                longClick((HueLight) parent.getAdapter().getItem(position), position);
                return true;
            }
        });

        mOptionMenu.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				openOptionsMenu();
			}
		});
    }

    @Override
    protected void onPause() {
        // store lights ?
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mHueBridge = getValidHueBridge();
        if (mHueBridge != null) {
            updateBridge();
        }
    }

    @Override
    public void onDestroy() {
        HueManager.getInstance().stop();
        EventBus.getDefault().unregister(this);
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.main, menu);
        if (mHueBridge == null) {
            menu.add(Menu.NONE, MENUID_SEARCH, 2, R.string.action_search);
            //menu.add(Menu.NONE, MENUID_SIMU, 6, "Simu");
        } else {
            menu.add(Menu.NONE, MENUID_RE_SEARCH, 2, R.string.action_re_search);
            menu.add(Menu.NONE, MENUID_UPDATE, 3, R.string.action_update);
            menu.add(Menu.NONE, MENUID_RM_USER, 4, "rm auth");
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        switch (id) {
            case MENUID_SEARCH:
            case MENUID_RE_SEARCH:
                searchBridge();
                break;
            case MENUID_UPDATE:
                updateBridge();
                break;
            case MENUID_RM_USER:
                HueManager.getInstance().removeUser();
                break;
            case MENUID_SIMU:
                simuConnected();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private QuickAdapter<HueLight> createAdapter(List<HueLight> devices) {
        return new QuickAdapter<HueLight>(mContext, R.layout.item_gv, devices) {

            @Override
            protected void convert(BaseAdapterHelper helper, HueLight item) {
                ImageView iv = helper.getView(R.id.iv_gv_item);
                TextView tv = helper.getView(R.id.tv_gv_device_name);
                String name;
                if (item.getAppDevId() == HueBridge.APPID_DEV_HUE_LIGHT) {
                    name = String.format("Light %d", item.getEndPoint());
                } else {
                    name = String.format("Group %d", item.getEndPoint());
                }

                // Log.d(TAG, String.format("Draw %s", name));
                iv.setImageResource(item.getIcon());
                tv.setText(name);
            }
        };
    }

    private void updateBridge() {
        tip("Updating...");
        mHueBridge = getValidHueBridge();
        String username = mHueBridge.getUsername();
        Log.d(TAG, username == null ? "<null>" : username);
        if (username == null || username.isEmpty() || username.equals(" ")) {
        	Log.d(TAG, "updateBridge user null");
        	username = null;
        } else {
        	Log.d(TAG, "update bridge by user " + username);
        }
        HueManager.getInstance().invokeToConnectBridge(mHueBridge.getIp(), username);
    }

    private void searchBridge() {
        mHueBridge = getValidHueBridge();
        // if (mHueBridge == null) { // do not search again if you have one
        tip("Searching...");
        HueManager.getInstance().searchBridge();
        // } else {
        // mText.setText("Has hue bridge remembered.");
        // }
    }

    private void refreshUI() {
        Collections.sort(mLights, new Comparator<HueLight>() {
            @Override
            public int compare(HueLight obj1, HueLight obj2) {
                int id1 = obj1.getAppDevId();
                int id2 = obj2.getAppDevId();
                if (id1 == id2) {
                    int ord1 = obj1.getEndPoint();
                    int ord2 = obj2.getEndPoint();
                    return ord1 - ord2;
                } else if (id1 == HueBridge.APPID_DEV_HUE_LIGHT) {
                    return -1;
                } else {
                    return 1;
                }
            }
        });
        mLightsAdapter = createAdapter(mLights);
        mLightsGridView.setAdapter(mLightsAdapter);
    }

    private boolean validString(String s) {
        return s != null && (!s.isEmpty());
    }

    private HueBridge getValidHueBridge() {
        HueBridge hb = null;
        HueSharedPreferences prefs = HueSharedPreferences.getInstance(LightHueListAct.this);
        String ip = prefs.getLastConnectedIPAddress();
        String mac = prefs.getLastConnectedMAC();
        String user = prefs.getUsername();
        if (validString(ip) && validString(mac)) {
            hb = HueBridge.loadHueBridge(mac, ip, user, prefs.getLastAuthStatus());
        }
        return hb;
    }

    public void onEventMainThread(HueAccessPointsFoundEvent evt) {
    	if (HueManager.getInstance().isFoundByIPScan()) {
    		Toast.makeText(mContext, "Found by IPScan", Toast.LENGTH_SHORT).show();
    	} else {
    		Toast.makeText(mContext, "Found by UPNP", Toast.LENGTH_SHORT).show();
    	}
        List<PHAccessPoint> lst = evt.getData();
        if (lst != null && lst.size() > 0) {
            tip("Found hue bridge");
            PHAccessPoint accessPoint = lst.get(0);
            Log.d(TAG, "---- HUE accepoint found ----");
            Log.d(TAG, "   IP " + accessPoint.getIpAddress());
            Log.d(TAG, "   MAC " + accessPoint.getMacAddress());
            Log.d(TAG, "   Username " + accessPoint.getUsername());
            Log.d(TAG, "==== HUE accepoint found ====");
            // write back
            String ip = accessPoint.getIpAddress();
            String mac = accessPoint.getMacAddress();
            String user = accessPoint.getUsername();
            if (user == null || user.isEmpty()) {
                Log.d(TAG, "hap found user null");
            } else {
            	Log.d(TAG, "hap found user " + user);
            }
            String id = mac;
            // remove FFFE: new StringBuilder(id).delete(6, 10).toString();
            mHueBridge = HueBridge.loadHueBridge(id, ip, user, false);
            mHueBridge.setOnline(true);
            EventBus.getDefault().post(new DevListUpdateEvent());
            // EventBus.getDefault().post(new NewDeviceAddedEvent("HUE网关"));
            HueSharedPreferences prefs = HueSharedPreferences.getInstance(LightHueListAct.this);
            boolean isStored = prefs.setLastConnectedIPAddress(ip);
            Log.d(TAG, "Store HUE ip " + isStored);
            isStored = prefs.setLastConnectedMAC(id);
            Log.d(TAG, "Store HUE mac " + isStored);
            isStored = prefs.setUsername(user);
            Log.d(TAG, "Store HUE user " + isStored);
            isStored = prefs.setLastAuthStatus(false);
            Log.d(TAG, "Store HUE auth " + isStored);
            updateBridge();
        } else {
            tip("No bridge found");
            Log.e(TAG, "Oooops, found nothing HUE access points.");
        }
    }

    public void onEventMainThread(NewDeviceAddedEvent evt) {
        Toast.makeText(mContext, evt.getName(), Toast.LENGTH_SHORT).show();
    }

    PHLightListener mHueLightListener = new PHLightListener() {

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

    public void onEventMainThread(HueBridgeConntectFailEvent evt) {
        if (dlgSetHueGateway != null) {
            dlgSetHueGateway.dismiss();
            dlgSetHueGateway = null;
        }
        String err = "No response";
        switch (evt.getErrorCode()) {
        case HueBridgeConntectFailEvent.ERR_NO_BRIDGE_FOUND:
        	err = "Bridge not found";
        	break;
        case HueBridgeConntectFailEvent.ERR_AUTHENTICATION_FAILED:
        	err = "Auth fail";
        	break;
        case HueBridgeConntectFailEvent.ERR_NO_RESPONSE:
        default:
        	break;
        }
        Toast.makeText(LightHueListAct.this, err, Toast.LENGTH_SHORT).show();
    }

    private void simuConnected() {
        Log.d(TAG, "Simu connected for test.");
        mHueBridge = new HueBridge("127.0.0.1", 1);
        mHueBridge.setOnline(true);
        mLights = new ArrayList<HueLight>();
        for (int i=1; i<4; i++) {
            HueLight hl = new HueLight(mHueBridge.getIp(), i, mHueBridge);
            hl.setAppDevId(HueBridge.APPID_DEV_HUE_LIGHT);
            mLights.add(hl);
        }
        refreshUI();
    }
    public void onEventMainThread(HueBridgeConnectedEvent evt) {
        tip("Hue bridge connected");
        Log.d(TAG, "Hue Bridge connected.");
        HueManager.getInstance().dump();
        if (dlgSetHueGateway != null) {
            dlgSetHueGateway.dismiss();
            dlgSetHueGateway = null;
        }
        PHHueSDK phHueSDK = PHHueSDK.getInstance();
        PHBridge bridge = phHueSDK.getSelectedBridge();
        if (bridge == null)
            return;

        mHueBridge = getValidHueBridge();
        boolean isNew = (mHueBridge == null);
        if (isNew) {
            Log.e(TAG, "Connected but list Hue bridge is empty");
        }
        if (evt.getUsername() != null && (!evt.getUsername().isEmpty())) {
        	mHueBridge.setUsername(evt.getUsername());
            HueSharedPreferences prefs = HueSharedPreferences.getInstance(LightHueListAct.this);
            prefs.setUsername(evt.getUsername());
            Log.d(TAG, "BridgeConnect user " + evt.getUsername());
        }
        List<PHLight> allLights = bridge.getResourceCache().getAllLights();
        List<PHGroup> allGroups = bridge.getResourceCache().getAllGroups();
//        Random rand = new Random();
        List<String> lightIdentifiers = new ArrayList<String>();
        mLights = new ArrayList<HueLight>();
        for (PHLight light : allLights) {
            int ep = 0;
            try {
                ep = Integer.parseInt(light.getIdentifier());
                HueLight hl = new HueLight(mHueBridge.getIp(), ep, mHueBridge);
                hl.setAppDevId(HueBridge.APPID_DEV_HUE_LIGHT);
                mLights.add(hl);
            } catch (NumberFormatException nfe) {
                ep = 0;
            }
            lightIdentifiers.add(light.getIdentifier());
            Log.d(TAG, "Add Light id " + light.getIdentifier());
            // Log.d(TAG, " uniid " + light.getUniqueId());
            // Log.d(TAG, " light type " + light.getLightType().toString());
            // Log.d(TAG, " manu name " + light.getManufacturerName());
            // Log.d(TAG, " model num " + light.getModelNumber());
            // Log.d(TAG, " name " + light.getName());
            // Log.d(TAG, " ver " + light.getVersionNumber());
            // PHLightState lightState = new PHLightState();
            // lightState.setHue(rand.nextInt(65535));
            // To validate your lightstate is valid (before sending to the
            // bridge) you can use:
            // String validState = lightState.validateState();
            // bridge.updateLightState(light, lightState, mHueLightListener);
        }
        for (PHGroup group : allGroups) {
            int ep = 0;
            try {
                ep = Integer.parseInt(group.getIdentifier());
                HueLight hl = new HueLight(mHueBridge.getIp(), ep, mHueBridge);
                hl.setAppDevId(HueBridge.APPID_DEV_HUE_GROUP);
                mLights.add(hl);
            } catch (NumberFormatException nfe) {
                ep = 0;
            }
            Log.d(TAG, "Add Group id " + group.getIdentifier());
        }

        refreshUI();
    }

    public void onEventMainThread(HueAuthRequiredEvent evt) {
        if (dlgSetHueGateway == null) {
            dlgSetHueGateway = DialogUtils.createCustomDialog(mContext, R.layout.dialog_set_hue_gateway, false);
            ImageView ivClose = (ImageView) dlgSetHueGateway.findViewById(R.id.iv_close);
            WindowManager.LayoutParams lp = dlgSetHueGateway.getWindow().getAttributes();
            lp.width = WindowUtil.getScreenWidth(mContext); // 设置宽度
            lp.height = (int) (WindowUtil.getScreenHeight(mContext) * 4 / 7); // 设置高度
            dlgSetHueGateway.getWindow().setAttributes(lp);

            ivClose.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    dlgSetHueGateway.dismiss();
                    dlgSetHueGateway = null;
                }
            });

            dlgSetHueGateway.show();
        }
    }

    public void onEventMainThread(DevListUpdateEvent evt) {
        Log.d(TAG, "Device updated.");
        if (mLightsAdapter != null) {
            mLightsAdapter.notifyDataSetInvalidated();
        }
    }

    private void click(HueLight light, int position) {
        boolean isOn = (light.getStatus() == 1);
        HueManager.getInstance().setLightOn((HueLight) light, !isOn);
    }

    private void longClick(HueLight light, int position) {
        Intent intent = new Intent(mContext, LightHueAct.class);
        intent.putExtra("light", light);
        intent.putExtra("isSimu", (mHueBridge.getIp().equals("127.0.0.1")));
        startActivity(intent);
    }

}
