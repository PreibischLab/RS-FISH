package gui.utils;

/*-
 * #%L
 * A plugin for radial symmetry localization on smFISH (and other) images.
 * %%
 * Copyright (C) 2016 - 2025 RS-FISH developers.
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
/**
 * License: GPL
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License 2
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

import java.util.Iterator;

import net.imglib2.type.numeric.ARGBType;

/**
 * Generate a stream of `random' saturated RGB colors with all colors being
 * maximally distinct from each other.
 *
 * @author Stephan Saalfeld &gt;saalfeld@mpi-cbg.de&lt;
 * @author Tobias Pietzsch &gt;tobias.pietzsch@gmail.com&lt;
 */
public class ColorStream
{
    final static protected double goldenRatio = 0.5 * Math.sqrt( 5 ) + 0.5;
    final static protected double stepSize = 6.0 * goldenRatio;
    final static protected double[] rs = new double[]{ 1, 1, 0, 0, 0, 1, 1 };
    final static protected double[] gs = new double[]{ 0, 1, 1, 1, 0, 0, 0 };
    final static protected double[] bs = new double[]{ 0, 0, 0, 1, 1, 1, 0 };

    static long i = -1;

    final static protected int interpolate( final double[] xs, final int k, final int l, final double u, final double v )
    {
        return ( int )( ( v * xs[ k ] + u * xs[ l ] ) * 255.0 + 0.5 );
    }

    final static protected int argb( final int r, final int g, final int b )
    {
        return ( ( ( r << 8 ) | g ) << 8 ) | b | 0xff000000;
    }

    final public static int get( final long index )
    {
        double x = goldenRatio * index;
        x -= ( long )x;
        x *= 6.0;
        final int k = ( int )x;
        final int l = k + 1;
        final double u = x - k;
        final double v = 1.0 - u;

        final int r = interpolate( rs, k, l, u, v );
        final int g = interpolate( gs, k, l, u, v );
        final int b = interpolate( bs, k, l, u, v );

        return argb( r, g, b );
    }

    final static public int next()
    {
        return get( ++i );
    }

    final static public Iterator< ARGBType > iterator()
    {
        return new Iterator< ARGBType >()
        {
            long i = -1;

            @Override
            public boolean hasNext()
            {
                return true;
            }

            @Override
            public ARGBType next()
            {
                return new ARGBType( get( ++i ) );
            }

            @Override
            public void remove()
            {}
        };
    }
}
