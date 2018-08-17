package cluster.radial.symmetry.process;


public class ImageData {
	// contains the image parameters 
	
	public int lambda; // wave length
	public boolean defects; // is this a good image or bad
	public String filename; // without extension
	public float center; // used for the second run to store the center of the fitted peak
	
	ImageData(){
		lambda = 0;
		defects = false;
		filename = "";
		center = 1;
	}
	
	ImageData(int lambda, boolean defects, String filename){
		this.lambda = lambda;
		this.defects = defects;
		this.filename = filename;
		this.center = 1;
	}
	
	ImageData(int lambda, boolean defects, String filename, float center){
		this.lambda = lambda;
		this.defects = defects;
		this.filename = filename;
		this.center = center;
	}
	
	ImageData(String filename, float center){
		this.lambda = 0;
		this.defects = false;
		this.filename = filename;
		this.center = center;
	}
	
	int getLambda() {
		return lambda;
	}
	
	boolean getDefects() {
		return defects;
	}
	
	String getFilename() {
		return filename;
	}
	
	float getCenter() {
		return center;
	}
}
