package gui.csv.overlay;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public  class FinishedButtonListener implements ActionListener {
	final CsvOverlay parent;
	final boolean done;

	public FinishedButtonListener(
			final CsvOverlay parent,
			final boolean done) {
		this.parent = parent;
		this.done = done;
	}

	@Override
	public void actionPerformed(final ActionEvent arg0) {
		parent.isFinished = done;
		parent.dispose();
	}
}
