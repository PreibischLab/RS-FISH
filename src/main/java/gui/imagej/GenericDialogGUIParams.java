package gui.imagej;

import ij.gui.GenericDialog;
import parameters.GUIParams;

public class GenericDialogGUIParams 
{
	final GUIParams guiParams;
	
	public GenericDialogGUIParams( final GUIParams guiParams ) {
		this.guiParams = guiParams;
		GenericDialog gui;
	}
	
	// TODO: this one should should show the 
	// initial dialog ! 
	
	// extra parameters are 
	// = image imp
	// = type of the detection either manual or interactive
	// = do additional gauss fit
}
