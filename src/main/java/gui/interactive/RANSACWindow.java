package gui.interactive;

import java.awt.Button;
import java.awt.Choice;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Label;
import java.awt.Scrollbar;
import java.awt.TextField;

import gui.interactive.InteractiveRadialSymmetry.ValueChange;
import parameters.GUIParams;

public class RANSACWindow
{
	final InteractiveRadialSymmetry parent;
	final Frame ransacFrame;

	public RANSACWindow( final InteractiveRadialSymmetry parent )
	{
		this.parent = parent;
		ransacFrame = new Frame("Adjust RANSAC Values");
		ransacFrame.setSize(260, 260);

		/* Instantiation */
		final GridBagLayout layout = new GridBagLayout();
		final GridBagConstraints gbc= new GridBagConstraints();

		int scrollbarInitialPosition = HelperFunctions.computeScrollbarPositionFromValue(
				parent.params.getSupportRadius(),
				parent.supportRadiusMin,
				parent.supportRadiusMax,
				parent.scrollbarSize);

		final Scrollbar supportRegionScrollbar = new Scrollbar(Scrollbar.HORIZONTAL, scrollbarInitialPosition, 10, 0,
				10 + parent.scrollbarSize);

		// final TextField SupportRegionTextField = new TextField(Integer.toString(parent.params.getSupportRadius()));
		// SupportRegionTextField.setEditable(true);
		// SupportRegionTextField.setCaretPosition(Integer.toString(parent.params.getSupportRadius()).length());

		scrollbarInitialPosition = HelperFunctions.computeScrollbarPositionFromValue(
				parent.params.getInlierRatio(),
				parent.inlierRatioMin,
				parent.inlierRatioMax,
				parent.scrollbarSize);
		final Scrollbar inlierRatioScrollbar = new Scrollbar(Scrollbar.HORIZONTAL, scrollbarInitialPosition, 10, 0,
				10 + parent.scrollbarSize);

		final float log1001 = (float) Math.log10(parent.scrollbarSize + 1);
		scrollbarInitialPosition = 1001
				- (int) Math.pow(10, (parent.maxErrorMax - parent.params.getMaxError()) / (parent.maxErrorMax - parent.maxErrorMin) * log1001);

		final Scrollbar maxErrorScrollbar = new Scrollbar(Scrollbar.HORIZONTAL, scrollbarInitialPosition, 10, 0,
				10 + parent.scrollbarSize);

		final Label supportRegionText = new Label(
				"Support Region Radius = " + parent.params.getSupportRadius(), Label.CENTER);
		final Label inlierRatioText = new Label(
				"Inlier Ratio = " + String.format(java.util.Locale.US, "%.2f", parent.params.getInlierRatio()), Label.CENTER);
		final Label maxErrorText = new Label("Max Error = " + String.format(java.util.Locale.US, "%.4f", parent.params.getMaxError()),
				Label.CENTER);

		final Label bsText = new Label("Local Background Subtraction:", Label.CENTER);
		final Choice bsMethodChoice = new Choice();
		for ( final String s : GUIParams.bsMethods )
			bsMethodChoice.add( s );

		final Button button = new Button("Done");
		final Button cancel = new Button("Cancel");

		// /* Location */
		ransacFrame.setLayout(layout);

		// insets constants
		int inTop = 0;
		int inRight = 5;
		int inBottom = 0;
		int inLeft = inRight;

		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.gridx = 0;
		gbc.gridy = 0;
		// gbc.weightx = 0.50;
//		gbc.gridwidth = 1;
		ransacFrame.add(supportRegionText, gbc);

//		gbc.gridx = 1;
//		gbc.weightx = 0.50;
//		gbc.gridwidth = 1;
//		gbc.insets = new Insets(inTop, inLeft, inBottom, inRight);
//		ransacFrame.add(SupportRegionTextField, gbc);

		gbc.gridx = 0;
		gbc.gridy = 1;
// 		gbc.gridwidth = 2;
		gbc.insets = new Insets(5, inLeft, inBottom, inRight);
		ransacFrame.add(supportRegionScrollbar, gbc);

		gbc.gridx = 0;
		gbc.gridy = 2;
//		gbc.gridwidth = 2;
		ransacFrame.add(inlierRatioText, gbc);

		gbc.gridx = 0;
		gbc.gridy = 3;
//		gbc.gridwidth = 2;
		gbc.insets = new Insets(inTop, inLeft, inBottom, inRight);
		ransacFrame.add(inlierRatioScrollbar, gbc);

		gbc.gridx = 0;
		gbc.gridy = 4;
//		gbc.gridwidth = 2;
		ransacFrame.add(maxErrorText, gbc);

		gbc.gridx = 0;
		gbc.gridy = 5;
//		gbc.gridwidth = 2;
		gbc.insets = new Insets(inTop, inLeft, inBottom, inRight);
		ransacFrame.add(maxErrorScrollbar, gbc);

		++gbc.gridy;
//		gbc.gridwidth = 2;
		gbc.insets = new Insets(inTop+5, inLeft, inBottom, inRight);
		ransacFrame.add(bsText, gbc);

		++gbc.gridy;
//		gbc.gridwidth = 2;
		gbc.insets = new Insets(inTop, inLeft, inBottom, inRight);
		ransacFrame.add(bsMethodChoice, gbc);

		++gbc.gridy;
		gbc.insets = new Insets(5, 50, 0, 50);
		ransacFrame.add(button, gbc);

		++gbc.gridy;
		gbc.insets = new Insets(0, 50, 0, 50);
		ransacFrame.add(cancel, gbc);
		
		/* Screen positioning */
		int xOffset = 20; 
		int yOffset = 230;
		ransacFrame.setLocation(xOffset, yOffset);

		// /* Configuration */
		supportRegionScrollbar.addAdjustmentListener(new GeneralListener(parent, supportRegionText, parent.supportRadiusMin,
				parent.supportRadiusMax, ValueChange.SUPPORTRADIUS, new TextField()));
		inlierRatioScrollbar.addAdjustmentListener(new GeneralListener(parent,inlierRatioText, parent.inlierRatioMin, parent.inlierRatioMax,
				ValueChange.INLIERRATIO, new TextField()));
		maxErrorScrollbar.addAdjustmentListener(
				new GeneralListener(parent, maxErrorText, parent.maxErrorMin, parent.maxErrorMax, ValueChange.MAXERROR, new TextField()));

		// SupportRegionTextField.addActionListener(new TextFieldListener(parent,supportRegionText, parent.supportRadiusMin,
		// 		parent.supportRadiusMax, ValueChange.SUPPORTRADIUS, SupportRegionTextField, supportRegionScrollbar));

		bsMethodChoice.addItemListener( new BackgroundRANSACListener( parent ) );

		button.addActionListener(new FinishedButtonListener(parent, false));
		cancel.addActionListener(new FinishedButtonListener(parent, true));

		ransacFrame.addWindowListener(new FrameListener(parent));
	}

	public Frame getFrame() { return ransacFrame; }
}
