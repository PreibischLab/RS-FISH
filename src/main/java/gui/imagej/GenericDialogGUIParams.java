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
		// GenericDialog gui;
	}
	
	public boolean automaticDialog(){
		boolean canceled = false;

		GenericDialog gd = new GenericDialog("Set Stack Parameters");

		gd.addNumericField("Sigma:", GUIParams.defaultSigma, 2);
		gd.addNumericField("Threshold:", GUIParams.defaultThreshold, 4);
		
		gd.addNumericField("Support_Region_Radius:", GUIParams.defaultSupportRadius, 0);
		
		// TODO: Hide for the case of RS without RANSAC
		if (guiParams.getRANSAC()){
			gd.addNumericField("Inlier_Ratio:", GUIParams.defaultInlierRatio, 2);
			gd.addNumericField("Max_Error:", GUIParams.defaultMaxError, 2);
			gd.addChoice("Local_Background_Subtraction:", GUIParams.bsMethods, GUIParams.bsMethods[GUIParams.defaultBsMethod] );
		}
			
		gd.addNumericField("Z-scaling value", GUIParams.defaultAnisotropy, 2);	
		
		gd.showDialog();
		if (gd.wasCanceled()) 
			canceled = true;
		else{
			// TODO: check if the values are numbers 
			float sigma = (float)gd.getNextNumber();
			float threshold = (float)gd.getNextNumber();	
			int supportRadius = (int)Math.round(gd.getNextNumber());
			
			float inlierRatio = 0;
			float maxError = 0.1f;	
			int bsMethod = 0;
			
			if (guiParams.getRANSAC()){	
				inlierRatio = (float)gd.getNextNumber();
				maxError = (float)gd.getNextNumber();	
				bsMethod = gd.getNextChoiceIndex();
			}
			else{
				// TODO: nothing  
				// supportRadius = (int)sigma + 1;
			}
			
			float anisotropyCoefficient = (float)gd.getNextNumber();
			
			// wrong values in the fields
			if (sigma == Double.NaN || threshold == Double.NaN ||  supportRadius == Double.NaN || inlierRatio == Double.NaN || maxError == Double.NaN )
				canceled = true;
			else{
				// set the parameters
				guiParams.setSigmaDog(sigma);
				guiParams.setThresholdDoG(threshold);
				guiParams.setSupportRadius(supportRadius);
				
				// keep old values the same
				if (guiParams.getRANSAC()){	
					guiParams.setInlierRatio(inlierRatio);
					guiParams.setMaxError(maxError);
					guiParams.setBsMethod(bsMethod);
				}		
				
				guiParams.setAnisotropyCoefficient(anisotropyCoefficient);
				
				// the default values are set in the Radial_Symmetry.java
			}
		}
				
		return canceled;
		}
}
