package net.finalstream.linearaudioplayer.bean;

public class ArtistBean {
	
	private int count;
	private String artist;
	
	public int getCount() {
		return count;
	}

	public void setCount(int count) {
		this.count = count;
	}

	public String getArtist() {
		return artist;
	}

	public void setArtist(String artist) {
		this.artist = artist;
	}
	
	@Override
	public String toString() {

		if (count == 0) {
			return "";
		}
		return artist + "(" + count +  " items)";
	}
	
}
