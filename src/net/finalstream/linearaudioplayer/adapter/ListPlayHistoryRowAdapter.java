package net.finalstream.linearaudioplayer.adapter;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import net.finalstream.linearaudioplayer.LinearConst;
import net.finalstream.linearaudioplayer.R.id;
import net.finalstream.linearaudioplayer.R.layout;
import net.finalstream.linearaudioplayer.bean.AudioItemBean;

import android.R;
import android.app.Activity;
import android.content.Context;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

public class ListPlayHistoryRowAdapter extends ArrayAdapter<AudioItemBean> implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 8230729083797318794L;
	DateFormat df;
	/**
	 * 
	 */
	private final Activity context;
	
	public ListPlayHistoryRowAdapter(Activity context, int resource, int textViewResourceId) {
		super(context, resource, textViewResourceId);
		this.context = context;
		df = new SimpleDateFormat(LinearConst.DATEFORMAT_YYYYMMDDHHMMSSSSS);
	}
	
	public View getView(int position, View convertView, ViewGroup parent) {
		View rowView = convertView;
		if (rowView == null) {
			// TODO:Å@inflaterÇÕíxÇ¢ã^òfÇ†ÇËÅB
    		LayoutInflater inflater = this.context.getLayoutInflater();
    		rowView = inflater.inflate(layout.any_list_item_2, null);
		}
		AudioItemBean audioItem = this.getItem(position);
		TextView textView1 = (TextView) rowView.findViewById(android.R.id.text1);    		
		textView1.setText(audioItem.getTitle());
		TextView textView2 = (TextView) rowView.findViewById(android.R.id.text2);    		
		textView2.setText(audioItem.getArtist());

		TextView textView3 = (TextView) rowView.findViewById(id.text3);    		
		
		Date lastplaydate = null;
		//Log.d("LINEAR", "sort value: " + audioItem.getSort());
		try {
			lastplaydate = df.parse(String.valueOf(audioItem.getSort()));
			//textView3.setText(lastplaydate.toLocaleString());
			textView3.setText(DateUtils.getRelativeTimeSpanString(lastplaydate.getTime()));
			
		} catch (ParseException e) {
			// TODO é©ìÆê∂ê¨Ç≥ÇÍÇΩ catch ÉuÉçÉbÉN
			//e.printStackTrace();
		
		}
		
		
		return rowView;
	}
}
