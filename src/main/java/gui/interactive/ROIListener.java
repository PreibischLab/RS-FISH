package gui.interactive;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import gui.interactive.InteractiveRadialSymmetry.ValueChange;
import ij.ImagePlus;
import ij.gui.Roi;
import mpicbg.imglib.multithreading.SimpleMultiThreading;

/**
 * Tests whether the ROI was changed and will recompute the preview
 * 
 * @author Stephan Preibisch
 */
public class ROIListener implements MouseListener {
	final InteractiveRadialSymmetry parent;
	final ImagePlus source, target;

	public ROIListener( final InteractiveRadialSymmetry parent, final ImagePlus s, final ImagePlus t){
		this.parent = parent;
		this.source = s;
		this.target = t;
	}

	@Override
	public void mouseClicked(MouseEvent e) {}

	@Override
	public void mouseEntered(MouseEvent e) {}

	@Override
	public void mouseExited(MouseEvent e) {}

	@Override
	public void mousePressed(MouseEvent e) {}

	@Override
	public void mouseReleased(final MouseEvent e) {		
		// here the ROI might have been modified, let's test for that
		final Roi roi = source.getRoi();

		// roi is wrong, clear the screen 
		if (roi == null || roi.getType() != Roi.RECTANGLE){
			source.setRoi( parent.rectangle );
			target.setRoi( parent.rectangle );
		}

		// TODO: might put the update part for the roi here instead of the updatePreview
		while (parent.isComputing)
			SimpleMultiThreading.threadWait(10);

		parent.updatePreview(ValueChange.ROI);

	}
}