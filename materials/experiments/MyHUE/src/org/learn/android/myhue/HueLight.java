package org.learn.android.myhue;

import java.io.Serializable;

public class HueLight implements Serializable {
    private static final long serialVersionUID = 1L;
//	private static final String TAG = "HueLight";
//	private static final long serialVersionUID = -5L;
    private int mAppDevID;
    
    public int getAppDevId() {
        return mAppDevID;
    }

    public void setAppDevId(int appDevID) {
        this.mAppDevID = appDevID;
    }

    private String mIP;
    private int mEndPoint;
    public int getEndPoint() {
        return mEndPoint;
    }
	public HueLight(String inAddr, int endpoint, HueBridge hb) {
		mIP = inAddr;
		mEndPoint = endpoint;
		mHueBridge = hb;
	}

	public boolean isOnline() {
		if (getAppDevId() == HueBridge.APPID_DEV_HUE_LIGHT) {
			mPrevOnline = HueManager.getInstance().isLightOnline(getEndPoint());
//			Log.d(TAG, String.format("Hue light %d online ", getEndPoint()) + mPrevOnline);
		} else {
			mPrevOnline = HueManager.getInstance().isGroupOnline(getEndPoint());
//			Log.d(TAG, String.format("Hue Group %d online ", getEndPoint()) + mPrevOnline);
		}
		return mPrevOnline;
	}

	private boolean mPrevOnline = false;

	public boolean isPrevOnline() {
		return mPrevOnline;
	}

	public byte getStatus() {
		if (getAppDevId() == HueBridge.APPID_DEV_HUE_LIGHT) {
			boolean isOn = HueManager.getInstance().isLightOn(getEndPoint());
			mPrevStatus = (byte) (isOn ? 1 : 0);
		} else {
			boolean state = HueManager.getInstance().isGroupOn(getEndPoint());
			mPrevStatus = (byte) (state ? 1 : 0);
		}
		return mPrevStatus;
	}

	private byte mPrevStatus = 0;

	public byte getPrevStatus() {
		return mPrevStatus;
	}

	public int getIcon() {
	    if (getAppDevId() == HueBridge.APPID_DEV_HUE_LIGHT) {
            if (!isOnline()) {
                return R.drawable.ic_hue_light_unlink;
            }
            return (1 == getStatus()) ? R.drawable.ic_hue_light_bright : R.drawable.ic_hue_light_normal;
        } else {
            if (!isOnline()) {
                return R.drawable.ic_hue_light_group_unlink;
            }
            return (1 == getStatus()) ? R.drawable.ic_hue_light_group_bright : R.drawable.ic_hue_light_group_normal;
        }
	}
	public String getIconName() {
		if (getAppDevId() == HueBridge.APPID_DEV_HUE_LIGHT) {
			if (!isOnline()) {
				return "ic_hue_light_unlink.png";
			}
			return (1 == getStatus()) ? "ic_hue_light_bright.png" : "ic_hue_light_normal.png";
		} else {
			if (!isOnline()) {
				return "ic_hue_light_group_unlink.png";
			}
			return (1 == getStatus()) ? "ic_hue_light_group_bright.png" : "ic_hue_light_group_normal.png";
		}
	}

	private HueBridge mHueBridge;
	public HueBridge getBridge() {
		return mHueBridge;
	}
	public void setBridge(HueBridge hb) {
	    mHueBridge = hb;
	}
}
