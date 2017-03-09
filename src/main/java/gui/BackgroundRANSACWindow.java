package gui;

import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Label;
import java.awt.Scrollbar;
import java.awt.TextField;

import gui.InteractiveRadialSymmetry.ValueChange;

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
				parent.bsInlierRatio,
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
						(parent.bsMaxErrorMax - parent.bsMaxError) /
						(parent.bsMaxErrorMax - parent.bsMaxErrorMin) * log1001);

		final Scrollbar maxErrorScrollbar = new Scrollbar(
				Scrollbar.HORIZONTAL,
				scrollbarBSInitialPosition, 10, 0,
				10 + parent.scrollbarSize);

		final Label bsInlierRatioText = new Label(
				"Inlier Ratio = " + String.format(java.util.Locale.US, "%.2f", parent.bsInlierRatio), Label.CENTER);

		final Label bsMaxErrorText = new Label("Max Error = " + String.format(java.util.Locale.US, "%.4f", parent.bsMaxError),
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
