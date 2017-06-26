package gui.imagej;

import java.awt.Choice;
import java.awt.Label;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GenericDialog;
import parameters.GUIParams;

// used for the advanced option of the initial dialog
public class GenericDialogGUIParams 
{
	// TODO: This one use useless
	final GUIParams guiParams;
		
	public GenericDialogGUIParams( final GUIParams guiParams ) {
		this.guiParams = guiParams;
		GenericDialog gui;
	}
	
	public boolean automaticDialog(){
		boolean canceled = false;

		GenericDialog gd = new GenericDialog("Set Stack Parameters");

		gd.addNumericField("Sigma:", GUIParams.defaultSigma, 2);
		gd.addNumericField("Threshold:", GUIParams.defaultThreshold, 4);
		gd.addNumericField("Support_Region_Radius:", GUIParams.defaultSupportRadius, 0);
		gd.addNumericField("Inlier_Ratio:", GUIParams.defaultInlierRatio, 2);
		gd.addNumericField("Max_Error:", GUIParams.defaultMaxError, 2);

		gd.addChoice("Local_Background_Subtraction:", GUIParams.bsMethods, GUIParams.bsMethods[GUIParams.defaultBsMethod] );
		
		gd.showDialog();
		if (gd.wasCanceled()) 
			canceled = true;
		else{
			// TODO: check if the values are numbers 
			float sigma = (float)gd.getNextNumber();
			float threshold = (float)gd.getNextNumber();
			int supportRadius = (int)Math.round(gd.getNextNumber());
			float inlierRatio = (float)gd.getNextNumber();
			float maxError = (float)gd.getNextNumber();	
			int bsMethod = gd.getNextChoiceIndex();
			
			// wrong values in the fields
			if (sigma == Double.NaN || threshold == Double.NaN ||  supportRadius == Double.NaN || inlierRatio == Double.NaN || maxError == Double.NaN )
				canceled = true;
			else{
				// set the parameters
				guiParams.setSigmaDog(sigma);
				guiParams.setThresholdDoG(threshold);
				guiParams.setSupportRadius(supportRadius);
				guiParams.setInlierRatio(inlierRatio);
				guiParams.setMaxError(maxError);
				guiParams.setBsMethod(bsMethod);
				
				// the default values are set in the Radial_Symmetry.java
			}
		}
				
		return canceled;
		}
}
