/*-
 * #%L
 * A plugin for radial symmetry localization on smFISH (and other) images.
 * %%
 * Copyright (C) 2016 - 2025 RS-FISH developers.
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
import java.awt.Scrollbar;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.regex.Pattern;

import gui.interactive.InteractiveRadialSymmetry.ValueChange;
import mpicbg.imglib.multithreading.SimpleMultiThreading;

// changes value of the scroller so that it is the same as in the text field
public class TextFieldListener implements ActionListener {
	final InteractiveRadialSymmetry parent;
	final Label label;
	final TextField textField;
	final int min, max;
	final ValueChange valueAdjust;
	final Scrollbar scrollbar;

	public TextFieldListener(
			final InteractiveRadialSymmetry parent,
			final Label label, final int min, final int max, ValueChange valueAdjust,
			TextField textField, Scrollbar scrollbar) {
		this.parent = parent;
		this.label = label;
		this.min = min;
		this.max = max;
		this.valueAdjust = valueAdjust;
		this.textField = textField;
		this.scrollbar = scrollbar;
	}

	// function checks that the textfield contains number
	// add ensures that the number is inside region [min, max]
	public int ensureNumber(String number, int min, int max) {
		boolean isInteger = Pattern.matches("^\\d*$", number);
		int res = -1;
		// TODO: instead of if/else write full try/catch block
		if (isInteger) {
			res = Integer.parseInt(number);
			if (res > max)
				res = max;
			if (res < min)
				res = min;
		} else {
			System.out.println("Not a valid number. Radius set to 10.");
			res = 10;
			// idle
		}
		return res;
	}

	@Override
	public void actionPerformed(final ActionEvent event) {
		// check that the value is in (min, max)
		// adjust and grab value

		int value = ensureNumber(textField.getText(), min, max);

		// System.out.println("value in the text field = " + value);
		String labelText = "";

		if (valueAdjust == ValueChange.SUPPORTRADIUS) {
			// set the value for the support region
			parent.params.setSupportRadius(value);
			// set label
			labelText = "Support Region Radius:"; // = " + supportRegion;
			// calculate new position of the scrollbar
			int newScrollbarPosition = HelperFunctions.computeScrollbarPositionFromValue(
					parent.params.getSupportRadius(), min, max, parent.scrollbarSize);
			// adjust the scrollbar position!
			scrollbar.setValue(newScrollbarPosition);
			// set new value for text label
			label.setText(labelText);
		} else {
			System.out.println("There is error in the support region adjustment");
		}

		while (parent.isComputing) {
			SimpleMultiThreading.threadWait(10);
		}
		parent.updatePreview(ValueChange.SUPPORTRADIUS);
	}
}
