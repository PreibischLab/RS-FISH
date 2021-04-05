package cmd;

import java.io.File;
import java.util.concurrent.Callable;

import gui.csv.overlay.CsvOverlay;
import ij.ImageJ;
import ij.ImagePlus;
import picocli.CommandLine;
import picocli.CommandLine.Option;

public class OverlayCSV implements Callable<Void> 
{
	// input file
	@Option(names = {"-i", "--image"}, required = true, description = "input image or N5 container path (requires additional -d for N5), e.g. -i /home/smFish.tif or /home/smFish.n5")
	private String image = null;

	@Option(names = {"-d", "--dataset"}, required = false, description = "if you selected an N5 path, you need to define the dataset within the N5, e.g. -d 'embryo_5_ch0/c0/s0'")
	private String dataset = null;

	@Option(names = {"-c", "--csvFile"}, required = true, description = "if you selected an N5 path, you need to define the dataset within the N5, e.g. -d 'embryo_5_ch0/c0/s0'")
	private String csvFile = null;

	@Override
	public Void call() throws Exception
	{
		final ImagePlus imp = RadialSymmetry.open( image, dataset );

		if ( imp == null )
		{
			System.out.println( "Could not open file: " + image  + " (if N5, dataset=" + dataset + ")");
			return null;
		}

		if ( ! new File( csvFile ).exists() )
			throw new RuntimeException( "csvFile does not exist: " + csvFile );

		new ImageJ();

		new CsvOverlay(imp, new File( csvFile ));

		imp.show();

		if ( imp.getStackSize() > 1 )
			imp.setSlice( imp.getStackSize() / 2 );

		imp.resetDisplayRange();

		return null;
	}

	public static final void main(final String... args) {
		new CommandLine( new OverlayCSV() ).execute( args );
	}
}
