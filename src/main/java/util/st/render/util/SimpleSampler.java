package util.st.render.util;

import net.imglib2.Sampler;

public class SimpleSampler< T > implements Sampler< T >
{
	final T type;

	public SimpleSampler( final T type )
 	{
		this.type = type;
	}

	@Override
	public T get()
	{
		return type;
	}

	@Override
	public Sampler< T > copy()
	{
		return new SimpleSampler< T >( type );
	}
}
