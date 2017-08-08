package gui.anisotropy.plugin;

import java.io.File;

import anisotropy.parameters.AParams;
import fiji.util.gui.GenericDialogPlus;
import gui.Radial_Symmetry;
import gui.anisotropy.AnisitropyCoefficient;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.io.Opener;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;

public class Anisotropy_Plugin implements PlugIn {
	
	@Override
	public void run(String arg) {
		double bestScale = anisotropyChooseImageDialog();
	}
	
	protected double anisotropyChooseImageDialog(){

		// boolean failed = false;
		double bestScale;

		GenericDialogPlus gdp = new GenericDialogPlus("Choose bead image");
		gdp.addFileField("Image", "/Volumes/Samsung_T3/2017-08-07-stephan-radial-symmetry-pipeline/psf.tif");

		// TODO: Check that the file is the image		
		// gdp.addDirectoryField(label, defaultPath);
		// gdp.addDirectoryOrFileField(label, defaultPath);		
		gdp.showDialog();

		if (gdp.wasCanceled()){ 
			// failed = true;
			bestScale = 1.0;
		}
		else{
			String imgPath = gdp.getNextString(); // this one should the name of the image
			File file = new File(imgPath);
			ImagePlus imagePlus = new Opener().openImage(file.getAbsolutePath());
			if (!file.exists())
				throw new RuntimeException("'" + file.getAbsolutePath() + "' doesn't exist.");

			imagePlus.show();

			// TODO: remove as a parameter? 
			AParams ap = new AParams();

			double [] minmax = calculateMinMax(imagePlus);
			AnisitropyCoefficient ac = new AnisitropyCoefficient(imagePlus, ap, minmax[0], minmax[1]);
			bestScale = ac.calculateAnisotropyCoefficient();
		}
		return bestScale;
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
		File path = new File( "/media/milkyklim/Samsung_T3/2017-06-26-radial-symmetry-test/Simulated_3D_2x.tif" );
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
