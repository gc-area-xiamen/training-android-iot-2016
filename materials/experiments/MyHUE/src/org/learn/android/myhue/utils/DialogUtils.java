package org.learn.android.myhue.utils;

import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import org.learn.android.myhue.R;

/**
 * 对话框工具类
 */
public class DialogUtils {
	public interface OnDialogClickListener{
		public void onClickConfirmBtnListener();
		public void onClickCancelBtnListener();
	}

	/**
	 * 创建一个ProgressDialog
	 * @param context
	 * @param title
	 * @param message
	 * @param cancelable
	 * @return
	 */
	public static ProgressDialog createProgressDialog(Context context, String title, String message, boolean cancelable){
		
		ProgressDialog pd = new ProgressDialog(context);
		
		if(!TextUtils.isEmpty(title)){
			pd.setTitle(title);
		}
		
		if(!TextUtils.isEmpty(message)){
			pd.setMessage(message);
		}
		
		pd.setCancelable(cancelable);
		return pd;
	}
	
	public static ProgressDialog createHorizontalProgressDialog(Context context, String title, String message, int max, boolean cancelable){
		
		ProgressDialog pd = new ProgressDialog(context);
		
		if(!TextUtils.isEmpty(title)){
			pd.setTitle(title);
		}
		
		if(!TextUtils.isEmpty(message)){
			pd.setMessage(message);
		}
		
		pd.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		pd.setMax(max);
		pd.setCancelable(cancelable);
		return pd;
	}
	
	/**
	 * 创建一个AlertDialog
	 * @param context
	 * @param title		标题，若不想要标题则传入null或者""
	 * @param message	内容，若没有则传入null或者""
	 * @param iconId  	图标id,若没有图标则传入-1
	 * @param cancelable true:按回退键可取消dialog  false:按回退键无法取消dialo
	 * @return
	 */
	public static Builder createAlertDailog(Context context, String title, String message, int iconId, boolean cancelable){
		Builder builder = new Builder(context);
		
		if(!TextUtils.isEmpty(title)){
			builder.setTitle(title);
		}
		
		if(!TextUtils.isEmpty(message)){
			builder.setMessage(message);
		}
		
		if(iconId != 1) {
			builder.setIcon(iconId);
		}
		
		builder.setCancelable(cancelable);
		return builder;
	}
	
	/**
	 * 创建一个AlertDialog
	 * @param context
	 * @param titleId    若没有则传入-1
	 * @param messageId  若没有则传入-1
	 * @param iconId  	  图标id,若没有图标则传入-1
	 * @param cancelable true:按回退键可取消dialog  false:按回退键无法取消dialo
	 * @return
	 */
	public static Builder createAlertDailog(Context context, int titleId, int messageId, int iconId, boolean cancelable){
		Builder builder = new Builder(context);
		
		if(titleId != -1){
			builder.setTitle(titleId);
		}
		
		if(messageId != -1){
			builder.setMessage(messageId);
		}
		
		if(iconId != 1) {
			builder.setIcon(iconId);
		}
		
		builder.setCancelable(cancelable);
		return builder;
	}
	
	/**
	 * 创建自定义的dialog
	 * @param context
	 * @param layoutId
	 * @param cancelable
	 * @return
	 */
	public static Dialog createCustomDialog(Context context, int layoutId, boolean cancelable){
		Dialog dialog = new Dialog(context);
		dialog.setCancelable(cancelable);
		dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		dialog.setContentView(layoutId);
		
		return dialog;
	}
	
	public static Dialog createCustomDialog(Context context, int layoutId, boolean cancelable, int style){
		Dialog dialog = new Dialog(context, style);
		dialog.setCancelable(cancelable);
		dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		dialog.setContentView(layoutId);
		
		return dialog;
	}
	
	/**
	 * 创建简单的自定义dialog
	 * @param context
	 * @param cancelable
	 * @return
	 */
	public static Dialog createCustomSimpleDialog(Context context, String content, boolean cancelable, final OnDialogClickListener listener){
		final Dialog dialog = new Dialog(context);
		dialog.setCancelable(cancelable);
//		View view = LayoutInflater.from(context).inflate(layoutId, null);
//		builder.setView(view);
		dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		dialog.setContentView(R.layout.dialog_simple);
		
		TextView tvContent = (TextView) dialog.findViewById(R.id.tv_content);
		Button btnConfirm = (Button) dialog.findViewById(R.id.btn_confirm);
		Button btnCancle = (Button) dialog.findViewById(R.id.btn_cancel);
		tvContent.setText(content);
		btnConfirm.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View view) {
				if(listener != null) {
					listener.onClickConfirmBtnListener();
				}
				dialog.dismiss();
			}
		});
		
		btnCancle.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				if(listener != null) {
					listener.onClickCancelBtnListener();
				}
				dialog.dismiss();
			}
		});
		
		return dialog;
	}
	
	public static Dialog createCustomSimpleDialog(Context context, String title, int imgResId, String content, boolean cancelable, final OnDialogClickListener listener){
		Dialog dialog = createCustomSimpleDialog(context, content, cancelable, listener);
		TextView tvtitle = (TextView) dialog.findViewById(R.id.tv_title);
		ImageView ivTitle = (ImageView) dialog.findViewById(R.id.iv_title);
		tvtitle.setText(title);
		if(imgResId != -1) {
			ivTitle.setVisibility(View.VISIBLE);
			ivTitle.setImageResource(imgResId);
		}
		return dialog;
	}
	
	/**
	 * 创建一个toast风格的dialog
	 * @param context
	 * @param cancelable
	 * @return
	 */
//	public static Dialog createToastByDialog(Context context, String content){
//		Dialog dialog = new Dialog(context, R.style.DialogFloatNotTitle);
//		dialog.setCancelable(false);
//		dialog.setContentView(R.layout.dialog_toast_tv);
//		
//		WindowManager.LayoutParams lp = dialog.getWindow().getAttributes();
//		lp.width = (int) (WindowUtil.getScreenWidth(context) * 3 / 4); // 设置宽度
//		lp.height = (int) (WindowUtil.getScreenWidth(context) / 2); // 设置高度
//		dialog.getWindow().setAttributes(lp);
//		
//		return dialog;
//	}
//	
//	public static Dialog createToastByDialog(Context context, int contentId){
//		String content = context.getString(contentId);
//		return createToastByDialog(context, content);
//	}

}

