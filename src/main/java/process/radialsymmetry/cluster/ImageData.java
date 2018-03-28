package process.radialsymmetry.cluster;


public class ImageData {
	// contains the image parameters 
	
	int lambda; // wave length
	String filename;
	
	ImageData(){
		lambda = 0;
		filename = "";
	}
	
	ImageData(int lambda, String filename){
		this.lambda = lambda;
		this.filename = filename;
	}
	
	int getLambda() {
		return lambda;
	}
	
	String getFilename() {
		return filename;
	}
}
