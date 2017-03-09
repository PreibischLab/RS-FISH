package gui;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import ij.ImagePlus;
import ij.gui.Roi;

/**
 * keeps the target ROI same as source ROI
 * @author spreibi
 *
 */
public class FixROIListener implements MouseListener {
	final ImagePlus source, target;

	public FixROIListener( final ImagePlus s, final ImagePlus t){
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

		if ( roi == null )
			target.deleteRoi();
		else
			target.setRoi( roi );
	}
}