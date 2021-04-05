package gui.anisotropy;

import fiji.tool.SliceListener;
import gui.anisotropy.AnisotropyCoefficient.ValueChange;
import ij.ImagePlus;
import mpicbg.imglib.multithreading.SimpleMultiThreading;

public class ImagePlusListener implements SliceListener
{
	final AnisotropyCoefficient parent;

	public ImagePlusListener( final AnisotropyCoefficient parent )
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
