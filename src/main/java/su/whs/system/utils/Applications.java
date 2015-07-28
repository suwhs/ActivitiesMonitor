package su.whs.system.utils;

import java.util.ArrayList;
import java.util.List;

import su.whs.system.models.ApplicationModel;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

public class Applications {
	public static List<ApplicationModel> getInstalledApplications(Context context) {
		PackageManager pm = context.getPackageManager();
		Intent intent = new Intent(Intent.ACTION_MAIN, null);
		intent.addCategory(Intent.CATEGORY_LAUNCHER);
		List<ResolveInfo> list = pm.queryIntentActivities(intent, PackageManager.PERMISSION_GRANTED);
		List<ApplicationModel> models = new ArrayList<ApplicationModel>();
		for (ResolveInfo pkg: list) {
			ApplicationModel m = new ApplicationModel();
			m.label = (String) pkg.loadLabel(pm);
			m.icon = pkg.loadIcon(pm);
			m.packageName = pkg.activityInfo.packageName;
			models.add(m);
		}
		return models;
	}
}
