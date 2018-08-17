package gui.radial.symmetry.interactive;

import fiji.tool.SliceListener;
import gui.radial.symmetry.interactive.InteractiveRadialSymmetry.ValueChange;
import ij.ImagePlus;
import mpicbg.imglib.multithreading.SimpleMultiThreading;

public class ImagePlusListener implements SliceListener
{
	final InteractiveRadialSymmetry parent;

	public ImagePlusListener( final InteractiveRadialSymmetry parent )
	{
		this.parent = parent;
	}

	@Override
	public void sliceChanged(ImagePlus arg0) {
		if (parent.isStarted) {
			// System.out.println("Slice changed!");
			while (parent.isComputing) {
				SimpleMultiThreading.threadWait(10);
			}
			parent.updatePreview(ValueChange.SLICE);
		}
	}
}
