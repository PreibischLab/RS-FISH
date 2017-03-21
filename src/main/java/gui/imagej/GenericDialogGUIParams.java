package gui.imagej;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GenericDialog;
import parameters.GUIParams;

public class GenericDialogGUIParams 
{
	final GUIParams guiParams;

	// used to save previous values of the fields
	public static String[] paramChoice = new String[] { "Manual", "Interactive" };
	public static int defaultImg = 0;
	public static int defaultParam = 1;
	public static boolean defaultGauss = false;
	
	// user chosen values
	ImagePlus imp;
	int parameterType;
	boolean gaussFit;

	
	
	public GenericDialogGUIParams( final GUIParams guiParams ) {
		this.guiParams = guiParams;
		GenericDialog gui;
	}
	
	
	public void setDefaultValues(){
		defaultParam = parameterType;
		defaultGauss = gaussFit;
		
	}
	
	// TODO: POLISH
	/**
	 * shows the initial GUI dialog 
	 * user has to choose 
	 * an image 
	 * a processing method -- advanced/interactive
	 * */
	protected boolean initialDialog(){
		boolean failed = false;
		// check that the are images
		final int[] imgIdList = WindowManager.getIDList();
		if (imgIdList == null || imgIdList.length < 1)
		{
			IJ.error("You need at least one open image.");
			failed = true;
		}
		else
		{
			// titles of the images		
			final String[] imgList = new String[imgIdList.length];
			for (int i = 0; i < imgIdList.length; ++i)
				imgList[i] = WindowManager.getImage(imgIdList[i]).getTitle();

			// choose image to process and method to use
			GenericDialog initialDialog = new GenericDialog("Initial Setup");

			if (defaultImg >= imgList.length)
				defaultImg = 0;

			initialDialog.addChoice("Image_for_detection", imgList, imgList[defaultImg]);
			initialDialog.addChoice("Define_Parameters", paramChoice, paramChoice[defaultParam]);
			initialDialog.addCheckbox("Do_additional_gauss_fit", defaultGauss);
			initialDialog.showDialog();

			if ( initialDialog.wasCanceled() )
			{
				failed = true;
			}
			else
			{
				// Save current index and current choice here 
				int tmp = defaultImg = initialDialog.getNextChoiceIndex();
				this.imp = WindowManager.getImage( imgIdList[ tmp ] );
				this.parameterType = defaultParam = initialDialog.getNextChoiceIndex();
				this.gaussFit = defaultGauss = initialDialog.getNextBoolean();
			}
		}

		return failed;
	}

	
	
}
