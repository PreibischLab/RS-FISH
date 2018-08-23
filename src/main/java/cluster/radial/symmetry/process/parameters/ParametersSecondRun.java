package cluster.radial.symmetry.process.parameters;

import radial.symmetry.parameters.GUIParams;

public class ParametersSecondRun {
	
	public static GUIParams getN2ParametersSecondRun(int lambda) {
		// set the parameters according to the wavelength, not the label (gene)
		final GUIParams params = new GUIParams();
		if (lambda == 670) {
			params.setMultipleParams(1.5f, 0.0081f, 3, 0.37f, 0.5034f, 1.08f, true);
		} else if(lambda == 610){
			params.setMultipleParams(1.5f, 0.0081f, 3, 0.37f, 0.5034f, 1.08f, true);
		} else if(lambda == 570){
			params.setMultipleParams(1.5f, 0.0081f, 3, 0.37f, 0.5034f, 1.08f, true);
		} else {
			System.out.println("This is the wave length value that you didn't use before. Check the results carefully!");
		}
		return params;
	}
	
	public static GUIParams getSEA12ParametersSecondRun(int lambda) {
		// set the parameters according to the wavelength, not the label (gene)
		final GUIParams params = new GUIParams();
		if (lambda == 670) {
			params.setMultipleParams(1.5f, 0.0081f, 3, 0.37f, 0.5034f, 1.08f, true);
		} else if(lambda == 610){
			params.setMultipleParams(1.5f, 0.0081f, 3, 0.37f, 0.5034f, 1.08f, true);
		} else if(lambda == 570){
			params.setMultipleParams(1.5f, 0.0081f, 3, 0.37f, 0.5034f, 1.08f, true);
		} else {
			System.out.println("This is the wave length value that you didn't use before. Check the results carefully!");
		}
		return params;
	}
}
