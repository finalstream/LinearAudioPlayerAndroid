package net.finalstream.linearaudioplayer.bean;

import java.io.Serializable;

public class AudioItemBean implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -9076149437711742859L;
	private long id;
	private String title;
	private String filePath;
	private String artist;
	private String album;
	private long duration;
	private int year;
	private long sort;
	private float rating = -1.0f;
	private int playcount;
	private long dateadd;
	private float playfreq;
	private long datemodified;
	
	public long getDatemodified() {
		return datemodified;
	}
	public void setDatemodified(long datemodified) {
		this.datemodified = datemodified;
	}
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public String getFilePath() {
		return filePath;
	}
	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}
	public String getArtist() {
		return artist;
	}
	public void setArtist(String artist) {
		this.artist = artist;
	}
	public String getAlbum() {
		return album;
	}
	public void setAlbum(String album) {
		this.album = album;
	}
	public long getDuration() {
		return duration;
	}
	public void setDuration(long duration) {
		this.duration = duration;
	}
	public int getYear() {
		return year;
	}
	public void setYear(int year) {
		this.year = year;
	}
	public long getId() {
		return id;
	}
	public void setId(long id) {
		this.id = id;
	}
	public long getSort() {
		return sort;
	}
	public void setSort(long sort) {
		this.sort = sort;
	}
	public int getPlaycount() {
		return playcount;
	}
	public void setPlaycount(int playcount) {
		this.playcount = playcount;
	}
	public float getRating() {
		return rating;
	}
	public void setRating(float rating) {
		this.rating = rating;
	}
	public long getDateadd() {
		return dateadd;
	}
	public void setDateadd(long dateadd) {
		this.dateadd = dateadd;
	}
	public float getPlayfreq() {
		return playfreq;
	}
	public void setPlayfreq(float playfreq) {
		this.playfreq = playfreq;
	}
	

}
