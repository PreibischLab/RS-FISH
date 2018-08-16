package gui.anisotropy.plugin;

import fiji.tool.SliceListener;
import gui.csv.overlay.CsvOverlay;
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
	public void sliceChanged(ImagePlus arg0) {
		if (parent.isStarted()) {
			while (parent.isComputing()) {
				SimpleMultiThreading.threadWait(10);
			}
			
			// TODO: adjust to the use case
			// parent.updatePreview(parent.getSlice()); // TODO: better way to do this ? 
		}
	}
}