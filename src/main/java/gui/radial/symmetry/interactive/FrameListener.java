package gui.radial.symmetry.interactive;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class FrameListener extends WindowAdapter {
	final InteractiveRadialSymmetry parent;

	public FrameListener(
			final InteractiveRadialSymmetry parent )
	{
		super();
		this.parent = parent;
	}

	@Override
	public void windowClosing(WindowEvent e) {
		parent.dispose();
	}
}