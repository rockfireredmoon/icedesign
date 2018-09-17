package org.icedesign;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.icelib.AbstractCreature;
import org.icelib.AttachableTemplate;
import org.icelib.AttachmentItem;
import org.icelib.RGB;
import org.icescene.Icescene;
import org.icescene.ServiceRef;
import org.icescene.configuration.ColorMapConfiguration;
import org.icescene.configuration.attachments.AttachableDef;
import org.iceui.IceUI;

import com.jme3.asset.AssetNotFoundException;
import com.jme3.font.BitmapFont;
import com.jme3.math.ColorRGBA;

import icetone.core.BaseScreen;
import icetone.core.Element;
import icetone.core.layout.FlowLayout;
import icetone.extras.chooser.ColorButton;
import icetone.extras.chooser.ColorSelector;

public class AttachmentColourBar extends Element {

	private static final Logger LOG = Logger.getLogger(AttachmentColourBar.class.getName());
	private AttachmentItem def;
	private final AbstractCreature creature;

	@ServiceRef
	protected static AttachableDef attachableDef;

	public AttachmentColourBar(BaseScreen mgr, AbstractCreature creature) {
		super(mgr);
		setLayoutManager(new FlowLayout(2, BitmapFont.Align.Center));
		this.creature = creature;
	}

	public AttachmentItem getAttachment() {
		return def;
	}

	public void setAttachment(AttachmentItem def) {
		this.def = def;
		removeAllChildren();
		if (def != null) {
			final String tintPath = Icescene.checkAssetPath(
					String.format("%1$s/%2$s-Tint.png.colormap", def.getKey().getPath(), def.getKey().getName()));
			try {
				final ColorMapConfiguration itemColours = ColorMapConfiguration
						.get(screen.getApplication().getAssetManager(), tintPath);
				final List<RGB> mapColors = itemColours.colors();
				for (int i = 0; i < mapColors.size(); i++) {
					RGB col = mapColors.get(i);
					if (def.getColors() != null && i < def.getColors().size()) {
						col = def.getColors().get(i);
					}
					addChooser(col, i, mapColors);
				}
			} catch (AssetNotFoundException anfe) {
				LOG.log(Level.SEVERE, "Could not find color map for item. Probably missing assets.", anfe);
			}
		}
		layoutChildren();
	}

	private void addChooser(final RGB rgb, final int idx, final List<RGB> mapColors) {
		ColorButton cfc = new ColorButton(screen, IceUI.toRGBA(rgb), false) {
			@Override
			protected void onChangeColor(ColorRGBA newColor) {
				// If the point doesn't have any colors (fill in the defaults
				// now)
				if (def.getColors() == null || def.getColors().isEmpty()) {
					List<RGB> l = new ArrayList<RGB>();
					AttachableTemplate item = attachableDef.get(def.getKey());
					// TODO def colours should come from item definition, not
					// tint map
					l.addAll(mapColors);
					def.setColors(l);
				}
				def.getColors().set(idx, IceUI.fromRGBA(newColor));
				onUpdate();
			}
		};
		cfc.setTabs(ColorSelector.ColorTab.values());
		addElement(cfc);
	}

	protected void onUpdate() {
	}
}
