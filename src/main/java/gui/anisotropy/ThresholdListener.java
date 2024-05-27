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
package gui.anisotropy;

import java.awt.Label;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;

import gui.anisotropy.AnisotropyCoefficient.ValueChange;
import mpicbg.imglib.multithreading.SimpleMultiThreading;

public class ThresholdListener implements AdjustmentListener {
	final AnisotropyCoefficient parent;
	final Label label;
	final float min, max;
	final float log1001 = (float) Math.log10(1001);

	public ThresholdListener(
			final AnisotropyCoefficient parent,
			final Label label, final float min, final float max) {
		this.parent = parent;
		this.label = label;
		this.min = min;
		this.max = max;
	}

	@Override
	public void adjustmentValueChanged(final AdjustmentEvent event) {
		float threshold = min + ((log1001 - (float) Math.log10(1001 - event.getValue())) / log1001) * (max - min);
		parent.params.setThresholdDoG(threshold); 
				
		label.setText("Threshold = " + String.format(java.util.Locale.US, "%.4f", parent.params.getThresholdDoG()));

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
