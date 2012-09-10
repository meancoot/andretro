package org.andretro.input.view;

import android.util.*;
import android.view.*;
import android.content.*;
import android.widget.*;
import android.app.*;

import javax.xml.parsers.*;
import java.io.*;
import java.util.*;

import org.xml.sax.*;
import org.xml.sax.helpers.*;

public class InputGroup extends RelativeLayout implements InputHandler
{
    public InputGroup(Context aContext, AttributeSet aAttributes)
    {
        super(aContext, aAttributes);
    }
    
    public int getBits()
    {    
        int bits = 0;
        int childCount = getChildCount();
        
        for(int i = 0; i != childCount; i ++)
        {
            View child = getChildAt(i);
            if(child instanceof InputHandler)
            {
                InputHandler childIH = (InputHandler)child;
                bits |= childIH.getBits();
            }
        }
        
        return bits;
    }
    
    public void removeChildren()
    {
    	final List<View> toRemove = new ArrayList<View>();
    	
    	for(int i = 0; i != getChildCount(); i ++)
    	{
    		final View thisOne = getChildAt(i);
    		if(thisOne instanceof InputHandler)
    		{
    			toRemove.add(thisOne);
    		}
    	}
    	
    	for(final View view: toRemove)
    	{
    		removeView(view);
    	}
    }
    
    public void loadInputLayout(final Activity aContext, final InputStream aFile)
    {
    	removeChildren();
    	
        SAXParserFactory factory = SAXParserFactory.newInstance();
        try
        {
        	final SAXParser parser = factory.newSAXParser();

        	parser.parse(aFile, new DefaultHandler()
        	{
        		int horizontalAnchor = RelativeLayout.ALIGN_PARENT_LEFT;
        		int verticalAnchor = RelativeLayout.ALIGN_PARENT_TOP;
        		        		
        		int getInt(String aInt)
        		{
        			return (null == aInt) ? 0 : Integer.parseInt(aInt);        			
        		}
        		
        		@Override public void startElement(String aURI, String aName, String aQualifiedName, Attributes aAttributes) throws SAXException
        		{
        			if("anchor-left".equals(aName))
        			{
        				horizontalAnchor = RelativeLayout.ALIGN_PARENT_LEFT;
        			}
        			else if("horizontal-center".equals(aName))
        			{
        				horizontalAnchor = RelativeLayout.CENTER_HORIZONTAL;
        			}
        			else if("anchor-right".equals(aName))
        			{
        				horizontalAnchor = RelativeLayout.ALIGN_PARENT_RIGHT;
        			}

        			if("anchor-top".equals(aName))
        			{
        				verticalAnchor = RelativeLayout.ALIGN_PARENT_TOP;
        			}
        			else if("vertical-center".equals(aName))
        			{
        				verticalAnchor = RelativeLayout.CENTER_VERTICAL;
        			}
        			else if("anchor-bottom".equals(aName))
        			{
        				verticalAnchor = RelativeLayout.ALIGN_PARENT_BOTTOM;
        			}        			
        			
        			InputHandler inputView = null;
        			
        			//nbDips * getResources().getDisplayMetrics().density
        			
        			if("ButtonDiamond".equals(aName))
        			{
        				final int upbits = getInt(aAttributes.getValue("", "upbits"));
        				final int downbits = getInt(aAttributes.getValue("", "downbits"));
        				final int leftbits = getInt(aAttributes.getValue("", "leftbits"));
        				final int rightbits = getInt(aAttributes.getValue("", "rightbits"));
        				inputView = new ButtonDiamond(aContext, upbits, downbits, leftbits, rightbits);
        			}
        			else if("ButtonDuo".equals(aName))
        			{
        				final int leftbits = getInt(aAttributes.getValue("", "leftbits"));
        				final int rightbits = getInt(aAttributes.getValue("", "rightbits"));
        				inputView = new ButtonDuo(aContext, leftbits, rightbits);        				        				
        			}
        			else if("Button".equals(aName))
        			{
        				final int bits = getInt(aAttributes.getValue("", "bits"));
        				inputView = new Button(aContext, bits);
        			}
        			
    				if(null != inputView)
    				{
	    				final int width = getInt(aAttributes.getValue("", "width"));
	    				final int height = getInt(aAttributes.getValue("", "height"));
	    				RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(width, height);
	
						params.topMargin = getInt(aAttributes.getValue("", "topmargin"));
						params.bottomMargin = getInt(aAttributes.getValue("", "bottommargin"));
						params.leftMargin = getInt(aAttributes.getValue("", "leftmargin"));
						params.rightMargin = getInt(aAttributes.getValue("", "rightmargin"));
	    				params.addRule(horizontalAnchor);
	    				params.addRule(verticalAnchor);
						    				
	    				addView((ImageView)inputView, params);
    				}        			
        		}
        	});
        }
        catch(final Exception e)
        {
        	aContext.runOnUiThread(new Runnable()
        	{
        		@Override public void run()
        		{
        			String message = e.getMessage();
        			message = (message == null) ? "No Message" : message;
                    Toast.makeText(aContext, "Failed to load controls from " + aFile + "\n" + e.getClass().toString() + "\n" + message, Toast.LENGTH_LONG).show();
        		}
        	});
        }    	
    }
}
