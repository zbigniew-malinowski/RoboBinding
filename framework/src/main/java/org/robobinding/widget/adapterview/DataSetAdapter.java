package org.robobinding.widget.adapterview;

import org.robobinding.BindableView;
import org.robobinding.itempresentationmodel.ItemContext;
import org.robobinding.itempresentationmodel.RefreshableItemPresentationModel;
import org.robobinding.presentationmodel.AbstractPresentationModelObject;
import org.robobinding.property.DataSetValueModel;
import org.robobinding.property.DataSetValueModelWrapper;
import org.robobinding.property.PropertyChangeListener;
import org.robobinding.viewattribute.ViewTag;
import org.robobinding.viewattribute.ViewTags;

import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

/**
 * 
 * @since 1.0
 * @author Cheng Wei
 * @author Robert Taylor
 */
public class DataSetAdapter<T> extends BaseAdapter {
	private enum DisplayType {
		ITEM_LAYOUT, DROPDOWN_LAYOUT
	}

	private final DataSetValueModel dataSetValueModel;

	private final ItemLayoutBinder itemLayoutBinder;
	private final ItemLayoutBinder dropdownLayoutBinder;
	private final ItemLayoutSelector layoutSelector;
	private final ViewTags<RefreshableItemPresentationModel> viewTags;

	private final boolean preInitializeViews;

	private boolean propertyChangeEventOccurred;

	public DataSetAdapter(DataSetValueModel dataSetValueModel, ItemLayoutBinder itemLayoutBinder, 
			ItemLayoutBinder dropdownLayoutBinder, ItemLayoutSelector layoutSelector, 
			ViewTags<RefreshableItemPresentationModel> viewTags, boolean preInitializeViews) {
		this.preInitializeViews = preInitializeViews;
		
		this.dataSetValueModel = createValueModelFrom(dataSetValueModel);
		this.itemLayoutBinder = itemLayoutBinder;
		this.dropdownLayoutBinder = dropdownLayoutBinder;
		this.layoutSelector = layoutSelector;
		this.viewTags = viewTags;

		propertyChangeEventOccurred = false;
	}

	private DataSetValueModel createValueModelFrom(DataSetValueModel valueModel) {
		if (!preInitializeViews) {
			return wrapAsZeroSizeDataSetUntilPropertyChangeEvent(valueModel);
		} else {
			return valueModel;
		}
	}

	public void observeChangesOnTheValueModel() {
		dataSetValueModel.addPropertyChangeListener(new PropertyChangeListener() {
			@Override
			public void propertyChanged() {
				propertyChangeEventOccurred = true;
				notifyDataSetChanged();
			}
		});
	}

	private DataSetValueModel wrapAsZeroSizeDataSetUntilPropertyChangeEvent(final DataSetValueModel valueModel) {
		return new DataSetValueModelWrapper(valueModel) {
			@Override
			public int size() {
				if (propertyChangeEventOccurred)
					return valueModel.size();

				return 0;
			}
		};
	}

	@Override
	public int getCount() {
		if (dataSetValueModel == null)
			return 0;

		return dataSetValueModel.size();
	}

	@Override
	public Object getItem(int position) {
		return dataSetValueModel.getItem(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		return createViewFromResource(position, convertView, parent, DisplayType.ITEM_LAYOUT);
	}

	@Override
	public View getDropDownView(int position, View convertView, ViewGroup parent) {
		return createViewFromResource(position, convertView, parent, DisplayType.DROPDOWN_LAYOUT);
	}

	private View createViewFromResource(int position, View convertView, ViewGroup parent, DisplayType displayType) {
		if (convertView == null) {
			return newView(position, parent, displayType);
		} else {
			updateItemPresentationModel(convertView, position);
			return convertView;
		}
	}

	private View newView(int position, ViewGroup parent, DisplayType displayType) {
		BindableView bindableView;
		Object item = getItem(position);
		if (displayType == DisplayType.ITEM_LAYOUT) {
			int layoutId = layoutSelector.selectItemLayout(item, position);
			bindableView = itemLayoutBinder.inflate(parent, layoutId);
		} else {
			int layoutId = layoutSelector.selectDropdownLayout(item, position);
			bindableView = dropdownLayoutBinder.inflate(parent, layoutId);
		}
		
		View view = bindableView.getRootView();
		RefreshableItemPresentationModel itemPresentationModel = dataSetValueModel.newRefreshableItemPresentationModel(
				getItemViewType(position));
		itemPresentationModel.updateData(item, new ItemContext(view, position));
		bindableView.bindTo((AbstractPresentationModelObject)itemPresentationModel);

		ViewTag<RefreshableItemPresentationModel> viewTag = viewTags.tagFor(view);
		viewTag.set(itemPresentationModel);
		return view;
	}

	private void updateItemPresentationModel(View view, int position) {
		ViewTag<RefreshableItemPresentationModel> viewTag = viewTags.tagFor(view);
		RefreshableItemPresentationModel itemPresentationModel = viewTag.get();
		itemPresentationModel.updateData(getItem(position), new ItemContext(view, position));
		refreshIfRequired(itemPresentationModel);
	}
	
	private void refreshIfRequired(RefreshableItemPresentationModel itemPresentationModel) {
		if(preInitializeViews) {
			itemPresentationModel.refresh();
		}
	}
	
	@Override
	public int getViewTypeCount() {
		return layoutSelector.getViewTypeCount();
	}
	
	@Override
	public int getItemViewType(int position) {
		return layoutSelector.getItemViewType(getItem(position), position);
	}
}