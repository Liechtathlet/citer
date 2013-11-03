/**
 * 
 */
package ch.zhaw.mdp.lhb.citr.activities;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.inputmethod.InputMethodManager;
import ch.zhaw.mdp.lhb.citr.exception.CitrExceptionHandler;

/**
 * @author Daniel Brun
 * 
 * Base-Class for citr-activities.
 */
public abstract class CitrBaseActivity extends Activity {

	
	/* (non-Javadoc)
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle aSavedInstanceState) {
		super.onCreate(aSavedInstanceState);
		
		Thread.setDefaultUncaughtExceptionHandler(new CitrExceptionHandler(this));
	}
	/**
	 * Clean up the current screen.
	 */
	public void cleanScreen() {

		InputMethodManager inputManager = (InputMethodManager) CitrBaseActivity.this
				.getSystemService(Context.INPUT_METHOD_SERVICE);
		
		if (inputManager != null && this.getCurrentFocus() != null && this.getCurrentFocus().getWindowToken() != null) {
			inputManager.hideSoftInputFromWindow(CitrBaseActivity.this
					.getCurrentFocus().getWindowToken(),
					InputMethodManager.HIDE_NOT_ALWAYS);
		}
	}

}
