package gui.interactive;

import java.awt.Label;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;

import gui.interactive.InteractiveRadialSymmetry.ValueChange;
import mpicbg.imglib.multithreading.SimpleMultiThreading;

public class ThresholdListener implements AdjustmentListener {
	final InteractiveRadialSymmetry parent;
	final Label label;
	final float min, max;
	final int scrollbarSize;
	final float log1001;

	public ThresholdListener(
			final InteractiveRadialSymmetry parent,
			final Label label,
			final int scrollbarSize,
			final float min,
			final float max) {
		this.parent = parent;
		this.label = label;
		this.scrollbarSize = scrollbarSize;
		this.min = min;
		this.max = max;
		this.log1001 =  (float) Math.log10(scrollbarSize+1)/(float)Math.log10( 2 );
	}

	@Override
	public void adjustmentValueChanged(final AdjustmentEvent event) {
		float threshold = min + ((log1001 - (float) (Math.log10(scrollbarSize + 1 - event.getValue())/Math.log10(2))  ) / log1001) * (max - min);
		parent.params.setThresholdDog(threshold);

		label.setText("Threshold = " + String.format(java.util.Locale.US, "%.5f", parent.params.getThresholdDoG()));

		if (!parent.isComputing) {
			parent.updatePreview(ValueChange.THRESHOLD);
		} else if (!event.getValueIsAdjusting()) {
			while (parent.isComputing) {
				SimpleMultiThreading.threadWait(10);
			}
			parent.updatePreview(ValueChange.THRESHOLD);
		}
	}
}
