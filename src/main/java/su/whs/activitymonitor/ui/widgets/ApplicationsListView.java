package su.whs.activitymonitor.ui.widgets;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import su.whs.activitymonitor.BuildConfig;
import su.whs.activitymonitor.R;
import su.whs.system.models.ApplicationModel;
import su.whs.system.utils.Applications;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

/**
 * 
 * @author igor n. boulliev
 *
 */

public class ApplicationsListView extends LinearLayout {
	
	public interface OnApplicationSelectionChangedListener {
		void OnApplicationSelectionChanged(String packageName, boolean selected);
	}
	
	public class ApplicationAdapter extends ArrayAdapter<ApplicationModel> {
		private LayoutInflater inflater;
		public ApplicationAdapter(Context context, int textViewResourceId, List<ApplicationModel> e) {
			super(context, textViewResourceId, e);
			inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);		
		}
		
		class Holder {
			public ImageView icon;
			public TextView label;
			public android.widget.CheckBox checkBox;
		}
		
		private OnCheckedChangeListener mOnCheckedChanged = new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton checkBox, boolean checked) {
				if (BuildConfig.DEBUG)
				Log.v("INFO","enter checked");
				ApplicationModel item = (ApplicationModel)checkBox.getTag();
				Log.v("INFO", "comparing " + item.selected + " and " + checked);
				if (item.selected!=checked) {
					item.selected = checked;
					ApplicationsListView.this.changePackageSelection(item.packageName, checked);
				} 
			}			
		};
		
		@SuppressWarnings("deprecation")
		@Override
		public View getView(int pos, View conView, ViewGroup parent) {
			View row = conView;
			Holder holder;
			if (row==null) {
				row = inflater.inflate(R.layout.app_list_cb_item, null, true);
				holder = new Holder();
				holder.icon = (ImageView) row.findViewById(R.id.appIcon);
				
				holder.label = (TextView) row.findViewById(R.id.appLabel);
				holder.checkBox = (android.widget.CheckBox) row.findViewById(R.id.appSelected);
				row.setTag(holder);
			} else {
				holder = (Holder) row.getTag();
				
			}
			
			ApplicationModel item = getItem(pos);
			if (item.icon!=null)
				holder.icon.setBackgroundDrawable(item.icon);
			holder.label.setText(item.label);
			holder.checkBox.setTag(item);
			
			if (holder.checkBox.isChecked()!=item.selected)
				holder.checkBox.setChecked(item.selected);
			holder.checkBox.setOnCheckedChangeListener(mOnCheckedChanged);
			
			return row;
		}
		
		public void sortCheckedFirst() {
			sort(new Comparator<ApplicationModel>(){

				@Override
				public int compare(ApplicationModel arg0, ApplicationModel arg1) {
					if (arg0.selected && arg1.selected) {
						return arg0.label.compareTo(arg1.label);
					}
					else if (arg0.selected) 
						return -1;
					else if (arg1.selected)
						return 1;
					return arg0.label.compareTo(arg1.label);					
				}});
		}
	}
	
	private OnApplicationSelectionChangedListener mOnApplicationSelectionChanged = null;
	private ListView listView = null;
	private PackageManager pm = null;
	private Map<String,Integer> ignoredPackages = null;
	private Map<String,Boolean> selectedPackages = null;
	private List<ApplicationModel> installedApplications = null;
	private String _title = null;
	private boolean isSortCheckedFirst = false;
	
	private class LoadingPackagesListTask extends AsyncTask<Void,Void,List<ApplicationModel>> {

		@Override
		protected List<ApplicationModel> doInBackground(Void... arg0) {
			return Applications.getInstalledApplications(getContext());
		}
		
		protected void onPostExecute(List<ApplicationModel> models) {
			fillList(models);
		}
	}
	
	public ApplicationsListView(Context context) {
		super(context,null);
	}
	
	public ApplicationsListView(Context context, AttributeSet attrs) {
		super(context, attrs);
		pm = getContext().getPackageManager();
		ignoredPackages = new HashMap<String,Integer>();
		selectedPackages = new HashMap<String,Boolean>();
		// loading starts		
		TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.whsInstalledApplicationsList);
		try {
			_title = ta.getString(R.styleable.whsInstalledApplicationsList_listTitle);
		} finally {
			ta.recycle();
		}		
	}
	
	
	public void update() {		
		new LoadingPackagesListTask().execute();
	}
	
	protected void fillList(List<ApplicationModel> models) {
		installedApplications = new ArrayList<ApplicationModel>();
		
		for (ApplicationModel model: models) {
			if (ignoredPackages!=null && ignoredPackages.containsKey(model.packageName)) {
				continue;
			}
			if (selectedPackages!=null && selectedPackages.containsKey(model.packageName) && selectedPackages.get(model.packageName)) { 
				model.selected = true;
			}
			installedApplications.add(model);
		}
		ApplicationAdapter ada = new ApplicationAdapter(listView.getContext(), R.layout.app_list_cb_item, installedApplications);
		if (isSortCheckedFirst)
			ada.sortCheckedFirst();
		listView.setAdapter(ada);
	}
	
	@Override
	protected void onFinishInflate() {
		LayoutInflater li = LayoutInflater.from(getContext());
		li.inflate(R.layout.app_list_fragment, this);
		listView = (ListView) findViewById(R.id.appListView);
		TextView apt = (TextView) findViewById(R.id.appListTitle);
		if (_title==null) {
			// apt.setVisibility(GONE);
		} else {
			apt.setText(_title);
		}
		update();
	}

	public void setIgnoredPackagesList(List<String> packageNames) {
		ignoredPackages.clear();
		if (packageNames==null) return;
		for(String p: packageNames) {
			ignoredPackages.put(p, 0);
		}
	}
	
	public void setSelectedPackagesList(List<String> packageNames) {
		selectedPackages.clear();
		if (packageNames==null) return;
		for(String p: packageNames) {
			selectedPackages.put(p, true);
		}
	}
	
	public void setOnApplicationSelectionChangedListener(OnApplicationSelectionChangedListener l) {
		mOnApplicationSelectionChanged = l;
	}
	
	public void setSortCheckedFirst(boolean scf) {
		isSortCheckedFirst = scf;
		if (listView!=null) {
			ApplicationAdapter ada = (ApplicationAdapter) listView.getAdapter();
			if (ada!=null)
				ada.sortCheckedFirst();
		}
	}
	
	public List<String> getSelectedPackages() {
		List<String> result = new ArrayList<String>();
		for(String k: selectedPackages.keySet()) {
			if (selectedPackages.get(k)==true) 
				result.add(k);
		}
		return result;
	}
	
	protected void changePackageSelection(String p, boolean s) {
		if (!selectedPackages.containsKey(p)) {
			selectedPackages.put(p, s);
			if (mOnApplicationSelectionChanged!=null) {
				mOnApplicationSelectionChanged.OnApplicationSelectionChanged(p, s);
			} else {
				if (BuildConfig.DEBUG)
				Log.v("INFO","set package '" + p + "' selection to '" + s);
				if (selectedPackages.get(p)!=s) {
					selectedPackages.put(p, s);
					mOnApplicationSelectionChanged.OnApplicationSelectionChanged(p, s);
				}				
			}
		} else {
			if (selectedPackages.get(p)!=s) {
				selectedPackages.put(p, s);
				mOnApplicationSelectionChanged.OnApplicationSelectionChanged(p, s);
			}
		}
	}
}
