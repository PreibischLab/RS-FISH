package util.st.filter;

import net.imglib2.IterableRealInterval;

public interface FilterFactory< S, T >
{
	Filter< T > createFilter( final IterableRealInterval< S > data );
	T create();
}
