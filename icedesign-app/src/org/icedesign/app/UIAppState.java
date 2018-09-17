package org.icedesign.app;

import java.util.prefs.Preferences;

import org.icedesign.DesignConfig;
import org.icescene.scene.AbstractSceneUIAppState;
import org.iceui.IceUI;

import icetone.controls.lists.FloatRangeSliderModel;
import icetone.controls.lists.Slider;
import icetone.core.Orientation;
import icetone.core.layout.mig.MigLayout;
import icetone.core.undo.UndoManager;
import icetone.extras.chooser.ColorFieldControl;

public class UIAppState extends AbstractSceneUIAppState {

	private ColorFieldControl lightColour;
	private Slider<Float> light;

	public UIAppState(UndoManager undoManager, Preferences prefs) {
		super(undoManager, prefs);
	}

	@Override
	protected void addBefore() {

		// Light Colour
		lightColour = new ColorFieldControl(screen, IceUI.getColourPreference(prefs,
				DesignConfig.CREATURE_TWEAK_LIGHT_COLOUR, DesignConfig.CREATURE_TWEAK_LIGHT_COLOUR_DEFAULT), 
				true, true);
		lightColour.onChange(evt -> IceUI.setColourPreferences(prefs, DesignConfig.CREATURE_TWEAK_LIGHT_COLOUR, evt.getNewValue()));
		lightColour.setToolTipText("Light colour");
		layer.addElement(lightColour);

		// Light
		light = new Slider<Float>(screen, Orientation.HORIZONTAL);
		light.onChanged(evt -> prefs.putFloat(DesignConfig.CREATURE_TWEAK_LIGHT, evt.getNewValue()));
		light.setSliderModel(new FloatRangeSliderModel(0, 5,
				prefs.getFloat(DesignConfig.CREATURE_TWEAK_LIGHT, DesignConfig.CREATURE_TWEAK_LIGHT_DEFAULT), 0.25f));
		light.setToolTipText("Light Amount");
		layer.addElement(light, "w 150");

	}

	@Override
	protected MigLayout createLayout() {
		return new MigLayout(screen, "fill", "[][]push[][][][]", "[]push");
	}
}
