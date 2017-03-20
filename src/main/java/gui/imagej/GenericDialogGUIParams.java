package gui.imagej;

import ij.gui.GenericDialog;
import parameters.GUIParams;

public class GenericDialogGUIParams extends GUIParams {
	public GenericDialogGUIParams() {
		GenericDialog gui;
	}

	public float sigmaDoG() {
		return defaultSigma;
	}

	public float thresholdDoG() {
		return defaultThreshold;
	}

	public int bsMethod() {
		return defaultBSMethod;
	}

	public float bsMaxErrorRANSAC() {
		return defaultBSMaxError;
	}

	public float bsInlierRatioRANSAC() {
		return defaultBSInlierRatio;
	}

	public float maxErrorRANSAC() {
		return defaultMaxError;
	}

	public float inlierRatioRANSAC() {
		return defaultInlierRatio;
	}

	public int supportRadiusRANSAC() {

		return defaultSupportRadius;
	}

}
