package gui.anisotropy;

import java.awt.Button;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Label;
import java.awt.Scrollbar;

import gui.interactive.HelperFunctions;

public class AnysotropyWindow {

	final AnisitropyCoefficient parent;
	final Frame aFrame; // anisotropy frame 

	public AnysotropyWindow( final AnisitropyCoefficient parent )
	{
		this.parent = parent;
		this.aFrame = new Frame( "Adjust Difference-of-Gaussian Values" );
		aFrame.setSize(360, 150);

		/* Instantiation */
		final GridBagLayout layout = new GridBagLayout();
		final GridBagConstraints c = new GridBagConstraints();

		int scrollbarInitialPosition = HelperFunctions.computeScrollbarPositionFromValue(parent.params.getSigmaDoG(), parent.sigmaMin, parent.sigmaMax, parent.scrollbarSize);
		final Scrollbar sigma1Bar = new Scrollbar(Scrollbar.HORIZONTAL, scrollbarInitialPosition, 10, 0,
				10 + parent.scrollbarSize);

		final float log1001 = (float) Math.log10(parent.scrollbarSize + 1);
		scrollbarInitialPosition = (int) Math
				.round(1001 - Math.pow(10, (parent.thresholdMax - parent.params.getThresholdDoG()) / (parent.thresholdMax - parent.thresholdMin) * log1001));
		final Scrollbar thresholdBar = new Scrollbar(Scrollbar.HORIZONTAL, scrollbarInitialPosition, 10, 0,
				10 + parent.scrollbarSize);

		final Label sigmaText1 = new Label("Sigma 1 = " + String.format(java.util.Locale.US, "%.2f", parent.params.getSigmaDoG()),
				Label.CENTER);

		final Label thresholdText = new Label(
				"Threshold = " + String.format(java.util.Locale.US, "%.4f", parent.params.getThresholdDoG()), Label.CENTER);
		final Button button = new Button("Done");
		final Button cancel = new Button("Cancel");

		/* Location */
		aFrame.setLayout(layout);

		// insets constants
		int inTop = 0;
		int inRight = 5;
		int inBottom = 0;
		int inLeft = inRight;

		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 0;
		c.gridy = 0;
		c.weightx = 1;
		aFrame.add(sigmaText1, c);

		++c.gridy;
		c.insets = new Insets(inTop, inLeft, inBottom, inRight);
		aFrame.add(sigma1Bar, c);

		++c.gridy;
		aFrame.add(thresholdText, c);

		++c.gridy;
		aFrame.add(thresholdBar, c);

		// insets for buttons
		int bInTop = 0;
		int bInRight = 120;
		int bInBottom = 0;
		int bInLeft = bInRight;

		 ++c.gridy;
		 c.insets = new Insets(bInTop, bInLeft, bInBottom, bInRight);
		 aFrame.add(button, c);

		++c.gridy;
		c.insets = new Insets(bInTop, bInLeft, bInBottom, bInRight);
		aFrame.add(cancel, c);
		
		/* On screen positioning */
		/* Screen positioning */
		int xOffset = 20; 
		int yOffset = 20;
		aFrame.setLocation(xOffset, yOffset);

		/* Configuration */
		sigma1Bar.addAdjustmentListener(new SigmaListener(parent,sigmaText1, parent.sigmaMin, parent.sigmaMax, parent.scrollbarSize, sigma1Bar));
		thresholdBar.addAdjustmentListener(new ThresholdListener(parent,thresholdText, parent.thresholdMin, parent.thresholdMax));
		button.addActionListener(new FinishedButtonListener(parent, false));
		cancel.addActionListener(new FinishedButtonListener(parent, true));
		aFrame.addWindowListener(new FrameListener(parent));
	}

	public Frame getFrame() { return aFrame; }
	
	
}
