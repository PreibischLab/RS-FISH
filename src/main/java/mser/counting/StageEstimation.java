package mser.counting;

import java.awt.Color;
import java.io.File;

import net.imglib2.Localizable;
import net.imglib2.algorithm.componenttree.mser.Mser;
import net.imglib2.algorithm.componenttree.mser.MserTree;
import net.imglib2.img.Img;
import net.imglib2.type.numeric.real.FloatType;

import ij.ImageJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.EllipseRoi;
import ij.process.ByteProcessor;
import util.ImgLib2Util;

public class StageEstimation {
	
	ImageStack stack;

	public void visualise( final Mser< FloatType > mser, final Color color, int w, int h) {
		final ByteProcessor byteProcessor = new ByteProcessor( w, h );
		final byte[] pixels = ( byte[] )byteProcessor.getPixels();
		for ( final Localizable l : mser )
		{
			final int x = l.getIntPosition( 0 );
			final int y = l.getIntPosition( 1 );
			pixels[ y * w + x ] = (byte)(255 & 0xff);
		}
		final String label = "" + mser.value();
		stack.addSlice( label, byteProcessor );

		// final EllipseRoi ellipse = createEllipse( mser.mean(), mser.cov(), 3 );
		// ellipse.setStrokeColor( color );
		// ov.add( ellipse );
	}

	public void visualise( final MserTree< FloatType > tree, final Color color, int w, int h)
	{
		for ( final Mser< FloatType > mser : tree )
			visualise( mser, color, w, h);
	}

	/**
	 * Paint ellipse at nsigmas standard deviations
	 * of the given 2D Gaussian distribution.
	 *
	 * @param mean (x,y) components of mean vector
	 * @param cov (xx, xy, yy) components of covariance matrix
	 * @return ImageJ roi
	 */
	public static EllipseRoi createEllipse( final double[] mean, final double[] cov, final double nsigmas )
	{
		final double a = cov[0];
		final double b = cov[1];
		final double c = cov[2];
		final double d = Math.sqrt( a*a + 4*b*b - 2*a*c + c*c );
		final double scale1 = Math.sqrt( 0.5 * ( a+c+d ) ) * nsigmas;
		final double scale2 = Math.sqrt( 0.5 * ( a+c-d ) ) * nsigmas;
		final double theta = 0.5 * Math.atan2( (2*b), (a-c) );
		final double x = mean[ 0 ];
		final double y = mean[ 1 ];
		final double dx = scale1 * Math.cos( theta );
		final double dy = scale1 * Math.sin( theta );
		final EllipseRoi ellipse = new EllipseRoi( x-dx, y-dy, x+dx, y+dy, scale2 / scale1 );
		return ellipse;
	}
	// used to predict the stage of C.elegans embryo
	public void gg() {
		File path = new File ("/Users/kkolyva/Desktop/2018-06-11-10-09-02-test-mser/C3-SEA-12_59.tif");
		Img<FloatType> img = ImgLib2Util.openAs32Bit(path);

		// TODO: check the parameters
		final int delta = 15; // 
		final long minSize = 10; // large enough to filter out the small objects
		final long maxSize = 100*100; // large enough to store the nulclei ? 
		final double maxVar = 0.8; // ???
		final double minDiversity = 0; // ??? 

		final long startTime = System.currentTimeMillis();
		final MserTree< FloatType > treeDarkToBright = MserTree.buildMserTree( img, new FloatType( delta ), minSize, maxSize, maxVar, minDiversity, true );
		System.out.println("Mser takes " + (System.currentTimeMillis() - startTime) / 1000 + " sec");
		// final MserTree< FloatType > treeBrightToDark = MserTree.buildMserTree( img, new FloatType( delta ), minSize, maxSize, maxVar, minDiversity, false );
	
		int numDimensions = img.numDimensions();
		long [] dimensions = new long[numDimensions];
		img.dimensions(dimensions);
		
		
		System.out.println("dims: " + (int) img.dimension( 0 ) + ", "+ (int) img.dimension( 1 ) );
		
		// resulting stack 
		final ImageStack stack = new ImageStack( (int) img.dimension( 0 ), (int) img.dimension( 1 ) );
		this.stack = stack;
		
		visualise(treeDarkToBright, Color.CYAN, (int)dimensions[0], (int)dimensions[1]);
		
		final ImagePlus imp = new ImagePlus("components", stack);
		imp.show();
	}

	public static void main(String [] args) {
		new ImageJ();
		new StageEstimation().gg();
		System.out.println("DOGE!");
	}
}