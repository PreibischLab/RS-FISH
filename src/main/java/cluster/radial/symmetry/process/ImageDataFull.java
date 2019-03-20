
package cluster.radial.symmetry.process;

import java.util.HashMap;

public class ImageDataFull {
//contains the image parameters 

	// public int lambda; // wave length
	// public String type; // stain type
	public boolean defects; // is this a good image or bad
	public String filename; // without extension

	public HashMap<String, String> channels; // contains the information about all
																						// channels for the specific image

//	public ImageDataFull(){
//		lambda = 0;
//		type = "";
//		defects = false;
//		filename = "";
//		center = 1;
//		channels = new HashMap<>();
//	}

//	public ImageDataFull(int lambda, String type, boolean defects, String filename, HashMap<String, String> channels){
//		this.lambda = lambda;
//		this.type = type;
//		this.defects = defects;
//		this.filename = filename;
//		this.center = 1;
//		this.channels = channels;
//	}

	public ImageDataFull(boolean defects, String filename,
		HashMap<String, String> channels)
	{
		this.defects = defects;
		this.filename = filename;
		this.channels = channels;
	}

//	public ImageDataFull(String filename, float center){
//		this.lambda = 0;
//		this.type = "";
//		this.defects = false;
//		this.filename = filename;
//		this.center = center;
//	}

	public boolean getDefects() {
		return defects;
	}

	public String getFilename() {
		return filename;
	}

	public HashMap<String, String> getChannels() {
		return channels;
	}

	public String getChannel(String channel) {
		return channels.get(channel);
	}

}
