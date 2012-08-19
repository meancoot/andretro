package org.andretro;

import android.content.*;
import android.app.*;
import android.os.*;
import android.view.*;
import android.widget.*;

public class QuestionDialog extends DialogFragment implements View.OnKeyListener
{
	public interface QuestionHandler
	{
		void onAnswer(int aID, QuestionDialog aDialog, boolean aPositive);
	}
	
	/**
	 * Build a question dialog with the specified strings.
	 * 
	 * @param aID The ID to pass to callback.
	 * @param aTitle The title of the dialog.
	 * @param aMessage The message to display in the dialog.
	 * @param aPositive The text on the positive button of the dialog.
	 * @param aNegative The text on the negative button of the dialog.
	 * @return The dialog.
	 */
    public static QuestionDialog newInstance(int aID, String aTitle, String aMessage, String aPositive, String aNegative, Bundle aUserData)
    {
       	// Create the output
        QuestionDialog result = new QuestionDialog();

        // Fill the details
        Bundle args = new Bundle();
        args.putInt("id", aID);
        args.putString("title", aTitle);
        args.putString("message", aMessage);
        args.putString("positive", aPositive);
        args.putString("negative", aNegative);
        args.putBundle("userdata", aUserData);
        result.setArguments(args);
        
        // Done
        return result;
    }
    
    Bundle getUserData()
    {
    	return getArguments().getBundle("userdata");
    }
    
	@Override public Dialog onCreateDialog(Bundle aState)
	{
		final Bundle args = getArguments();
		final int id = args.getInt("id");
		
		// HACK: Use hack view for input focus
	    final View hackView = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_focus_hack, null);
	    ((EditText)hackView).setText(args.getString("message"));

		// TODO: Use string resources
		AlertDialog result = new AlertDialog.Builder(getActivity())
				.setTitle(args.getString("title"))
				.setView(hackView)
				.setPositiveButton(args.getString("positive"), new DialogInterface.OnClickListener()
		        {
		            @Override public void onClick(DialogInterface UNUSED, int aWhich)
		            {
		            	((QuestionHandler)getActivity()).onAnswer(id, QuestionDialog.this, true);
		            }
		        })
		        .setNegativeButton(args.getString("negative"), new DialogInterface.OnClickListener()
		        {
		        	@Override public void onClick(DialogInterface UNUSED, int aWhich)
		        	{
		        		((QuestionHandler)getActivity()).onAnswer(id, QuestionDialog.this, false);
		        	}
		        })
		        .create();
		
		return result;
	}
		
	@Override public void onCancel(DialogInterface aDialog)
	{
		((QuestionHandler)getActivity()).onAnswer(getArguments().getInt("id"), QuestionDialog.this, false);
		super.onCancel(aDialog);
	}
	
	@Override public boolean onKey(View aView, int aKeyCode, KeyEvent aEvent)
	{
		Dialog dialog = getDialog();
		return (null != dialog) ? dialog.dispatchKeyEvent(aEvent) : false;
	}
}
