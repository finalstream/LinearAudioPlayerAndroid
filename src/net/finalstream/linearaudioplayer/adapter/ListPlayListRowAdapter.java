package net.finalstream.linearaudioplayer.adapter;

import java.io.Serializable;
import java.util.List;

import net.finalstream.linearaudioplayer.LinearAudioPlayer.FILTERING_MODE;
import net.finalstream.linearaudioplayer.bean.AudioItemBean;
import net.finalstream.linearaudioplayer.services.LinearAudioPlayerService;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.RatingBar;
import android.widget.SeekBar;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.TextView;
import net.finalstream.linearaudioplayer.LinearAudioPlayer;
import net.finalstream.linearaudioplayer.R;

public class ListPlayListRowAdapter extends ArrayAdapter<AudioItemBean> implements Serializable {
	
	OnTouchListener listener;
	/**
	 * 
	 */
	private static final long serialVersionUID = 8687908631081954832L;
	private final Activity mContext;
	
	public ListPlayListRowAdapter(Activity context, int resource, int textViewResourceId) {
		super(context, resource, textViewResourceId);
		this.mContext = context;
		final LinearAudioPlayer linear = (LinearAudioPlayer) context;
		listener = new OnTouchListener() {
		    @Override
		    public boolean onTouch(View v, MotionEvent event) {
		        if (event.getAction() == MotionEvent.ACTION_DOWN) {

		        	RatingBar r = (RatingBar) v;
		        	if (r.getRating() == 0.0f) {
		        		r.setRating(0.5f);
		        	} else if (r.getRating() == 0.5f)  {
		        		r.setRating(1.0f);
		        	} else {
		        		r.setRating(0.0f);
		        	}
		        	//r.setTag(R.string.listitem_fromuser,true);
		        	Long targetId = (Long)r.getTag(R.string.listitem_id);
		        	((LinearAudioPlayer) mContext).updateRating(targetId ,r.getRating());
		        	
		        	// アダプター更新
		            for (int i = 0; i < linear.getPlaylistAdapter().getCount(); i++) {
		    			if(linear.getPlaylistAdapter().getItem(i).getId() == targetId){
		    				linear.getPlaylistAdapter().getItem(i).setRating(r.getRating());
		    				if (linear.getLinearAudioPlayerService().getPlayingItem().getId() == targetId) {
		    					//LayoutInflater inflater = linear.getLayoutInflater();
		    			        //View mainView = inflater.inflate(R.layout.activity_linear_audio_player, null);
		    			        // RatingBar
		    			        RatingBar rating = (RatingBar) linear.findViewById(R.id.ratingBarPlaying);
		    			        rating.setRating(r.getRating());
		    				}
		    				break;
		    			}
		    		}
		            
		        	Log.d("LINEAR", "Rating Ontatch.");
		        }
		        return true;
		    }
		};
	}
	
	public View getView(int position, View convertView, ViewGroup parent) {
		View rowView = convertView;
		if (rowView == null) {
			// TODO:　inflaterは遅い疑惑あり。
    		LayoutInflater inflater = this.mContext.getLayoutInflater();
    		rowView = inflater.inflate(R.layout.playlist_item, null);
		}
		AudioItemBean audioItem = this.getItem(position);
		TextView textView1 = (TextView) rowView.findViewById(android.R.id.text1);    		
		textView1.setText(audioItem.getTitle());
		TextView textView2 = (TextView) rowView.findViewById(android.R.id.text2);    		
		textView2.setText(audioItem.getArtist());
		TextView textView3 = (TextView) rowView.findViewById(R.id.text3);    		
		textView3.setText(audioItem.getPlaycount() + "plays");
		
		RatingBar rating1 = (RatingBar) rowView.findViewById(R.id.ratingBar1);
		
		rating1.setOnTouchListener(listener);
		
		/*
		rating1.setOnRatingBarChangeListener(new RatingBar.OnRatingBarChangeListener() {
		
			@Override
			public void onRatingChanged(RatingBar ratingBar, float rating,
					boolean fromUser) {
				if ((Boolean) ratingBar.getTag(R.string.listitem_fromuser)) {	
					((LinearAudioPlayer) context).updateRating((Long)ratingBar.getTag(R.string.listitem_id) ,rating);
				}
				Log.d("LINEAR", "Rating OnRatingChange." + (Boolean) ratingBar.getTag(R.string.listitem_fromuser));
			}
		});*/
		
		
		
		
		
		rating1.setTag(R.string.listitem_id, audioItem.getId());
		//rating1.setTag(R.string.listitem_fromuser,false);
		rating1.setRating(audioItem.getRating());

		return rowView;
	}
	
}
