
package cluster.radial.symmetry.process;

public class ImageData {
	// contains the image parameters

	public int lambda; // wave length
	public String type; // stain type
	public boolean defects; // is this a good image or bad
	public String filename; // without extension
	public float center; // used for the second run to store the center of the
												// fitted peak

	public ImageData() {
		lambda = 0;
		type = "";
		defects = false;
		filename = "";
		center = 1;
	}

	public ImageData(int lambda, String type, boolean defects, String filename) {
		this.lambda = lambda;
		this.type = type;
		this.defects = defects;
		this.filename = filename;
		this.center = 1;
	}

	public ImageData(int lambda, String type, boolean defects, String filename,
		float center)
	{
		this.lambda = lambda;
		this.type = type;
		this.defects = defects;
		this.filename = filename;
		this.center = center;
	}

	public ImageData(String filename, float center) {
		this.lambda = 0;
		this.type = "";
		this.defects = false;
		this.filename = filename;
		this.center = center;
	}

	public int getLambda() {
		return lambda;
	}

	public String getType() {
		return type;
	}

	public boolean getDefects() {
		return defects;
	}

	public String getFilename() {
		return filename;
	}

	public float getCenter() {
		return center;
	}
}
