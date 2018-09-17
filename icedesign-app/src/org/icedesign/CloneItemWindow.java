package org.icedesign;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;

import org.icelib.AttachableTemplate;
import org.icelib.EntityKey;
import org.icelib.Icelib;
import org.icescene.HUDMessageAppState;
import org.icescene.ServiceRef;
import org.icescene.configuration.attachments.AttachableTemplates;

import icetone.core.BaseScreen;
import icetone.core.ToolKit;

public class CloneItemWindow extends NewItemWindow {

	private AttachableTemplate itemDef;
	@ServiceRef
	protected static AttachableTemplates attachableTemplatesConfiguration;

	public CloneItemWindow(BaseScreen screen) {
		super(screen);
		setWindowTitle("Clone Item");
		setButtonOkText("Clone");
	}

	public void setItemKey(EntityKey key) {
		AttachableTemplate def = attachableDef.get(key);
		AttachableTemplate templ = def.getDelegate();

		type.setSelectedByValue(key.getType());
		type.setEnabled(false);

		template.runAdjusting(() -> template.setSelectedByValue(templ));
		template.setEnabled(false);

		name.setText(key.getItem());
		item.setText(key.getTemplate());
		rebuildName();

	}

	public void setItem(AttachableTemplate itemDef) {
		this.itemDef = itemDef;
		setItemKey(itemDef.getKey());
	}

	@Override
	protected AttachableTemplate createItem(EntityKey key) {
		AttachableTemplate clone = (AttachableTemplate) itemDef.clone();
		clone.setKey(key);
		return clone;
	}

	@Override
	protected void configureNewItem(AttachableTemplate newItem) {
		newItem.setAnimated(itemDef.isAnimated());
		newItem.setIlluminated(itemDef.isIlluminated());
		newItem.setEntity(itemDef.getEntity());
		newItem.setMesh(itemDef.getMesh());
		newItem.setParticle(itemDef.isParticle());
		newItem.setParticles(new ArrayList<String>(itemDef.getParticles()));
	}

	@Override
	protected void onCreate(AttachableTemplate newDef, File assetsDir) throws IOException {

		copy(newDef, "-Tint", assetsDir, "png");
		copy(newDef, "-Tint", assetsDir, "png.colormap");
		copy(newDef, "", assetsDir, "png");
		copy(newDef, "", assetsDir, "mesh.xml");

		HUDMessageAppState hud = ToolKit.get().getApplication().getStateManager().getState(HUDMessageAppState.class);
		if (hud != null) {
			hud.message(Level.INFO,
					String.format("Cloned %s to %s", itemDef.getKey().getName(), newDef.getKey().getName()));
		}

	}

	private void copy(AttachableTemplate newDef, final String suffix, File assetDir, String ext) throws IOException {
		String sourcePath = String.format("%1$s/%2$s/%2$s-%3$s%4$s.%5$s", Icelib.toEnglish(itemDef.getKey().getType()),
				itemDef.getKey().getItemName(), itemDef.getKey().getTemplate(), suffix, ext);
		String targetPath = String.format("%1$s/%2$s/%2$s-%3$s%4$s.%5$s", Icelib.toEnglish(newDef.getKey().getType()),
				newDef.getKey().getItemName(), newDef.getKey().getTemplate(), suffix, ext);
		copy(assetDir, sourcePath, targetPath);
	}

}
