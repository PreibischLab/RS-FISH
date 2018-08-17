package gui.radial.symmetry.histogram;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ButtonFinishedListener implements ActionListener {

	Histogram parent;
	
	public ButtonFinishedListener(final Histogram parent) {
		this.parent = parent;
	}

	@Override
	public void actionPerformed(final ActionEvent arg0) {
		parent.isFinished = true;
		parent.dispose();
	}
}
