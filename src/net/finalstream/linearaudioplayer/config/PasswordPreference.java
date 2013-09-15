package net.finalstream.linearaudioplayer.config;

import net.finalstream.linearaudioplayer.commons.Crypto;
import android.app.Activity;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.location.Address;
import android.preference.EditTextPreference;
import android.util.AttributeSet;

public class PasswordPreference extends EditTextPreference {

	String seed = "net.finalstream.linearaudioplayer";
	
	public PasswordPreference(Context context) {
		super(context);
		// TODO 自動生成されたコンストラクター・スタブ
	}
	
		 public PasswordPreference(Context context, AttributeSet attrs) {
	         super(context, attrs);
	         // TODO 自動生成されたコンストラクター・スタブ
	 }

	public PasswordPreference(Context context, AttributeSet attrs, int
	defStyle) {
	         super(context, attrs, defStyle);
	         // TODO 自動生成されたコンストラクター・スタブ
	 } 
	
   @Override
    protected void onPrepareDialogBuilder(Builder builder) {
        super.onPrepareDialogBuilder(builder);

        // get value from xml file
        String value = getText();

        // convert value
        try {
			value = Crypto.decrypt(seed, value);
		} catch (Exception e) {
		}

        // display converted value
        this.getEditText().setText(value);
    } 
   
   @Override
    public void onClick(DialogInterface dialog, int which) {
            // re-convert value and save to xml
            String value = this.getEditText().getText().toString();
            try {
				value = Crypto.encrypt(seed, value);
			} catch (Exception e) {
			}
            setText(value);
        } 

}
