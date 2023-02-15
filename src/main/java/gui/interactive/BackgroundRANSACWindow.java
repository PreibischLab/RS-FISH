/*-
 * #%L
 * A plugin for radial symmetry localization on smFISH (and other) images.
 * %%
 * Copyright (C) 2016 - 2023 RS-FISH developers.
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

import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Label;
import java.awt.Scrollbar;
import java.awt.TextField;

import gui.interactive.InteractiveRadialSymmetry.ValueChange;

public class BackgroundRANSACWindow
{
	final InteractiveRadialSymmetry parent;
	final Frame frame;

	public BackgroundRANSACWindow( final InteractiveRadialSymmetry parent )
	{
		this.parent = parent;

		frame = new Frame( "RANSAC Values Background" );
		frame.setSize(260, 200);

		/* Instantiation */
		final GridBagLayout layout = new GridBagLayout();
		final GridBagConstraints gbc = new GridBagConstraints();

		int scrollbarBSInitialPosition = HelperFunctions.computeScrollbarPositionFromValue(
				parent.params.getBsInlierRatio(),
				parent.bsInlierRatioMin,
				parent.bsInlierRatioMax,
				parent.scrollbarSize );
		
		final Scrollbar bsInlierRatioScrollbar = new Scrollbar(
				Scrollbar.HORIZONTAL,
				scrollbarBSInitialPosition, 10, 0,
				10 + parent.scrollbarSize );

		final float log1001 = (float) Math.log10( parent.scrollbarSize + 1 );
		scrollbarBSInitialPosition = 1001
				- (int) Math.pow(10, 
						(parent.bsMaxErrorMax - parent.params.getBsMaxError()) /
						(parent.bsMaxErrorMax - parent.bsMaxErrorMin) * log1001);

		final Scrollbar maxErrorScrollbar = new Scrollbar(
				Scrollbar.HORIZONTAL,
				scrollbarBSInitialPosition, 10, 0,
				10 + parent.scrollbarSize);

		final Label bsInlierRatioText = new Label(
				"Inlier Ratio = " + String.format(java.util.Locale.US, "%.2f", parent.params.getBsInlierRatio()), Label.CENTER);

		final Label bsMaxErrorText = new Label("Max Error = " + String.format(java.util.Locale.US, "%.4f", parent.params.getBsMaxError()),
				Label.CENTER);

		// /* Location */
		frame.setLayout(layout);

		// insets constants
		int inTop = 0;
		int inRight = 5;
		int inBottom = 0;
		int inLeft = inRight;

		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.weightx = 1;
		frame.add(bsInlierRatioText, gbc);

		++gbc.gridy;
		gbc.insets = new Insets(inTop, inLeft, inBottom, inRight);
		frame.add(bsInlierRatioScrollbar, gbc);
		
		++gbc.gridy;
		frame.add(bsMaxErrorText, gbc);

		++gbc.gridy;
		gbc.insets = new Insets(inTop, inLeft, inBottom, inRight);
		frame.add(maxErrorScrollbar, gbc);

		// /* Configuration */
		bsInlierRatioScrollbar.addAdjustmentListener(
				new GeneralListener(
						parent,
						bsInlierRatioText,
						parent.bsInlierRatioMin,
						parent.bsInlierRatioMax,
						ValueChange.BSINLIERRATIO, new TextField()));

		maxErrorScrollbar.addAdjustmentListener(
				new GeneralListener(
						parent,
						bsMaxErrorText,
						parent.bsMaxErrorMin,
						parent.bsMaxErrorMax, 
						ValueChange.BSMAXERROR, new TextField()));

		frame.addWindowListener(new FrameListener( parent ));
	}

	public Frame getFrame() { return frame; }

	public void setVisible( final boolean visible )
	{
		frame.setVisible( visible );
	}
}
