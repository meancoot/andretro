package org.andretro.input.view;
import org.andretro.emulator.*;

import android.util.*;
import android.view.*;
import android.content.*;
import android.widget.*;
import android.app.*;
import android.graphics.*;

import java.io.*;
import java.util.*;

import javax.xml.parsers.*;
import org.w3c.dom.*;

public class InputGroup extends RelativeLayout
{
	public static interface InputHandler
	{
	    int getBits(int aX, int aY);
	}

	private static class Handler
	{
		final Rect area = new Rect();
		final View view;
		final InputHandler handler;
		
		public Handler(View aView, InputHandler aHandler)
		{
			view = aView;
			handler = aHandler;
		}
		
		public int getBits(int aX, int aY)
		{
			area.left = view.getLeft();
			area.right = view.getRight();
			area.top = view.getTop();
			area.bottom = view.getBottom();
			return area.contains(aX, aY) ? handler.getBits(aX, aY) : 0;
		}
	}
	private ArrayList<Handler> handlers = new ArrayList<Handler>();
	
	private int currentBits;
	
    public InputGroup(Context aContext, AttributeSet aAttributes)
    {
        super(aContext, aAttributes);
        setFocusableInTouchMode(true);
    }

    @Override public boolean onTouchEvent(MotionEvent aEvent)
    {
    	currentBits = 0;
    	
        final int count = aEvent.getPointerCount();
        for(int i = 0; i != count; i ++)
        {
        	if(aEvent.getActionMasked() != MotionEvent.ACTION_UP || i != aEvent.getActionIndex())
        	{
        		for(Handler j: handlers)
	        	{
	        		currentBits |= j.getBits((int)aEvent.getX(i), (int)aEvent.getY(i));
	        	}
        	}
        }

        return true;
    }    
    
    public int getBits()
    {    
    	return currentBits;
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
    	
    	handlers.clear();
    }
    
	int getInt(String aInt)
	{
		return (null == aInt || "".equals(aInt)) ? 0 : Integer.parseInt(aInt);        			
	}

    
    public void loadInputLayout(final Activity aContext, final ModuleInfo aModule, final InputStream aFile)
    {
    	try
    	{
	    	removeChildren();
	    	
	    	Element inputElement = aModule.getInputDefinition();
	    	if(null == inputElement)
	    	{
	    		Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(aFile);
	    		inputElement = document.getDocumentElement();
	    	}
	    	
			int horizontalAnchor = RelativeLayout.ALIGN_PARENT_LEFT;
			int verticalAnchor = RelativeLayout.ALIGN_PARENT_TOP;
	
	    	NodeList inputs = inputElement.getChildNodes();
	    	for(int i = 0; i != inputs.getLength(); i ++)
	    	{
	    		if(Node.ELEMENT_NODE == inputs.item(i).getNodeType())
	    		{
		    		Element input = (Element)inputs.item(i);
		    		
					if("anchor-left".equals(input.getNodeName()))
					{
						horizontalAnchor = RelativeLayout.ALIGN_PARENT_LEFT;
					}
					else if("horizontal-center".equals(input.getNodeName()))
					{
						horizontalAnchor = RelativeLayout.CENTER_HORIZONTAL;
					}
					else if("anchor-right".equals(input.getNodeName()))
					{
						horizontalAnchor = RelativeLayout.ALIGN_PARENT_RIGHT;
					}
		
					if("anchor-top".equals(input.getNodeName()))
					{
						verticalAnchor = RelativeLayout.ALIGN_PARENT_TOP;
					}
					else if("vertical-center".equals(input.getNodeName()))
					{
						verticalAnchor = RelativeLayout.CENTER_VERTICAL;
					}
					else if("anchor-bottom".equals(input.getNodeName()))
					{
						verticalAnchor = RelativeLayout.ALIGN_PARENT_BOTTOM;
					}        			
					
					InputHandler inputView = null;
					
					//nbDips * getResources().getDisplayMetrics().density
					
					if("ButtonDiamond".equals(input.getNodeName()))
					{
						final int upbits = getInt(input.getAttribute("upbits"));
						final int downbits = getInt(input.getAttribute("downbits"));
						final int leftbits = getInt(input.getAttribute("leftbits"));
						final int rightbits = getInt(input.getAttribute("rightbits"));
						inputView = new ButtonDiamond(aContext, upbits, downbits, leftbits, rightbits);
					}
					else if("ButtonDuo".equals(input.getNodeName()))
					{
						final int leftbits = getInt(input.getAttribute("leftbits"));
						final int rightbits = getInt(input.getAttribute("rightbits"));
						inputView = new ButtonDuo(aContext, leftbits, rightbits);        				        				
					}
					else if("Button".equals(input.getNodeName()))
					{
						final int bits = getInt(input.getAttribute("bits"));
						inputView = new Button(aContext, bits);
					}
					
					if(null != inputView)
					{
						final int width = getInt(input.getAttribute("width"));
						final int height = getInt(input.getAttribute("height"));
						RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(width, height);
		
						params.topMargin = getInt(input.getAttribute("topmargin"));
						params.bottomMargin = getInt(input.getAttribute("bottommargin"));
						params.leftMargin = getInt(input.getAttribute("leftmargin"));
						params.rightMargin = getInt(input.getAttribute("rightmargin"));
						params.addRule(horizontalAnchor);
						params.addRule(verticalAnchor);
						    				
						ImageView handler = (ImageView)inputView;
						addView(handler, params);
						handlers.add(new Handler(handler, inputView));
					}	    			
	    		}
	    	}
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
