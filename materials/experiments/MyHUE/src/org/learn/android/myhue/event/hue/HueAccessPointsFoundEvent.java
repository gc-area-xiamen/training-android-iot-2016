package org.learn.android.myhue.event.hue;

import java.util.List;

import com.philips.lighting.hue.sdk.PHAccessPoint;

public class HueAccessPointsFoundEvent {

	private List<PHAccessPoint> mList;
	public HueAccessPointsFoundEvent(List<PHAccessPoint> in) {
		mList = in;
	}
	
	public List<PHAccessPoint> getData() {
		return mList;
	}

}
