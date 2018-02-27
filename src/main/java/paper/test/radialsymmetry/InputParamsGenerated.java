package paper.test.radialsymmetry;

import parameters.GUIParams;
import parameters.RadialSymmetryParameters;

public class InputParamsGenerated {
	// stores input parameters for images and csv files
	public  String path;
	public  String imgName;
	public  String posName;
	public  long [] dims;
	public  double [] sigma; 
	public  int numDimensions;
	public  long numSpots;
	public  int seed;
	public  boolean padding;

	// DoG parameters
	public  float sigmaDog; 
	public  float threshold;

	// RANSAC parameters
	public  boolean RANSAC;
	public  int supportRadius; // this one I know
	public  float maxError; 
	public  float inlierRatio;

	// Background Subtraction parameters
	public  float bsMaxError;
	public float bsInlierRatio;
	public  String bsMethod;

	// Gauss Fit over intensities
	public  boolean gaussFit;
	
	// 
	RadialSymmetryParameters rsm;
	
	public InputParamsGenerated(String localPath) {
		// input data params
		path = localPath.equals("") ? "/Users/kkolyva/Desktop/2018-02-21-paper-radial-symmetry-test/" : localPath;
		imgName = "test-2D-image";
		posName = "test-2D-pos";
		dims = new long[] {512, 512};
		sigma = new double[] {3, 3}; 
		numDimensions = dims.length;
		numSpots = 800;
		seed = 42;
		padding = false;

		// radial symmetry params
		// this parameters should come from the manual adjustment
		// DoG parameters
		sigmaDog = 1.5f; 
		threshold = 0.015f;

		// RANSAC parameters
		RANSAC = true;
		supportRadius = 3; // this one I know
		maxError = 0.3f; 
		inlierRatio = 0.60f;

		// Background Subtraction parameters
		bsMaxError = 0.05f;
		bsInlierRatio = 0.75f;
		bsMethod = "No background subtraction";

		// Gauss Fit over intensities
		gaussFit = false;
		
		// set the parameters from the defaults
		final GUIParams params = new GUIParams();

		params.setRANSAC(RANSAC);
		params.setMaxError(maxError);
		params.setInlierRatio(inlierRatio);
		params.setSupportRadius(supportRadius);
		params.setBsMaxError(bsMaxError);
		params.setBsInlierRatio(bsInlierRatio);
		params.setBsMethod(bsMethod);
		params.setSigmaDog(sigmaDog);
		params.setThresholdDog(threshold);
		params.setGaussFit(gaussFit);

		// back up the parameter values to the default variables
		params.setDefaultValues();

		double [] calibration  = new double[numDimensions];
		for (int d = 0; d < numDimensions; d++)
			calibration[d] = 1;
		rsm = new RadialSymmetryParameters(params, calibration);
	}
	
	// TODO: should have setters\getters
}
