package su.whs.activitymonitor;

import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.app.AppOpsManager;
import android.app.Service;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * ActivityWatcherService - abstract class, creating thread for monitoring
 * android activities
 * 
 * on starts, service starts monitor screen on/off events, so watching are
 * stopped if screen turned off
 * 
 * @author igor n. boulliev
 * 
 *         required permissions: * android.permission.GET_TASKS
 * 
 * WARNING:
 *    with AOSP > 20 - here required user interaction to allow usage stats
 *
 */

public abstract class ActivityWatcherService extends Service {
	private static final String TAG = "ActivityMonitor";
	private Thread mThread = null;
	private BroadcastReceiver mEventsReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (action.equals(Intent.ACTION_SCREEN_OFF)) {
				stopThread();
			} else if (action.equals(Intent.ACTION_SCREEN_ON)) {
				startThread();
			}
		}
	};

	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
		filter.addAction(Intent.ACTION_SCREEN_ON);
		registerReceiver(mEventsReceiver, filter);
		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		if (isScreenOn(pm)) {
			startThread();
		} else if (BuildConfig.DEBUG) {
			Log.d(TAG, "Screen IS OFF");
		}
	}

	@SuppressWarnings("deprecation")
	private boolean isScreenOn(PowerManager pm) {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT_WATCH) {
			return pm.isScreenOn();
		} else
			return isScreenOnKitKat(pm);
	}

	@TargetApi(Build.VERSION_CODES.KITKAT_WATCH)
	private boolean isScreenOnKitKat(PowerManager pm) {
		return pm.isInteractive();
	}

	@Override
	public void onDestroy() {
		if (mThread != null) {
			if (mThread.isAlive())
				mThread.interrupt();
			mThread = null;
		}
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (BuildConfig.DEBUG)
			Log.d(TAG, String.format(
                    "ActivitiesMonitor.onStartCommand(intent, %d, %d)", flags,
                    startId));

		return super.onStartCommand(intent, flags, startId);
	}

	private void startThread() {
		if (Build.VERSION.SDK_INT>20)
			startThreadLollipop1();
		else
			startThreadCompat();
	}

	private void startThreadCompat() {
		stopThread();
		mThread = new Thread(new Runnable() {
			@Override
			public void run() {
				if (BuildConfig.DEBUG)
					Log.d(TAG, "Thread starts");
				onThreadStarts();
				ActivityManager tasks = (ActivityManager) ActivityWatcherService.this
						.getSystemService(Context.ACTIVITY_SERVICE);
				List<ActivityManager.RunningTaskInfo> taskInfo;
				ComponentName componentInfo;
				String activePackageName = getPackageName();
				String activeActivity = "";

				while (true) {
					taskInfo = tasks.getRunningTasks(1);
					componentInfo = taskInfo.get(0).topActivity;

					if (!componentInfo.getPackageName().equals(
							activePackageName)
							|| !activeActivity.equals(componentInfo
									.getShortClassName())) {

						onActivityShown(componentInfo);
						activePackageName = componentInfo.getPackageName();
						activeActivity = componentInfo.getShortClassName();
						if (BuildConfig.DEBUG)
							Log.d(TAG, String.format("component = '%s'",
									componentInfo.flattenToString()));
					}
					try {
						Thread.sleep(300);
					} catch (InterruptedException e) {
						if (BuildConfig.DEBUG)
							Log.d(TAG, "was interrupted " + e);
						break;
					}
				}

			}
		});
		if (!mThread.isAlive())
			mThread.start();
	}

	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	private void startThreadLollipop1() {
		mThread = new Thread(new Runnable() {

			@Override
			public void run() {
				UsageStatsManager usageStatsManager = (UsageStatsManager)getSystemService("usagestats");
				Comparator<UsageStats> recentComp = new RecentUseComparator();
				int TIMINGS = 20;
				long[] timers_array = new long[TIMINGS];
				int logging_counter = 0;
				String activePackageName = "";

				while(true) {
					long ts = System.currentTimeMillis();
					List<UsageStats> usageStats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_BEST, ts-1000, ts);

					if(usageStats != null && usageStats.size()>0 ) {
						Collections.sort(usageStats, recentComp);
						String packageName = usageStats.get(0).getPackageName(); // return usageStats.get(0).getPackageName();
						if (packageName!=null && !activePackageName.equals(packageName)) {
                            if (activePackageName.length()>0) {
                                onActivityPaused(new ComponentName(activePackageName,"null"));
                            }
							onActivityShown(new ComponentName(packageName,"null"));
							activePackageName = packageName;
						}
					}
                    if (BuildConfig.DEBUG) {
					    timers_array[logging_counter] = System.currentTimeMillis() - ts;
					    logging_counter++;

					    if (logging_counter>TIMINGS-1) {
                            long average = 0;
                            for (int i = 0; i < logging_counter; i++)
                                average += timers_array[i];
                            Log.v(TAG, " average timing: " + average / TIMINGS);
                            logging_counter = 0;
                        }
					}

					try {
						Thread.sleep(300);
					} catch (InterruptedException e) {
						Log.e(TAG, "was interrupted " + e);
						break;
					}
				}
			}
		});
		if (!mThread.isAlive())
			mThread.start();
	}

	private void startThreadLollipop2() {
		stopThread();
		mThread = new Thread(new Runnable() {
			@Override
			public void run() {
				ActivityManager tasks = (ActivityManager) ActivityWatcherService.this
						.getSystemService(Context.ACTIVITY_SERVICE);
				Set<String> activePackages = new HashSet<String>();

                Field field = null;
                try {
                    field = ActivityManager.RunningAppProcessInfo.class.getDeclaredField("processState");
                } catch (NoSuchFieldException e) {
                    // e.printStackTrace();
                    // leave field null
                }
                while(true) {


					final List<ActivityManager.RunningAppProcessInfo> processInfos = tasks.getRunningAppProcesses();


					for (ActivityManager.RunningAppProcessInfo processInfo : processInfos) {
						if (processInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND && processInfo.importanceReasonCode == 0) {
                            Integer state = null;
                            try {
                                state = field.getInt( processInfo );
                                if (state != null && state == 2) { // PROCESS_STATE_TOP
                                    onActivityShown(ComponentName.unflattenFromString(processInfo.pkgList[0]));
                                }
                                continue; // sucessfull
                            } catch (IllegalAccessException e) {

                            }


                            for (String s: processInfo.pkgList) {
								if (activePackages.contains(s)) {
                                    // already in activePackagesList
								} else {
									// package s become active
									if (BuildConfig.DEBUG) {
										Log.w(TAG,"package: " + s + " goes foreground");
									}
									activePackages.add(s);
									ComponentName componentName = ComponentName.unflattenFromString(s);
								}
							}
						} else {
							for (String s : processInfo.pkgList) {
								if (activePackages.contains(s)) {
									if (BuildConfig.DEBUG) {
										Log.w(TAG,"package: " + s + " goes background");
									}
									activePackages.remove(s);
                                    onActivityPaused(ComponentName.unflattenFromString(s));
								}
							}
						}
					}
					try {
						Thread.sleep(300);
					} catch (InterruptedException e) {
						if (BuildConfig.DEBUG)
							Log.d(TAG, "was interrupted " + e);
						break;
					}
				}
			}
		});
		if (!mThread.isAlive())
			mThread.start();
	}

	private void stopThread() {
		if (mThread != null) {
			if (mThread.isAlive()) {
				mThread.interrupt();
				mThread = null;
			}
		}
	}

	static class RecentUseComparator implements Comparator<UsageStats> {

		@TargetApi(Build.VERSION_CODES.LOLLIPOP)
		@Override
		public int compare(UsageStats lhs, UsageStats rhs) {
			return (lhs.getLastTimeUsed() > rhs.getLastTimeUsed())?-1:(lhs.getLastTimeUsed() == rhs.getLastTimeUsed())?0:1;
		}
	}

	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	public static boolean needPermissionForBlocking(Context context){
		try {
			PackageManager packageManager = context.getPackageManager();
			ApplicationInfo applicationInfo = packageManager.getApplicationInfo(context.getPackageName(), 0);
			AppOpsManager appOpsManager = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
			int mode = appOpsManager.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, applicationInfo.uid, applicationInfo.packageName);
			return  (mode != AppOpsManager.MODE_ALLOWED);
		} catch (PackageManager.NameNotFoundException e) {
			return true;
		}
	}

	public static void openSettingsForUnblock(Context context) {
		Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
		context.startActivity(intent);
	}

	/**
	 * called on activity changed
	 * 
	 * @param componentName
	 *            - see {@link ComponentName}
	 */

	public abstract void onActivityShown(ComponentName componentName);
	public abstract void onActivityPaused(ComponentName componentName);

	protected void onThreadStarts() {

	}
}
