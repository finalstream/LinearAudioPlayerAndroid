package net.finalstream.linearaudioplayer.commons;

import java.io.File;
import java.io.FilenameFilter;

public class AudioFilenameFilter implements FilenameFilter {

	public boolean accept(File dir, String name) {
		File file = new File(name);
        if(file.isDirectory()){
            return false;
        }
        name= name.toLowerCase();
        return(name.endsWith(".mp3"));
	}

}
