package ustc.ssqstone.xueba;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import com.renn.rennsdk.RennClient;
import com.renn.rennsdk.RennResponse;
import com.renn.rennsdk.RennExecutor.CallBack;
import com.renn.rennsdk.exception.RennException;
import com.renn.rennsdk.param.AccessControl;
import com.renn.rennsdk.param.PutBlogParam;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.Application;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.Vibrator;
import android.provider.Settings;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

/**
 * 方便服务和活动处理的应用类
 * 
 * @author ssqstone
 */
public class XueBaYH extends Application
{
	protected static final String	SMS_STRING					= "ustc.ssqstone.xueba.SMS_String";
	protected static final String	SMS_NO						= "ustc.ssqstone.xueba.SMS_No";
	protected static final String	SMS_PHONE_NO				= "ustc.ssqstone.xueba.SMS_PhoneNo";
	protected static final String	PENDING_SMSs				= "pending_SMSs";
	protected static final String	LAST_WRITE					= "last_write";
	protected static final String	SHUTDOWN_TIME				= "shutdown_time";
	protected static final String	RESTRICTED_MODE				= "ustc.ssqstone.xueba.restricted_mode";
	protected static final String	START_TIME					= "ustc.ssqstone.xueba.start_time";
	protected static final boolean	myself						= true;
	protected static final boolean	debug						= false;
	protected static final boolean	debugSMS					= true;
	
	protected static final String	STATE						= "state";
	protected static final String	ACK_INTERRUTION				= "ack_interrution";
	protected static final String	INTERRUPTED_TIMES			= "interrupted times";
	protected static final String	HOW_MANY_INTERRUPTED_TIMES	= "how_many_interrupted_times";
	protected static final long		我							= 15556958998l;
	protected static final String	我s							= Long.valueOf(我).toString();
	protected static final long		我的监督人						= debug ? 10010 : 18297958221l;
	protected static final String	我的监督人s						= Long.valueOf(我的监督人).toString();
	protected static final String	默认监督人						= Long.valueOf(myself ? 我的监督人 : 我).toString();
	protected static final String	INFORM_NOT_SAVED			= "本次输入未保存";
	protected static final String	INFORM_SAVING_ERROR			= "本次输入有错误而不能保存, 再次按下返回键退出而不保存. ";
	protected static final String	PHONE_NUM					= "phone_num";
	protected static final String	NOW_EDITTING				= "nowEditting";
	protected static final String	CONFIRM_PHONE				= "confirmPhone";
	protected static final String	BACK_PRESSED				= "backPressed";
	protected static final String	INFORM_NOT_SAVING			= "注意, 直接退出时, 本次数据不被保存. ";
	protected static final String	INFORM_OFF					= "您的监督人身份刚刚被我撤销，请注意。";
	protected static final String	INFORM_ON					= "我已经设定您为学习监督短信的接收人。若您不认识我，请与我联系并要求我更改设置。\n如果您多次收到本短信，说明我曾更改程序数据。这是不好的。 ";
	protected static final String	KEY							= "key";
	protected static final String	INFORM_WON_T_SAVE			= "输入有误, 不能保存";
	protected static final String	INFORM_SAVED				= "已成功保存";
	// protected static final String DENIED = "denied";
	protected static final String	PARITY						= "parity";
	protected static final String	STUDY_DENIED				= "study_denied";
	protected static final String	NOON_DENIED					= "noon_denied";
	protected static final String	NIGHT_DENIED				= "night_denied";
	protected static final String	STUDY_END					= "study_end";
	protected static final String	VALUES						= "values";
	protected static final String	NOON_END					= "noon_end";
	protected static final String	NIGHT_END					= "night_end";
	protected static final String	NOON_BEGIN					= "noon_begin";
	protected static final String	NIGHT_BEGIN					= "night_begin";
	protected static final String	STUDY_BEGIN					= "study_begin";
	protected static final String	STUDY_EN					= "study_en";
	protected static final String	NIGHT_EN					= "night_en";
	protected static final String	NOON_EN						= "noon_en";
	private static final String		SMS_LOG						= "sms_log";
	protected static final String	DESTROY_RESTRICTION			= "ustc.ssqstone.xueba.destroy";
	protected final static String	SMS_SENT_S					= "ustc.ssqstone.xueba.SMS_Sent";
	
	protected static XueBaYH		ApplicationContext;
	// protected static boolean confirmPhone;
	
	static final String				APP_ID						= "168802";
	static final String				API_KEY						= "e884884ac90c4182a426444db12915bf";
	static final String				SECRET_KEY					= "094de55dc157411e8a5435c6a7c134c5";
	protected static final String	PENGDING_LOGS				= "pending_log";
	
	private BroadcastReceiver		shutdownBroadcastReceiver;
	private BroadcastReceiver		airReceiver					= new BroadcastReceiver()
																{
																	@Override
																	public void onReceive(Context context, Intent intent)
																	{
																		Bundle bundle = intent.getExtras();
																		if (bundle != null)
																		{
																			switch (bundle.getInt("state"))
																			{
																				case 0: // 飞行模式已关闭
																					SharedPreferences sharedPreferences = getSharedPreferences(VALUES, MODE_PRIVATE);
																					String pendingString = sharedPreferences.getString(PENDING_SMSs, "");
																					if (pendingString.length() > 0)
																					{
																						Editor editor = sharedPreferences.edit();
																						String[] strings = pendingString.split(";;");
																						for (int i = 0; i < strings.length; i++)
																						{
																							String string = strings[i];
																							if (string.contains("to"))
																							{
																								sendSMS(string.substring(string.indexOf("content: ") + 8), string.substring(4, 15), string.contains("; mode: ")?string.substring(string.indexOf("; mode: ")+"; mode: ".length(), string.indexOf(';', string.indexOf("mode: "))):null);
																							}
																							String bufferString = "";
																							for (int j = i + 1; j < strings.length; j++)
																							{
																								String string_ = strings[j];
																								bufferString += string_;
																							}
																							editor.putString(PENDING_SMSs, bufferString);
																							editor.commit();
																							editor.putLong(PARITY, getParity());
																							editor.commit();
																						}
																					}
																					break;
																				case 1: // 飞行模式正在关闭
																					break;
																				case 3: // 飞行模式已开启
																					break;
																			}
																		}
																	}
																};
	protected RennClient			rennClient;
	private SMS_SentReceiver		smsSentReceiver;
	
	public void onCreate()
	{
		super.onCreate();
		ApplicationContext = this;
		
		shutdownBroadcastReceiver = new BroadcastReceiver()
		{
			@Override
			public void onReceive(Context context, Intent intent)
			{
				onShutdown();
			}
		};
		
		IntentFilter intentFilter;
		intentFilter = new IntentFilter();
		intentFilter.addAction(Intent.ACTION_SHUTDOWN);
		registerReceiver(shutdownBroadcastReceiver, intentFilter);
		
		/* 自定义IntentFilter为SENT_SMS_ACTIOIN Receiver */
		intentFilter = new IntentFilter(SMS_SENT_S);
		smsSentReceiver = new SMS_SentReceiver();
		registerReceiver(smsSentReceiver, intentFilter);
		
		intentFilter = new IntentFilter("android.intent.action.SERVICE_STATE");
		registerReceiver(airReceiver, intentFilter);
		// confirmPhone= false;
	}
	
	@Override
	public void onTerminate()
	{
		if (shutdownBroadcastReceiver != null)
		{
			unregisterReceiver(shutdownBroadcastReceiver);
		}
		
		if (smsSentReceiver != null)
		{
			unregisterReceiver(smsSentReceiver);
		}
		super.onTerminate();
	}
	
	protected static XueBaYH getApp()
	{
		return ApplicationContext;
	}
	
	protected void restartMonitorService()
	{
		// stopService(new Intent("ustc.ssqstone.xueba.MonitorService"));
		// //在Service退出的时候加入短信通知, 所以不能在此关闭. 其实关闭Service没啥意思.
		startService(new Intent("ustc.ssqstone.xueba.MonitorService"));
	}
	
	public class SMS_SentReceiver extends BroadcastReceiver
	{
		@Override
		public void onReceive(Context context, Intent intent)
		{
			try
			{
				Editor editor;
				Bundle bundle = intent.getExtras();
				switch (getResultCode())
				{
					case Activity.RESULT_OK:
						if (bundle.containsKey(DESTROY_RESTRICTION))
						{
							destoryRestrictedActivity(bundle.getString(DESTROY_RESTRICTION));
						}
						XueBaYH.getApp().showToast("已向" + bundle.getString(SMS_PHONE_NO) + "发送短信:\n" + bundle.getString(SMS_STRING));
						editor = getSharedPreferences(XueBaYH.SMS_LOG, MODE_PRIVATE).edit();
						editor.putString(getSimpleTime(Calendar.getInstance().getTimeInMillis()), "to: " + bundle.getString(SMS_PHONE_NO) + "; content: " + bundle.getString(SMS_STRING) + "; No." + bundle.getInt(SMS_NO));
						editor.commit();
						editor.putLong(PARITY, getParity());
						editor.commit();
						break;
					case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
					case SmsManager.RESULT_ERROR_RADIO_OFF:
					case SmsManager.RESULT_ERROR_NULL_PDU:
					default:
						/* 发送短信失败 */
						XueBaYH.getApp().showToast("向" + bundle.getString(SMS_PHONE_NO) + "发送短信:\n" + bundle.getString(SMS_STRING) + "失败, 已经记档, 在有网的时候自动发送. ");
						SharedPreferences sharedPreferences = getSharedPreferences(XueBaYH.VALUES, MODE_PRIVATE);
						editor = sharedPreferences.edit();
						String pendingSMS = sharedPreferences.getString(PENDING_SMSs, "");
						pendingSMS += "to: " + bundle.getString(SMS_PHONE_NO) + (bundle.containsKey(DESTROY_RESTRICTION)?"; mode: "+bundle.getString(DESTROY_RESTRICTION):"") +"; content: " + bundle.getString(SMS_STRING) + ";;";
						editor.putString(PENDING_SMSs, pendingSMS);
						editor.commit();
						editor.putLong(PARITY, getParity());
						editor.commit();
						break;
				}
			}
			catch (Exception e)
			{
				e.getStackTrace();
			}
		}
	}
	
	protected void sendSMS(String smsString, String phoneText, String mode)
	{
		if (!debug || debugSMS)
		{
			// setOnLine();
			SmsManager sms = SmsManager.getDefault();
			List<String> texts = sms.divideMessage(smsString);
			
			int i = 0;
			for (String text : texts)
			{
				Intent itSend = new Intent(SMS_SENT_S);
				Bundle bundle = new Bundle();
				bundle.putString(SMS_PHONE_NO, phoneText);
				bundle.putInt(SMS_NO, ++i);
				bundle.putString(SMS_STRING, text);
				if (i == 1 && (mode != null) && (!mode.isEmpty()))
				{
					bundle.putString(DESTROY_RESTRICTION, mode);
				}
				itSend.putExtras(bundle);
				PendingIntent mSendPI = PendingIntent.getBroadcast(getApplicationContext(), (int) System.currentTimeMillis(), itSend, PendingIntent.FLAG_UPDATE_CURRENT);
				
				sms.sendTextMessage(phoneText, null, text, mSendPI, null);
			}
		}
		else
		{
			showToast("此处向" + phoneText + "发送短信:\n" + smsString);
		}
	}
	
	protected static String getSimpleTime(long time)
	{
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(time);
		SimpleDateFormat simpleFormat = new SimpleDateFormat("yyyy.MM.dd hh:mm:ss");
		return simpleFormat.format(calendar.getTime());
	}
	
	protected static String getSimpleDate(long time)
	{
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(time);
		SimpleDateFormat simpleFormat = new SimpleDateFormat("yyyy.MM.dd");
		return simpleFormat.format(calendar.getTime());
	}
	
	protected void showToast(String string)
	{
		// if (toast == null)
		// {
		// toast = Toast.makeText(this, string, Toast.LENGTH_LONG);
		// toast.show();
		// }
		// else
		// {
		// toast.cancel();
		// toast = null;
		// }
		Toast.makeText(this, string, Toast.LENGTH_LONG).show();
	}
	
	// protected void showShortToast(String string)
	// {
	// Toast.makeText(this, string, Toast.LENGTH_SHORT).show();
	// }
	
	protected boolean isInAirplaneMode()
	{
		return android.provider.Settings.System.getInt(this.getContentResolver(), android.provider.Settings.System.AIRPLANE_MODE_ON, 0) == 1;
	}
	
	/**
	 * 随便弄一个加密，防止手动改。
	 * 
	 * @return 校验码
	 */
	
	protected long getDeniedParity()
	{
		SharedPreferences sharedPreferences = getSharedPreferences("denied", MODE_PRIVATE);
		long key = sharedPreferences.getLong("key", XueBaYH.myself ? 18297958221l : 15556958998l);
		
		double result = key * (sharedPreferences.getBoolean("night_denied", false) ? 1.1 : 1.2) * (sharedPreferences.getBoolean("noon_denied", false) ? 1.1 : 1.2) * (sharedPreferences.getBoolean("study_denied", false) ? 1.1 : 1.2);
		
		return (long) (result / 20130906);
	}
	
	/**
	 * 防止存档数据篡改. 防止数据篡改的方法是: 在文件中加入校验域. 数据校验通过后才能作为有效数据被读取.
	 * 防止数据篡改应该在保存数据时写入当前parity, 在读取数据时检查parity. 随便定一个校验规则. 只要将重要信息都校验过就好.
	 * 
	 * *注意, 仅在保存数据时checkData成功后使用.
	 * 
	 * @return 校验码
	 */
	protected long getParity()
	{
		SharedPreferences sharedPreferences = getSharedPreferences(VALUES, MODE_PRIVATE);
		
		double result = (sharedPreferences.getBoolean(STUDY_EN, false) ? 1 : 0)
				* 3464
				+ (sharedPreferences.getBoolean(NOON_EN, false) ? 1 : 0)
				* 342453
				+ (sharedPreferences.getBoolean(NIGHT_EN, false) ? 1 : 0)
				* 454325
				+ Math.log((double) sharedPreferences.getLong(STUDY_BEGIN, 0) * 3414 + Math.log((double) sharedPreferences.getLong(NIGHT_BEGIN, 0)) * 45134 + Math.log((double) sharedPreferences.getLong(NOON_BEGIN, 0)) * 123412341 + Math.log((double) sharedPreferences.getLong(NIGHT_END, 0)) * 124 + Math.log((double) sharedPreferences.getLong(NOON_END, 0)) * 14314 + Math.log((double) Long.valueOf(sharedPreferences.getString(PHONE_NUM, myself ? 我s : 我的监督人s))) * 14314 + Math.log((double) Long.valueOf(sharedPreferences.getLong(SHUTDOWN_TIME, 0))) * 143 + Math.log((double) Long.valueOf(sharedPreferences.getLong(LAST_WRITE, 0))) * 143 + sharedPreferences.getString(PENDING_SMSs, "").hashCode() * sharedPreferences.getString(PENDING_SMSs, "").length()
						+ sharedPreferences.getString(PENGDING_LOGS, "").hashCode() * sharedPreferences.getString(PENGDING_LOGS, "").length());
		return (long) result;
	}
	
	private void setOffLine()
	{
		boolean airplaneModeOn = XueBaYH.getApp().isInAirplaneMode();
		if (!airplaneModeOn)
		{
			if (!android.provider.Settings.System.putString(this.getContentResolver(), android.provider.Settings.System.AIRPLANE_MODE_ON, "1"))
			{
				XueBaYH.getApp().showToast("自动打开飞行模式失败，请手动打开飞行模式。");
			}
			else
			{
				Intent intent = new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED);
				intent.putExtra(STATE, airplaneModeOn);
				sendBroadcast(intent);
			}
		}
	}
	
	private boolean setOnLine()
	{
		boolean airplaneModeOn = XueBaYH.getApp().isInAirplaneMode();
		if (airplaneModeOn)
		{
			if (!android.provider.Settings.System.putString(this.getContentResolver(), android.provider.Settings.System.AIRPLANE_MODE_ON, "0"))
			{
				XueBaYH.getApp().showToast("自动关闭飞行模式失败，请手动关闭飞行模式。");
				return false;
			}
			else
			{
				Intent intent = new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED);
				intent.putExtra(STATE, !airplaneModeOn);
				sendBroadcast(intent);
				return true;
			}
		}
		else
		{
			return true;
		}
	}
	
	protected String getPhoneNum()
	{
		TelephonyManager phoneManager = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
		String phsString = phoneManager.getLine1Number();
		return phsString;
	}
	
	protected void vibrateOK()
	{
		Vibrator vibrator = (Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE);
		vibrator.vibrate(new long[] { 0, 400, 400, 400, 400, 200, 200, 200, 200, 400, 400 }, -1);
	}
	
	protected void vibrateOh()
	{
		Vibrator vibrator = (Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE);
		vibrator.vibrate(new long[] { 0, 800, 800, 800, 800, 800 }, -1);
	}
	
	protected void killBackGround()
	{
		ActivityManager am = (ActivityManager) this.getSystemService(ACTIVITY_SERVICE);
		List<ActivityManager.RunningAppProcessInfo> appList = am.getRunningAppProcesses();
		
		for (Iterator<ActivityManager.RunningAppProcessInfo> iterator = appList.iterator(); iterator.hasNext();)
		{
			RunningAppProcessInfo runningAppProcessInfo = (RunningAppProcessInfo) iterator.next();
			if (runningAppProcessInfo.importance > RunningAppProcessInfo.IMPORTANCE_SERVICE)
			{
				for (String pkg : runningAppProcessInfo.pkgList)
					am.killBackgroundProcesses(pkg);
			}
		}
		
		showToast("已清理后台应用. ");
	}
	
	public RennClient iniRennClient(Context context)
	{
		rennClient = RennClient.getInstance(context);
		rennClient.init(XueBaYH.APP_ID, XueBaYH.API_KEY, XueBaYH.SECRET_KEY);
		rennClient.setScope("read_user_blog read_user_photo read_user_status read_user_album " + "read_user_comment read_user_share publish_blog publish_share " + "send_notification photo_upload status_update create_album " + "publish_comment publish_feed");
		rennClient.setTokenType("bearer");
		return rennClient;
	}
	
	public void onShutdown()
	{
		Calendar calendar = Calendar.getInstance();
		SharedPreferences sharedPreferences = getSharedPreferences(VALUES, MODE_PRIVATE);
		Editor editor = sharedPreferences.edit();
		editor.putLong(SHUTDOWN_TIME, calendar.getTimeInMillis());
		editor.commit();
		editor.putLong(PARITY, getParity());
		editor.commit();
	}
	
	protected void destoryRestrictedActivity(String mode)
	{
		Intent intent = new Intent(XueBaYH.this, RestrictedModeActivity.class);
		intent.putExtra(DESTROY_RESTRICTION, true);
		intent.putExtra(RESTRICTED_MODE, mode);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivity(intent);
	}
	
	protected boolean checkParity()
	{
		boolean result = (XueBaYH.getApp().getParity() == getSharedPreferences(XueBaYH.VALUES, MODE_PRIVATE).getLong(XueBaYH.PARITY, -13));
		if (!result)
		{
			Editor valuesEditor = getSharedPreferences(VALUES, MODE_PRIVATE).edit();
			valuesEditor.commit();
			valuesEditor.putLong(PARITY, getApp().getParity());
			valuesEditor.commit();
			
			punish();
			showToast("上次保存的时候是正常的数据, 现在却坏了. 已经给设定的手机发送短信, 以儆效尤. ");
		}
		return result;
	}
	
	protected void punish()
	{
		sendSMS("我已经设定您为学习监督短信的接收人。若您不认识我，请与我联系并要求我更改设置。\n如果您多次收到本短信，说明我曾经更改程序数据。这是不好的。", getSharedPreferences(VALUES, MODE_PRIVATE).getString(PHONE_NUM, 默认监督人), null);
	}
	
	/**
	 * 检查是否强制退出过, 和是否跳过任务.
	 * 
	 * 先根据lastWrite和shutdown的关系判断是否强退过. 如果没有, 那么取消所有结束时间在现在之前的任务. 如果强退过, 记录强退信息,
	 * 也记录所有跳过的任务.
	 */
	protected void checkStatus()
	{
		SharedPreferences sharedPreferences = getSharedPreferences(VALUES, MODE_PRIVATE);
		Editor editor = sharedPreferences.edit();
		Calendar calendar = Calendar.getInstance();
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MM月dd日hh时mm分");
		
		if (sharedPreferences.getLong(LAST_WRITE, 0) > sharedPreferences.getLong(SHUTDOWN_TIME, 0))
		{
			// 强制退出过.
			String string = "我于" + simpleDateFormat.format(new Date(sharedPreferences.getLong(LAST_WRITE, 0))) + "强制退出了学霸银魂(一个监督不乱鼓捣手机的应用), 直到" + simpleDateFormat.format(calendar.getTime()) + "学霸银魂才得以重新启动. 这是非常不道德的行为, 我强烈谴责自己. \\timeStamp = " + sharedPreferences.getLong(LAST_WRITE, 0) + "\n";
			
			editor.putString(PENGDING_LOGS, sharedPreferences.getString(PENGDING_LOGS, "") + string);
			
			if (sharedPreferences.getLong(NIGHT_END, 0) <= calendar.getTimeInMillis())
			{
				string = "在没有监督的日子里, 我所定的" + "从" + simpleDateFormat.format(sharedPreferences.getLong(NIGHT_BEGIN, 0)) + "到" + simpleDateFormat.format(sharedPreferences.getLong(NIGHT_END, 0)) + "睡觉" + "的计划也没有得到正常的执行, 再口头批评一次! \\timeStamp = " + sharedPreferences.getLong(NIGHT_END, 0) + "\n";
				editor.putString(PENGDING_LOGS, sharedPreferences.getString(PENGDING_LOGS, "") + string);
			}
			if (sharedPreferences.getLong(NOON_END, 0) <= calendar.getTimeInMillis())
			{
				string = "在没有监督的日子里, 我所定的" + "从" + simpleDateFormat.format(sharedPreferences.getLong(NOON_BEGIN, 0)) + "到" + simpleDateFormat.format(sharedPreferences.getLong(NOON_END, 0)) + "睡午觉" + "的计划也没有得到正常的执行, 再口头批评一次! \\timeStamp = " + sharedPreferences.getLong(NOON_END, 0) + "\n";
				editor.putString(PENGDING_LOGS, sharedPreferences.getString(PENGDING_LOGS, "") + string);
			}
			if (sharedPreferences.getLong(STUDY_END, 0) <= calendar.getTimeInMillis())
			{
				string = "在没有监督的日子里, 我所定的" + "从" + simpleDateFormat.format(sharedPreferences.getLong(STUDY_BEGIN, 0)) + "到" + simpleDateFormat.format(sharedPreferences.getLong(STUDY_END, 0)) + "学习" + "的计划也没有得到正常的执行, 再口头批评一次! \\timeStamp = " + sharedPreferences.getLong(STUDY_END, 0) + "\n";
				editor.putString(PENGDING_LOGS, sharedPreferences.getString(PENGDING_LOGS, "") + string);
			}
		}
		
		if (sharedPreferences.getLong(NIGHT_END, 0) <= calendar.getTimeInMillis())
		{
			editor.putBoolean(NIGHT_EN, false);
		}
		if (sharedPreferences.getLong(NOON_END, 0) <= calendar.getTimeInMillis())
		{
			editor.putBoolean(NOON_EN, false);
		}
		if (sharedPreferences.getLong(STUDY_END, 0) <= calendar.getTimeInMillis())
		{
			editor.putBoolean(STUDY_EN, false);
		}
		editor.commit();
	}
	
	protected void sendRennLog(final String content)
	{
		PutBlogParam param = new PutBlogParam();
		param.setTitle("我是来谴责自己的@" + Calendar.getInstance().getTimeInMillis());
		param.setContent(content);
		param.setAccessControl(AccessControl.PUBLIC);
		try
		{
			RennClient rennClient = iniRennClient(XueBaYH.this);
			
			rennClient.getRennService().sendAsynRequest(param, new CallBack()
			{
				@Override
				public void onSuccess(RennResponse response)
				{
					XueBaYH.getApp().showToast("我已本着惩前毖后治病救人的精神代你发表了一篇自我谴责的日志. ");
				}
				
				@Override
				public void onFailed(String errorCode, String errorMessage)
				{
					SharedPreferences sharedPreferences = getSharedPreferences(VALUES, MODE_PRIVATE);
					Editor editor = sharedPreferences.edit();
					
					editor.putString(PENGDING_LOGS, content + sharedPreferences.getString(PENGDING_LOGS, ""));
					
					editor.commit();
				}
			});
		}
		catch (RennException e1)
		{
			e1.printStackTrace();
		}
	}
}