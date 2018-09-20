package gui.csv.overlay;

import java.awt.Label;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;

public class ThresholdListener implements AdjustmentListener{
	final CsvOverlay parent;
	final Label label;
	final float min, max;
	final int scrollbarSize;
	
	public ThresholdListener(CsvOverlay parent, Label label, float min, float max, int scrollbarSize) {
		this.parent = parent;
		this.label = label;
		this.min = min;
		this.max = max;
		this.scrollbarSize = scrollbarSize;
	}
	
	@Override
	public void adjustmentValueChanged(AdjustmentEvent event){
		float value = min + (event.getValue() / (float) scrollbarSize) * (max - min);
		
		parent.setThresholdValue(value);
		label.setText("Intensity = " + String.format(java.util.Locale.US, "%.2f", parent.getThresholdValue()));
			
		parent.updatePreview();
		
//		// TODO: doesn't look like you run into concurrency issues 
//		if (!parent.isComputing) {
//			parent.updatePreview(ValueChange.THRESHOLD);
//		} else if (!event.getValueIsAdjusting()) {
//			while (parent.isComputing) {
//				SimpleMultiThreading.threadWait(10);
//			}
//			parent.updatePreview(ValueChange.THRESHOLD);
//		}
	}
	
}
