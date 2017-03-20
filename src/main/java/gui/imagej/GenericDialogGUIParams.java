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
}
