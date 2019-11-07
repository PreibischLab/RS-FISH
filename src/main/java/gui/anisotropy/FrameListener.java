package gui.anisotropy;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class FrameListener extends WindowAdapter {
	final AnisitropyCoefficient parent;

	public FrameListener(
			final AnisitropyCoefficient parent )
	{
		super();
		this.parent = parent;
	}

	@Override
	public void windowClosing(WindowEvent e) {
		parent.dispose();
	}
}