/*-
 * #%L
 * A plugin for radial symmetry localization on smFISH (and other) images.
 * %%
 * Copyright (C) 2016 - 2024 RS-FISH developers.
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
package util.st.render.util;

import java.util.function.Supplier;

import net.imglib2.Sampler;

public class SimpleSampler< T > implements Sampler< T >
{
	final Supplier< T > supplier;
	final T type;

	public SimpleSampler( final Supplier< T > supplier )
 	{
		this.supplier = supplier;
		this.type = supplier.get();
	}

	@Override
	public T get()
	{
		return type;
	}

	@Override
	public Sampler< T > copy()
	{
		return new SimpleSampler<>(supplier);
	}
}
