package org.learn.android.myhue.utils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.util.Log;

public class CommonUtils {

	/**
	 * 获取当前应用的信息
	 */
	public static PackageInfo getPackageInfo(Context context) {
		PackageInfo info = null;
		try {
			PackageManager packageManager = context.getPackageManager();
			// getPackageName()是当前类的包名，0代表是获取版本信息
			info = packageManager.getPackageInfo(context.getPackageName(), 0);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return info;
	}


	public static double keepThreePlacesOfDecimal(double number) {
		BigDecimal b = new BigDecimal(number);
		return b.setScale(3, BigDecimal.ROUND_HALF_UP).doubleValue();
	}
	/**
	 * 判断程序是否在后台
	 * @param context
	 * @return
	 */
	public static boolean isBackground(Context context) {
		ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
		List<RunningAppProcessInfo> appProcesses = activityManager.getRunningAppProcesses();
		for (RunningAppProcessInfo appProcess : appProcesses) {
			if (appProcess.processName.equals(context.getPackageName())) {
				if (appProcess.importance == RunningAppProcessInfo.IMPORTANCE_BACKGROUND) {
					Log.i("Background", "wwwwwwwwwww-->" + appProcess.processName);
					return true;
				} else {
					Log.i("Foreground", "wwwwwwwwwww-->" + appProcess.processName);
					return false;
				}
			}
		}
		return false;
	}

	static Locale mCurLocale;
	static Resources getResourcesByLocale( Resources res, String localeName )
	{	    
		Configuration conf = new Configuration(res.getConfiguration());	
		conf.locale = new Locale(localeName);	
		return new Resources(res.getAssets(), res.getDisplayMetrics(), conf);
	}		

	static private void resetLocale(Resources res)
	{		
		Configuration conf = new Configuration(res.getConfiguration());
		conf.locale = mCurLocale;
		new Resources(res.getAssets(), res.getDisplayMetrics(), conf);
	}

}
