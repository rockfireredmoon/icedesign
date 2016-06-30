package org.icedesign.app;

import java.util.prefs.Preferences;

import org.icedesign.DesignConfig;
import org.icelib.UndoManager;
import org.icescene.scene.AbstractSceneUIAppState;
import org.iceui.IceUI;
import org.iceui.controls.color.ColorFieldControl;

import com.jme3.math.ColorRGBA;
import com.jme3.scene.debug.Arrow;

import icetone.controls.lists.FloatRangeSliderModel;
import icetone.controls.lists.Slider;
import icetone.core.Element.Orientation;
import icetone.core.layout.mig.MigLayout;

public class UIAppState extends AbstractSceneUIAppState {

    private ColorFieldControl lightColour;
    private Slider<Float> light;

    public UIAppState(UndoManager undoManager, Preferences prefs) {
        super(undoManager, prefs);
    }

    @Override
    protected void addBefore() {
    	Arrow w;

        // Light Colour
        lightColour = new ColorFieldControl(screen, IceUI.getColourPreference(prefs,
                DesignConfig.CREATURE_TWEAK_LIGHT_COLOUR,
                DesignConfig.CREATURE_TWEAK_LIGHT_COLOUR_DEFAULT), false, true, true) {
            @Override
            protected void onChangeColor(ColorRGBA newColor) {
                IceUI.setColourPreferences(prefs, DesignConfig.CREATURE_TWEAK_LIGHT_COLOUR, newColor);
            }
        };
        lightColour.setToolTipText("Light colour");
        layer.addChild(lightColour);

        // Light
        light = new Slider<Float>(screen, Orientation.HORIZONTAL, true) {
            @Override
            public void onChange(Float value) {
                prefs.putFloat(DesignConfig.CREATURE_TWEAK_LIGHT, (Float) value);
            }
        };
        light.setSliderModel(new FloatRangeSliderModel(0, 5, prefs.getFloat(DesignConfig.CREATURE_TWEAK_LIGHT, DesignConfig.CREATURE_TWEAK_LIGHT_DEFAULT), 0.25f));
        light.setToolTipText("Light Amount");
        layer.addChild(light, "w 150");

    }

    @Override
    protected MigLayout createLayout() {
        return new MigLayout(screen, "fill", "[][]push[][][][]", "[]push");
    }
}
