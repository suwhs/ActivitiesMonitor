package su.whs.activitymonitor.ui.models;

import android.graphics.drawable.Drawable;

public class InstalledApplicationModel {
	public static final int SELECTED = 1;
	private String packageName;
	private Drawable icon;
	private String label;
	private int f = 0;
	
	public InstalledApplicationModel(String pName, Drawable ico, String lab) {
		packageName = pName;
		icon = ico;
		label = lab;
	}
	
	public void addFlag(int flag) {
		f = f | flag;
	}
	public String getPackageName() { return packageName; }
	public Drawable getIcon() { return icon; }
	public String getLabel() { return label; }
	public boolean isSelected() { return (f & SELECTED) > 0; }
}
