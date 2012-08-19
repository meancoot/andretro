package org.andretro.browser;

import org.andretro.*;

import android.app.*;
import android.content.*;
import android.view.*;
import android.widget.*;

interface IconAdapterItem
{
	public abstract String getText();
	public abstract int getIconResourceId();
}

class IconAdapter<T extends IconAdapterItem> extends ArrayAdapter<T>
{
    private final int layout;

    public IconAdapter(Activity aContext, int aLayout)
    {
        super(aContext, aLayout);
    
        layout = aLayout;
    }
    
    @Override public View getView(int aPosition, View aConvertView, ViewGroup aParent)
    {
        // Build the view
        if(aConvertView == null)
        {
            LayoutInflater inflater = (LayoutInflater)aParent.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            aConvertView = inflater.inflate(layout, aParent, false);
        }
        
        // Fill the view
        IconAdapterItem item = getItem(aPosition);

        TextView textView = (TextView)aConvertView.findViewById(R.id.name);
        if(null != textView)
        {
            textView.setText(item.getText());
        }
        
        ImageView imageView = (ImageView)aConvertView.findViewById(R.id.icon);
        if(null != imageView)
        {
            imageView.setImageResource(item.getIconResourceId());
        }
        
        return aConvertView;
    }
}
