package gui.csv.overlay;

import fiji.tool.SliceListener;
import ij.ImagePlus;
import mpicbg.imglib.multithreading.SimpleMultiThreading;

public class ImagePlusListener implements SliceListener
{
	final CsvOverlay parent;

	public ImagePlusListener( final CsvOverlay parent )
	{
		this.parent = parent;
	}

	@Override
	public void sliceChanged(ImagePlus imp) {
		if (parent.isStarted()) 
			parent.updatePreview(); 
	}
}