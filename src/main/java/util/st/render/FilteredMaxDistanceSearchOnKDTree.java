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
package util.st.render;

import net.imglib2.KDTree;
import net.imglib2.RealLocalizable;
import net.imglib2.Sampler;
import util.st.filter.Filter;
import util.st.filter.RadiusSearchFilterFactory;
import util.st.render.util.SimpleSampler;


public class FilteredMaxDistanceSearchOnKDTree< S, T > implements IntegratingNeighborSearch< T >
{
	protected final int n;
	final KDTree< S > tree;
	final SimpleSampler< T > value;
	final RadiusSearchFilterFactory< S, T > filterFactory;
	final Filter< T > filter;

	public FilteredMaxDistanceSearchOnKDTree(
			final KDTree< S > tree,
			final RadiusSearchFilterFactory< S, T > filterFactory )
	{
		this.n = tree.numDimensions();
		this.tree = tree;
		this.value = new SimpleSampler<>( filterFactory.create() );
		this.filterFactory = filterFactory;
		this.filter = filterFactory.createFilter( tree );
	}

	@Override
	public void search( final RealLocalizable p )
	{
		filter.filter( p, value.get() );
	}

	@Override
	public Sampler< T > getSampler()
	{
		return value;
	}

	@Override
	public int numDimensions()
	{
		return n;
	}

	@Override
	public FilteredMaxDistanceSearchOnKDTree< S, T > copy()
	{
		return new FilteredMaxDistanceSearchOnKDTree< S, T >( tree, filterFactory );
	}
}
