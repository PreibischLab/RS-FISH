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
package util.st.render;

import com.google.common.base.Supplier;

import net.imglib2.KDTree;
import net.imglib2.RealLocalizable;
import net.imglib2.Sampler;
import net.imglib2.neighborsearch.NearestNeighborSearchOnKDTree;
import util.st.render.util.SimpleRealLocalizable;
import util.st.render.util.SimpleSampler;

public class NearestNeighborMaxDistanceSearchOnKDTree< T > extends NearestNeighborSearchOnKDTree< T >
{
	final Supplier<T> outOfBounds;
	final SimpleSampler<T> oobSampler;
	final SimpleRealLocalizable position;
	final MaxDistanceParam param;
	final KDTree< T > tree; // is private in superclass
	final double[] pos;

	Sampler< T > value;
	RealLocalizable point;
	double newBestSquDistance;

	public NearestNeighborMaxDistanceSearchOnKDTree(final KDTree< T > tree, final Supplier<T> outOfBounds, final MaxDistanceParam param )
	{
		super( tree );

		this.pos = new double[ tree.numDimensions() ];
		this.tree = tree;
		this.oobSampler = new SimpleSampler<>(outOfBounds);
		this.position = new SimpleRealLocalizable( pos ); // last queried location
		this.outOfBounds = outOfBounds;
		this.param = param;
	}

	@Override
	public void search( final RealLocalizable p )
	{
		super.search( p );
		p.localize( pos );

		if ( super.getSquareDistance() > param.maxSqDistance() )
		{
			value = oobSampler;
			point = position;
			newBestSquDistance = 0;
		}
		else
		{
			value = super.getSampler();
			point = super.getPosition();
			newBestSquDistance = super.getSquareDistance();
		}
	}

	@Override
	public Sampler< T > getSampler()
	{
		return value;
	}

	@Override
	public RealLocalizable getPosition()
	{
		return point;
	}

	@Override
	public double getSquareDistance()
	{
		return newBestSquDistance;
	}

	@Override
	public double getDistance()
	{
		return Math.sqrt(newBestSquDistance);
	}

	@Override
	public NearestNeighborMaxDistanceSearchOnKDTree< T > copy()
	{
		final NearestNeighborMaxDistanceSearchOnKDTree< T > copy =
				new NearestNeighborMaxDistanceSearchOnKDTree<>( new KDTree<>( tree.treeData() ), outOfBounds, param);

		// make sure the state is preserved
		copy.search( position );

		return copy;
	}
}

