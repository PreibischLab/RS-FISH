package gui.anisotropy;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class FrameListener extends WindowAdapter {
	final AnisotropyCoefficient parent;

	public FrameListener(
			final AnisotropyCoefficient parent )
	{
		super();
		this.parent = parent;
	}

	@Override
	public void windowClosing(WindowEvent e) {
		parent.dispose();
	}
}