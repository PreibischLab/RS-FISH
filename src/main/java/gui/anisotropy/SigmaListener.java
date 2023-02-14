/*-
 * #%L
 * A plugin for radial symmetry localization on smFISH (and other) images.
 * %%
 * Copyright (C) 2016 - 2023 Developers.
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
import java.awt.Scrollbar;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;

import gui.anisotropy.AnisotropyCoefficient.ValueChange;
import gui.interactive.HelperFunctions;
import mpicbg.imglib.multithreading.SimpleMultiThreading;

public class SigmaListener implements AdjustmentListener {
	final AnisotropyCoefficient parent;
	final Label label;
	final float min, max;
	final int scrollbarSize;

	final Scrollbar sigmaScrollbar1;

	public SigmaListener(
			final AnisotropyCoefficient parent,
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
