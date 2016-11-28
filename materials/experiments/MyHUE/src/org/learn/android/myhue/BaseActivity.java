package org.learn.android.myhue;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

/**
 * 快速开发的activity基础框架类，提供了getView来简化findViewById,并且提供了构建框架.
 */
public abstract class BaseActivity extends Activity {
	protected String TAG = getClass().getSimpleName();
	protected Context mContext;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		mContext = this;
		// AppManager.getAppManager().addActivity(this);
		beforeSetContentView();
		if (getContentViewId() != 0) {
			setContentView(getContentViewId());
		}
		findViews();
		initData();
		setListener();
	}
	
	/**
	 * 在setContentView之前触发的方法
	 */
	protected void beforeSetContentView() {

	}

	/**
	 * 如果没有布局，那么就返回0
	 * 
	 * @return activity的布局文件
	 */
	protected abstract int getContentViewId();

	/**
	 * 找到所有的views
	 */
	protected abstract void findViews();

	/**
	 * 在这里初始化设置view的各种资源，比如适配器或各种变量
	 */
	protected abstract void initData();

	/**
	 * 设置所有的监听事件
	 */
	protected abstract void setListener();

	/**
	 * 通过泛型来简化findViewById
	 */
	@SuppressWarnings("unchecked")
    protected final <E extends View> E getView(int id) {
		try {
			return (E) findViewById(id);
		} catch (ClassCastException ex) {
			Log.e(TAG, "Could not cast View to concrete class.", ex);
			throw ex;
		}
	}

	// @Override
	// protected void onDestroy() {
	// super.onDestroy();
	// AppManager.getAppManager().finishActivity(this);
	// }

	@Override
	protected void onStop() {
		super.onStop();
//		if(isTrimMemory){
//			onTrimMemory(TRIM_MEMORY_UI_HIDDEN);
//		}
	}
	
	@Override
	public void onTrimMemory(int level) {
		Log.d(TAG, "cc---"+level);
		switch (level) {
			case TRIM_MEMORY_COMPLETE:
				Log.d(TAG, "cc---TRIM_MEMORY_COMPLETE");
				break;

			case TRIM_MEMORY_MODERATE:
				Log.d(TAG, "cc---TRIM_MEMORY_MODERATE");
				break;

			case TRIM_MEMORY_BACKGROUND:
				Log.d(TAG, "cc---TRIM_MEMORY_BACKGROUND");
				break;
		}
		super.onTrimMemory(level);
	}
	
	@Override
	public void onLowMemory() {
		Log.d(TAG, "cc---onLowMemory");
		super.onLowMemory();
	}
}
