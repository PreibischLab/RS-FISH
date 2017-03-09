package gui;

import java.awt.Label;
import java.awt.Scrollbar;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;

import gui.InteractiveRadialSymmetry.ValueChange;
import mpicbg.imglib.multithreading.SimpleMultiThreading;

public class SigmaListener implements AdjustmentListener {
	final InteractiveRadialSymmetry parent;
	final Label label;
	final float min, max;
	final int scrollbarSize;

	final Scrollbar sigmaScrollbar1;
	// final Scrollbar sigmaScrollbar2;
	// final Label sigmaText2;

	public SigmaListener(
			final InteractiveRadialSymmetry parent,
			final Label label, final float min, final float max,
			final int scrollbarSize,
			final Scrollbar sigmaScrollbar1) {
		this.parent = parent;
		this.label = label;
		this.min = min;
		this.max = max;
		this.scrollbarSize = scrollbarSize;

		this.sigmaScrollbar1 = sigmaScrollbar1;
		// this.sigmaScrollbar2 = sigmaScrollbar2;
		// this.sigmaText2 = sigmaText2;
	}

	@Override
	public void adjustmentValueChanged(final AdjustmentEvent event) {
		parent.sigma = HelperFunctions.computeValueFromScrollbarPosition(event.getValue(), min, max, scrollbarSize);
		parent.sigma2 = HelperFunctions.computeSigma2(parent.sigma, parent.sensitivity);

		// TODO: this might never be the case
		if (parent.sigma > parent.sigma2) {
			parent.sigma = parent.sigma2 - 0.001f;
			sigmaScrollbar1.setValue(HelperFunctions.computeScrollbarPositionFromValue(parent.sigma, min, max, scrollbarSize));
		}

		label.setText("Sigma 1 = " + String.format(java.util.Locale.US, "%.2f", parent.sigma));

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
