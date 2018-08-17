package gui.anisotropy;

import java.awt.Label;
import java.awt.Scrollbar;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;

import gui.anisotropy.AnisitropyCoefficient.ValueChange;
import gui.radial.symmetry.interactive.HelperFunctions;
import mpicbg.imglib.multithreading.SimpleMultiThreading;

public class SigmaListener implements AdjustmentListener {
	final AnisitropyCoefficient parent;
	final Label label;
	final float min, max;
	final int scrollbarSize;

	final Scrollbar sigmaScrollbar1;

	public SigmaListener(
			final AnisitropyCoefficient parent,
			final Label label, final float min, final float max,
			final int scrollbarSize,
			final Scrollbar sigmaScrollbar1) {
		this.parent = parent;
		this.label = label;
		this.min = min;
		this.max = max;
		this.scrollbarSize = scrollbarSize;

		this.sigmaScrollbar1 = sigmaScrollbar1;
	}

	@Override
	public void adjustmentValueChanged(final AdjustmentEvent event) {
		float sigmaDog = HelperFunctions.computeValueFromScrollbarPosition(event.getValue(), min, max, scrollbarSize);
		parent.params.setSigmaDog(sigmaDog);
		label.setText("Sigma 1 = " + String.format(java.util.Locale.US, "%.2f", parent.params.getSigmaDoG()));

		// Real time change of the radius
		// if ( !event.getValueIsAdjusting() )
		{
			while (parent.isComputing) {
				SimpleMultiThreading.threadWait(10);
			}
			parent.updatePreview(ValueChange.SIGMA);
		}
	}
}
