package org.icedesign;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.icelib.AbstractCreature;
import org.icelib.Appearance;
import org.icelib.ClothingItem;
import org.icelib.ClothingTemplate;
import org.icelib.RGB;
import org.icelib.Region;
import org.icescene.Icescene;
import org.icescene.configuration.ColorMapConfiguration;
import org.iceui.IceUI;
import org.iceui.controls.color.ColorButton;
import org.iceui.controls.color.XColorSelector;

import com.jme3.asset.AssetNotFoundException;
import com.jme3.font.BitmapFont;
import com.jme3.math.ColorRGBA;

import icetone.core.Element;
import icetone.core.ElementManager;
import icetone.core.layout.FlowLayout;
import icetone.core.utils.UIDUtil;

public class ArmourColourBar extends Element {
    private static final Logger LOG = Logger.getLogger(ArmourColourBar.class.getName());
    private final Region region;
    private final AbstractCreature creature;

    public ArmourColourBar(ElementManager mgr, Region region, AbstractCreature creature) {
        super(mgr);
        setLayoutManager(new FlowLayout(1, BitmapFont.Align.Left));
        this.region = region;
        this.creature = creature;
    }

    void setDefinition(ClothingTemplate def) {
        removeAllChildren();
        if (def != null) {
            String area = def.getRegions().get(region);
            if (area != null) {
                final String tintPath = Icescene.checkAssetPath(String.format("%1$s/%2$s-%3$s-Tint.png.colormap", def.getKey().getPath(), 
                			def.getKey().getName(), area));
                try {
                    final ColorMapConfiguration clothingMapColors = ColorMapConfiguration.get(getScreen().getApplication().getAssetManager(), tintPath);
                    final List<RGB> mapColors = clothingMapColors.colors();
                    for (final RGB rgb : mapColors) {
                        ColorButton cfc = new ColorButton(screen, UIDUtil.getUID(), IceUI.toRGBA(rgb), false) {
                            @Override
                            protected void onChangeColor(ColorRGBA newColor) {
                                Appearance appearance = creature.getAppearance();
                                Appearance.ClothingList clothingList = appearance.getClothing();
                                Appearance.ClothingType clothingType = region.toClothingType();
                                ClothingItem it = clothingList.getItemForType(clothingType);
                                // Populate the list with the default colors if there are none or not enough
                                List<RGB> colors = it.getColors();
                                if (colors == null) {
                                    colors = new ArrayList<>();
                                }
                                final Collection<RGB> defaultColors = clothingMapColors.colors();
                                Iterator<RGB> defaultColorIt = defaultColors.iterator();
                                while (colors.size() < defaultColors.size()) {
                                    colors.add(defaultColorIt.next());
                                }
                                // Update with the new color
                                colors.set(mapColors.indexOf(rgb), IceUI.fromRGBA(newColor));
                                it.setColors(colors);
                                appearance.setClothing(clothingList);
                                onUpdate();
                            }
                        };
                        cfc.setTabs(XColorSelector.ColorTab.values());
                        addChild(cfc);
                    }
                } catch (AssetNotFoundException anfe) {
                    LOG.log(Level.SEVERE, "Could not find color map for item. Probably missing assets.", anfe);
                }
            }
        }
        layoutChildren();
    }
    
    protected void onUpdate() {
        
    }
    
}
