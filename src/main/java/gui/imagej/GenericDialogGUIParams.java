package gui.imagej;

import java.awt.Choice;
import java.awt.Label;

import org.scijava.Cancelable;
import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.command.ContextCommand;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GenericDialog;
import parameters.GUIParams;

// used for the advanced option of the initial dialog
@Plugin(type = Command.class, name="LUL!")
public class GenericDialogGUIParams extends ContextCommand
{
	// TODO: This one use useless
	@Parameter(type=ItemIO.INPUT)
	private GUIParams guiParams;
		
	@Parameter(label="Sigma")
	float sigma = GUIParams.defaultSigma;
	@Parameter(label="Threshold")
	float threshold = GUIParams.defaultThreshold;
	
	@Parameter(label="Support region radius")
	int supportRadius = GUIParams.defaultSupportRadius;
	
	@Parameter(label="Inlier ratio")
	float inlierRatio = GUIParams.defaultInlierRatio;
	@Parameter(label="Max error")
	float maxError = GUIParams.defaultMaxError;
	
	@Parameter(label="Local background subtraction")
	String bsMethod = GUIParams.defaultBsMethod;
	
	@Parameter(label="Cancel?")
	private boolean wasCanceled = false;
	
	@Override
	public void run() {
		if (!wasCanceled) {
			guiParams.setSigmaDog(sigma);
			guiParams.setThresholdDoG(threshold);
			guiParams.setSupportRadius(supportRadius);
			
			guiParams.setInlierRatio(inlierRatio);
			guiParams.setMaxError(maxError);
			guiParams.setBsMethod(bsMethod);
		}
	}
}
