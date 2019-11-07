package gui.vizualization;

import fiji.tool.SliceListener;
import ij.ImagePlus;
import mpicbg.imglib.multithreading.SimpleMultiThreading;
import visualization.Detections;

public class ImagePlusListener implements SliceListener
{
	final Detections parent;

	public ImagePlusListener( final Detections parent )
	{
		this.parent = parent;
	}

	@Override
	public void sliceChanged(ImagePlus arg0) {
		if (parent.isStarted()) {
			while (parent.isComputing()) {
				SimpleMultiThreading.threadWait(10);
			}
			parent.updatePreview(parent.getThreshold()); // TODO: better way to do this ? 
		}
	}
}
