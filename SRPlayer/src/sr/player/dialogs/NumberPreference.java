package sr.player.dialogs;

import sr.player.R;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.preference.EditTextPreference;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;

public class NumberPreference extends EditTextPreference implements TextWatcher {

	private int minValue;
	private int maxValue;
	
	public NumberPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		if (attrs != null) {
			TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.numberpreference);
			this.minValue = array.getInt(R.styleable.numberpreference_min, 0);
			this.maxValue = array.getInt(R.styleable.numberpreference_max, Integer.MAX_VALUE);
			array.recycle();
		} else {
			this.minValue = 0;
			this.maxValue = Integer.MAX_VALUE;
		}
		getEditText().addTextChangedListener(this);
	}

	
	public NumberPreference(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		if (attrs != null) {
			TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.numberpreference);
			this.minValue = array.getInt(R.styleable.numberpreference_min, 0);
			this.maxValue = array.getInt(R.styleable.numberpreference_max, Integer.MAX_VALUE);
			array.recycle();
		} else {
			this.minValue = 0;
			this.maxValue = Integer.MAX_VALUE;
		}
		getEditText().addTextChangedListener(this);
	}


	@Override
	public void afterTextChanged(Editable s) {
		AlertDialog dialog = (AlertDialog) getDialog();
		// Sometimes this method is called and dialog is null
		if (dialog != null) {
			int enteredNumber;
			try {
				enteredNumber = Integer.parseInt(getEditText().getText().toString());
			} catch (NumberFormatException e) {
				enteredNumber = -1;
			}
		    dialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(
		    		enteredNumber >= this.minValue && enteredNumber <= this.maxValue);
		}
	}


	@Override
	public void beforeTextChanged(CharSequence s, int start, int count,
			int after) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count) {
		// TODO Auto-generated method stub
		
	}

}
