package process.radialsymmetry.cluster;


public class ImageData {
	// contains the image parameters 
	
	int lambda; // wave length
	boolean defects; // is this a good image or bad
	String filename; // without extension
	
	ImageData(){
		lambda = 0;
		defects = false;
		filename = "";
	}
	
	ImageData(int lambda, boolean defects, String filename){
		this.lambda = lambda;
		this.defects = defects;
		this.filename = filename;
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
}
