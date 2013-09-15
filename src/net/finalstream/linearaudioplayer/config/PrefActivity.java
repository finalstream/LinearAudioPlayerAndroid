package net.finalstream.linearaudioplayer.config;

import net.finalstream.linearaudioplayer.R;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

public class PrefActivity extends PreferenceActivity {

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.pref);
    }
 
	public static String getLastfmUser(Context ctx){
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(ctx);
        String res  = settings.getString("lastfmUser", null);
        return res;
    }
	
	public static String getLastfmPassword(Context ctx){
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(ctx);
        String res  = settings.getString("lastfmPassword", null);
        return res;
    }
	
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        // •Û‘¶‚Ìˆ—
        super.onSaveInstanceState(outState);
    }
	
}
