package gui.anisotropy.plugin;

import java.io.File;

import anisotropy.parameters.AParams;
import fiji.util.gui.GenericDialogPlus;
import gui.Radial_Symmetry;
import gui.anisotropy.AnisitropyCoefficient;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.io.Opener;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;

public class Anisotropy_Plugin implements PlugIn {

	public static String[] paramChoice = new String[] { "Gauss Fit", "Radial Symmetry" };
	public static int defaultParam = 0;
	public static int defaultImg = 0;

	ImagePlus imagePlus;
	int paramType;

	@Override
	public void run(String arg) {
		boolean wasCanceled = false; 
		wasCanceled = chooseMethodDialog();

		double bestScale = 1.0;


		AParams ap = new AParams();
		double [] minmax = calculateMinMax(imagePlus);
		AnisitropyCoefficient ac = new AnisitropyCoefficient(imagePlus, ap, paramType, minmax[0], minmax[1]);
		
		if (paramType == 0) // gauss fit 
			;//
		else
			bestScale = ac.calculateAnisotropyCoefficientRS();

		// bestScale = anisotropyChooseImageDialog();


	}

	// here user chooses the image and 
	// the method for the calculation of the anisotropy coefficient
	protected boolean chooseMethodDialog(){
		boolean failed = false;
		// check that the are images
		final int[] imgIdList = WindowManager.getIDList();
		if (imgIdList == null || imgIdList.length < 1) {
			IJ.error("You need at least one open image.");
			failed = true;
		}
		else{
			// titles of the images
			final String[] imgList = new String[imgIdList.length];
			for (int i = 0; i < imgIdList.length; ++i)
				imgList[i] = WindowManager.getImage(imgIdList[i]).getTitle();

			if (defaultImg >= imgList.length)
				defaultImg = 0;

			GenericDialog gd = new GenericDialog("Choose the image");
			gd.addChoice("Image_for_detection", imgList, imgList[defaultImg]);
			gd.addChoice("Detection_method", paramChoice, paramChoice[defaultParam]);

			gd.showDialog();

			if (gd.wasCanceled()) {
				failed = true;
			} else {
				int tmp = defaultImg = gd.getNextChoiceIndex();
				this.paramType = defaultParam = gd.getNextChoiceIndex();
				this.imagePlus = WindowManager.getImage(imgIdList[tmp]);
			}
		}

		return failed;
	}

	public static double[] calculateMinMax(ImagePlus imp){
		float min = Float.MAX_VALUE;
		float max = -Float.MAX_VALUE;

		for ( int z = 1; z <= imp.getStack().getSize(); ++z )
		{
			final ImageProcessor ip = imp.getStack().getProcessor( z );

			for ( int i = 0; i < ip.getPixelCount(); ++i )
			{
				final float v = ip.getf( i );
				min = Math.min( min, v );
				max = Math.max( max, v );
			}
		}

		return new double[]{min, max};
	}

	public static void main(String[] args)
	{
		File path = new File( "/Users/kkolyva/Desktop/gauss3d-1,2,3.tif" );
		// path = path.concat("test_background.tif");

		if ( !path.exists() )
			throw new RuntimeException( "'" + path.getAbsolutePath() + "' doesn't exist." );

		new ImageJ();
		System.out.println( "Opening '" + path + "'");

		ImagePlus imp = new Opener().openImage( path.getAbsolutePath() );

		if (imp == null)
			throw new RuntimeException( "image was not loaded" );

		float min = Float.MAX_VALUE;
		float max = -Float.MAX_VALUE;

		for ( int z = 1; z <= imp.getStack().getSize(); ++z )
		{
			final ImageProcessor ip = imp.getStack().getProcessor( z );

			for ( int i = 0; i < ip.getPixelCount(); ++i )
			{
				final float v = ip.getf( i );
				min = Math.min( min, v );
				max = Math.max( max, v );
			}
		}

		IJ.log( "min=" + min );
		IJ.log( "max=" + max );

		imp.show();

		imp.setSlice(20);


		// imp.setRoi(imp.getWidth() / 4, imp.getHeight() / 4, imp.getWidth() / 2, imp.getHeight() / 2);
		// new AnisitropyCoefficient( imp, new AParams(), min, max );
		new Anisotropy_Plugin().run(new String());

		System.out.println("DOGE!");
	}


}
