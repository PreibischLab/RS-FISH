package gui.anisotropy.plugin;

import java.io.File;

import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import anisotropy.parameters.AParams;
import gui.anisotropy.AnisitropyCoefficient;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.io.Opener;
import ij.process.ImageProcessor;
import parameters.GUIParams;

@Plugin(type = Command.class, menuPath = "Plugins>Radial Symmetry Localization>Calculate Anisotropy Coefficient")
public class Anisotropy_Plugin implements Command {

	public static String[] paramChoice = new String[] { "Gauss Fit", "Radial Symmetry" };
	public static int defaultParam = 0;
	public static int defaultImg = 0;

	@Parameter(autoFill=false, label="Image")
	ImagePlus imagePlus;
	
	@Parameter(choices={ "Gauss Fit", "Radial Symmetry" }, label="Detection method")
	String paramType = paramChoice[defaultParam];

	@Override
	public void run() {
		boolean wasCanceled = false; 
		// wasCanceled = chooseMethodDialog();

		double bestScale = 1.0;
		AParams ap = new AParams();
		if (!wasCanceled){
			
			double [] minmax = calculateMinMax(imagePlus);
			AnisitropyCoefficient ac = new AnisitropyCoefficient(imagePlus, ap, paramType, minmax[0], minmax[1]);
			
			bestScale = ac.calculateAnisotropyCoefficient();	
		}
	
		// TODO: write bestScale somewhere
		ap.setAnisotropy((float)bestScale);
		// TODO: will it work? Should not it be 1/bestScale ?
		GUIParams.defaultAnisotropy = (float) bestScale; 
		IJ.log("Anisotropy coefficient: " + bestScale);		
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
		// File path = new File( "/media/milkyklim/Samsung_T3/2017-08-18-radial-symmetry-test/gauss3d-1,2,3.tif" );
		// File path = new File("/Users/kkolyva/Desktop/beads.tif");
		File path = new File("/Users/kkolyva/Desktop/2017-09-20-hackathon-dresden-projects/2017-09-20-anisotropy-fix/beads-cropped.tif");
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
		new Anisotropy_Plugin().run();

		System.out.println("DOGE!");
	}


}
