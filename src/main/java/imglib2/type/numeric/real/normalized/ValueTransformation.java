package imglib2.type.numeric.real.normalized;

public interface ValueTransformation< S, T >
{
	/**
	 * Transform from a to b
	 * @param a - input
	 * @param b - output
	 */
	public void transform( S a, T b );
}
