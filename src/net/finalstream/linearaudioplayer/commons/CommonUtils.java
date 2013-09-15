package net.finalstream.linearaudioplayer.commons;

import java.io.UnsupportedEncodingException;
import java.util.List;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.util.Log;

public class CommonUtils {

	/**
	 * サービス起動中か確認
	 * @param c
	 * @param cls
	 * @return
	 */
	public static boolean isServiceRunning(Context c, Class<?> cls) {
	    ActivityManager am = (ActivityManager) c.getSystemService(Context.ACTIVITY_SERVICE);
	    List<RunningServiceInfo> runningService = am.getRunningServices(Integer.MAX_VALUE);
	    for (RunningServiceInfo i : runningService) {
	        if (cls.getName().equals(i.service.getClassName())) {
	            return true;
	        }
	    }
	    return false;
	}
	
	/**
	 * アプリのバージョンを取得
	 * @param prefix
	 * @param context
	 * @return
	 */
	 public static String getVersionNumber(String prefix, Context context){
	    	String versionName = prefix;
	    	PackageManager pm = context.getPackageManager();
	    	try {
	    		PackageInfo info = pm.getPackageInfo(context.getPackageName(), PackageManager.GET_META_DATA);
	    		versionName += info.versionName;
	    	} catch (NameNotFoundException e) {
	    		versionName +="0";
	    	}
	    	return versionName;
	    }
	
	 public static String utf8toSJIS(byte[] b) {
			int len = b.length;
			byte[] nb = new byte[len];
			int i = 0;
			int j = 0;
			while (i < len) {
				byte first = b[i++];
				if (first == 0) break;
				
				if ((first & 0x80) == 0) {
					nb[j++] = first;
				} else {
					byte second = b[i++];
					nb[j++] = (byte)((((first & 0x03) << 6) | (second & 0x3f)) & 0xff);
				}
			}
			
			byte[] last = new byte[j];
			System.arraycopy(nb, 0, last, 0, j);
			
			try {
				if (isValidSJIS(last))
					return new String(last, "Windows-31J");
				else
					return new String(b);
			} catch (UnsupportedEncodingException e) {
				throw new RuntimeException(e);
			}
		}

		private static boolean isValidSJIS(byte[] last) {
			int len = last.length;
			int i = 0;
			while (i < len) {
				int b = last[i++] & 0xff;
				if (b < 0x80) continue;
				
				if (i >= len) return false;
				
				int c = last[i++] & 0xff;
				
				if ((0x81 <= b && 0x9f >= b) || (0xe0 <= b && 0xef >= b)) {
					if (0x40 <= c && 0xfc >= c && 0x7f != c) {
						continue;
					}
				}

				return false;			
			}
			return true;
		}
}
