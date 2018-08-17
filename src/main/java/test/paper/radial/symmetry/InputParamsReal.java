package test.paper.radial.symmetry;

import radial.symmetry.parameters.GUIParams;
import radial.symmetry.parameters.RadialSymmetryParameters;

public class InputParamsReal {
	// stores input parameters for images and csv files
	public  String path;
	public  String imgName;
	public  int numDimensions;
	
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
	public  float bsInlierRatio;
	public  String bsMethod;

	// Gauss Fit over intensities
	public  boolean gaussFit;
	
	// only applies to the 3D images
	public float anisotropyCoefficient; 
	
	public RadialSymmetryParameters rsm;

	// TODO: 
	
	public InputParamsReal(String localPath, String localImgName, int type) {
		// 0 - 2D max projection
		// 1 - 3D image anisotropic
		// 2 - 3D image anisotropic
		
		if (type == 0) {
			// input data params
			path = localPath.equals("") ? "/Users/kkolyva/Desktop/2018-02-21-paper-radial-symmetry-test/max-project-images/" : localPath;
			imgName = localImgName.equals("") ? "MAX_Result of C3-N2_dpy-23_ex_int_ama-1_014.nd2 - N2_dpy-23_ex_int_ama-1_014.nd2 (series 01).tif" : localImgName;
			numDimensions = 2;
			
			// radial symmetry params
			// this parameters should come from the manual adjustment
			// DoG parameters
			sigmaDog = 1.0f; 
			threshold = 0.0120f;

			// RANSAC parameters
			RANSAC = true;
			supportRadius = 2; // this one I know
			maxError = 0.6f; 
			inlierRatio = 0.5f;

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
		else if(type == 1) {
			// input data params
			path = localPath.equals("") ? "/Users/kkolyva/Desktop/2018-02-21-paper-radial-symmetry-test/max-project-images/" : localPath;
			imgName = localImgName.equals("") ? "MAX_Result of C3-N2_dpy-23_ex_int_ama-1_014.nd2 - N2_dpy-23_ex_int_ama-1_014.nd2 (series 01).tif" : localImgName;
			numDimensions = 3;
			
			// radial symmetry params
			// this parameters should come from the manual adjustment
			// DoG parameters
			sigmaDog = 1.0f; 
			threshold = 0.0120f;

			// RANSAC parameters
			RANSAC = true;
			supportRadius = 2; // this one I know
			maxError = 0.6f; 
			inlierRatio = 0.5f;

			// Background Subtraction parameters
			bsMaxError = 0.05f;
			bsInlierRatio = 0.75f;
			bsMethod = "No background subtraction";

			// Gauss Fit over intensities
			gaussFit = false;
			
			// anisotropy 
			anisotropyCoefficient = 1.0f;
			
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
			params.setAnisotropyCoefficient(anisotropyCoefficient);

			// back up the parameter values to the default variables
			params.setDefaultValues();
			
			double [] calibration  = new double[numDimensions];
			for (int d = 0; d < numDimensions; d++)
				calibration[d] = 1;
			// fix calibration in z0direction 
			calibration[numDimensions - 1] = 1.5384614;
			rsm = new RadialSymmetryParameters(params, calibration);
		}
		else 
			System.out.println("Wrong data type");
	} 
}
