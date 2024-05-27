/*-
 * #%L
 * A plugin for radial symmetry localization on smFISH (and other) images.
 * %%
 * Copyright (C) 2016 - 2024 RS-FISH developers.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */
package gui.interactive;

import java.awt.Label;
import java.awt.TextField;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;

import gui.interactive.InteractiveRadialSymmetry.ValueChange;
import mpicbg.imglib.multithreading.SimpleMultiThreading;

// general listener used by ransac
public class GeneralListener implements AdjustmentListener {
	final InteractiveRadialSymmetry parent;
	final Label label;
	final TextField textField;
	final float min, max;
	final ValueChange valueAdjust;

	public GeneralListener(
			final InteractiveRadialSymmetry parent,
			final Label label, final float min, final float max, ValueChange valueAdjust,
			TextField textField) {
		this.parent = parent;
		this.label = label;
		this.min = min;
		this.max = max;
		this.valueAdjust = valueAdjust;
		this.textField = textField;
	}

	@Override
	public void adjustmentValueChanged(final AdjustmentEvent event) {
		float value = HelperFunctions.computeValueFromScrollbarPosition(event.getValue(), min, max, parent.scrollbarSize);
		String labelText = "";
		if (valueAdjust == ValueChange.SUPPORTRADIUS) {
			parent.params.setSupportRadius((int) value);
			labelText = "Support Region Radius = " + parent.params.getSupportRadius() ;
			// textField.setText(Integer.toString( parent.params.getSupportRadius() ));
			// labelText = "Inlier Ratio = " + String.format(java.util.Locale.US, "%.2f", parent.params.getInlierRatio());
		} else if (valueAdjust == ValueChange.INLIERRATIO) {
			parent.params.setInlierRatio(value);
			// this is ugly fix of the problem when inlier's ratio is 1.0
			if (parent.params.getInlierRatio() >= 0.999)
				parent.params.setInlierRatio(0.99999f);
			labelText = "Inlier Ratio = " + String.format(java.util.Locale.US, "%.2f", parent.params.getInlierRatio());
		} else if (valueAdjust == ValueChange.MAXERROR) { // MAXERROR
			final float log1001 = (float) Math.log10(1001);
			value = min + ((log1001 - (float) Math.log10(1001 - event.getValue())) / log1001) * (max - min);
			parent.params.setMaxError(value);
			labelText = "Max Error = " + String.format(java.util.Locale.US, "%.4f", parent.params.getMaxError());
		} else if (valueAdjust == ValueChange.BSMAXERROR) { // BACKGROUND MAXERROR
			final float log1001 = (float) Math.log10(1001);
			value = min + ((log1001 - (float) Math.log10(1001 - event.getValue())) / log1001) * (max - min);
			parent.params.setBsMaxError(value);
			labelText = "Max Error = " + String.format(java.util.Locale.US, "%.4f", parent.params.getBsMaxError());
		} else if (valueAdjust == ValueChange.BSINLIERRATIO){ // BACKGROUND INLIER RATIO
			parent.params.setBsInlierRatio(value);
			// this is ugly fix of the problem when inlier's ratio is 1.0
			if (parent.params.getBsInlierRatio() >= 0.999)
				parent.params.setBsInlierRatio(0.99999f);
			labelText = "Inlier Ratio = " + String.format(java.util.Locale.US, "%.2f", parent.params.getBsInlierRatio());
		} else {
			System.out.println("Attached GeneralListener to the wrong scrollbar");
		}
		label.setText(labelText);
		if (!parent.isComputing) {
			parent.updatePreview(valueAdjust);
		} else if (!event.getValueIsAdjusting()) {
			while (parent.isComputing) {
				SimpleMultiThreading.threadWait(10);
			}
			parent.updatePreview(valueAdjust);
		}
	}
}
