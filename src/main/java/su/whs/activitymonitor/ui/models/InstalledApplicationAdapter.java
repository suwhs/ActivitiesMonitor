package su.whs.activitymonitor.ui.models;

import java.util.List;

import su.whs.activitymonitor.R;
import su.whs.activitymonitor.ui.widgets.OnSelectionChangeListener;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.TextView;

public class InstalledApplicationAdapter extends
		ArrayAdapter<InstalledApplicationModel> {
		
	private Context context = null;
	private List<InstalledApplicationModel> items = null;
	private OnSelectionChangeListener mOnChecked;
	
	public InstalledApplicationAdapter(Context ctx, List<InstalledApplicationModel> entries, int tvri) {
		super(ctx,tvri);
		
		mOnChecked = (OnSelectionChangeListener) context;		
		context = ctx;
		items = entries;
	}
	
	@Override
	public InstalledApplicationModel getItem(int pos) {
		return items.get(pos);
	}
	
	static class ViewHolder {
		public ImageView icon;
		public TextView label;
		public CheckBox selected;
	}
	
	public View getView(int position, View convertView, ViewGroup parent) {
		View row = convertView;
		ViewHolder h = null;
		if (row==null) {
			LayoutInflater li = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			row = li.inflate(R.layout.app_list_cb_item, null, true);
			h = new ViewHolder();
			h.icon = (ImageView) row.findViewById(R.id.appIcon);
			h.label = (TextView) row.findViewById(R.id.appLabel);
			h.selected = (CheckBox) row.findViewById(R.id.appSelected);
			h.selected.setChecked(false);
			final int pos = position;
			h.selected.setOnCheckedChangeListener(new OnCheckedChangeListener() {

				@Override
				public void onCheckedChanged(CompoundButton arg0, boolean arg1) {
				//	InstalledApplicationModel mo = (InstalledApplicationModel) arg0.getTag();
					if (mOnChecked!=null && arg1 != arg0.isChecked())
						mOnChecked.OnApplicationSelectionChanged(pos, arg0.isChecked());					
				}});
			row.setTag(h);
		} else {
			h = (ViewHolder) row.getTag();
		}
		
		InstalledApplicationModel m = getItem(position);
		h.selected.setTag(m);
		if (m!=null) {
			h.icon.setImageDrawable(m.getIcon());
			h.label.setText(m.getLabel());
			if (h.selected.isChecked() != m.isSelected())
				h.selected.setChecked(m.isSelected());
			}
		
		return row;
	}

}
