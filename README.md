# ActivitiesMonitor
android library for montoring activities on device

required permissions in AndroidManifest.xml:

```xml
  <uses-permission android:name="android.permission.GET_TASKS" />
  <uses-permission android:name="android.permission.WAKE_LOCK" />
  <uses-permission xmlns:tools="http://schemas.android.com/tools"
    android:name="android.permission.PACKAGE_USAGE_STATS"
    tools:ignore="ProtectedPermissions" />
```

1. extend ActivityWatcherService and implements onActivityShown and onActivityPaused
2. describe your service class in AndroidManifest.xml
3. start service somewere (from main activiy, or use broadcastreceiver for USER_PRESENT, for example)

that's all

note - for LOLLIPOP (API LEVEL >= 20) works only if user allow collect usage stats for application 
 (Settings->Security->Apps with usage access) 


