package radial.symmetry.parameters;


public class RadialSymmetryParameters
{
	GUIParams params;
	double[] calibration; 
	
	public RadialSymmetryParameters( final GUIParams guiParams, double[] calibration) {
		this.params = guiParams;
		this.calibration = calibration;
	}
	
	public GUIParams getParams(){
		return params;
	}
	
	public double[] getCalibration(){
		return calibration;
	}
	
	public void setCalibration(double [] calibration){
		this.calibration = calibration.clone();
	}
	
}
