/*-
 * #%L
 * A plugin for radial symmetry localization on smFISH (and other) images.
 * %%
 * Copyright (C) 2016 - 2023 Developers.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */
package gui.anisotropy;

import fiji.tool.SliceListener;
import gui.anisotropy.AnisotropyCoefficient.ValueChange;
import ij.ImagePlus;
import mpicbg.imglib.multithreading.SimpleMultiThreading;

public class ImagePlusListener implements SliceListener
{
	final AnisotropyCoefficient parent;

	public ImagePlusListener( final AnisotropyCoefficient parent )
	{
		this.parent = parent;
	}

	@Override
	public void sliceChanged(ImagePlus arg0) {
		if (parent.isStarted) {
			// System.out.println("Slice changed!");
			while (parent.isComputing) {
				SimpleMultiThreading.threadWait(10);
			}
			parent.updatePreview(ValueChange.SLICE);
		}
	}
}
