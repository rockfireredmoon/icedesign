package org.icedesign;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.icelib.ClothingTemplate;
import org.icelib.ClothingTemplateKey;
import org.icelib.Icelib;
import org.icescene.ServiceRef;
import org.icescene.configuration.ClothingDef;
import org.iceui.controls.UIUtil;

import icetone.core.BaseScreen;
import icetone.core.ToolKit;
import icetone.core.Layout.LayoutType;

public class CloneClothingWindow extends AbstractCloneWindow<List<ClothingTemplate>> {

	@ServiceRef
	private static ClothingDef clothingDef;
	private ClothingTemplateKey clothingKey;

	public CloneClothingWindow(BaseScreen screen, ClothingTemplateKey clothingKey, List<ClothingTemplate> def) {
		super(screen, def);
		this.clothingKey = clothingKey;
		setWindowTitle(String.format("Clone %s", clothingKey.getType()));
		setMsg(clothingKey.getItem());

		// Change the OK button text (annoyingly we have to relayout as well)
		setButtonOkText("Clone Item");
	}

	protected final void doCloneItem(List<ClothingTemplate> def, String newName) throws IOException {
		ClothingTemplateKey ctk = clothingKey.clone();
		ctk.setItem(input.getText());
		ItemWriter iw = new ItemWriter(ToolKit.get().getApplication(), ctk);
		for (ClothingTemplate originalDefinition : def) {
			ClothingTemplate template = originalDefinition.clone();
			template.getKey().setItem(input.getText());
			clothingDef.put(template.getKey(), template);

			Set<String> regs = new HashSet<String>(template.getRegions().values());
			for (String r : regs) {
				copy(originalDefinition, template, r, "-Clothing_Map", iw.getAssetsDir(), "png");
				copy(originalDefinition, template, r, "-Clothing_Map", iw.getAssetsDir(), "png.colormap");
				copy(originalDefinition, template, r, "-Tint", iw.getAssetsDir(), "png");
				copy(originalDefinition, template, r, "-Tint", iw.getAssetsDir(), "png.colormap");
				copy(originalDefinition, template, r, "", iw.getAssetsDir(), "png");
			}

			onDoCloneItem(originalDefinition, template, newName);
		}
		iw.write();
	}

	protected void onDoCloneItem(final ClothingTemplate def, final ClothingTemplate newDef, String newName)
			throws IOException {
	}

	private void copy(ClothingTemplate def, ClothingTemplate newDef, String r, final String suffix, File assetDir,
			String ext) throws IOException {

		String sourcePath = String.format("%1$s/%2$s/%2$s-%3$s%4$s.%5$s", Icelib.toEnglish(def.getKey().getType()),
				def.getKey().getItemName(), r, suffix, ext);
		String targetPath = String.format("%1$s/%2$s/%2$s-%3$s%4$s.%5$s", Icelib.toEnglish(newDef.getKey().getType()),
				newDef.getKey().getItemName(), r, suffix, ext);

		// String sourcePath = String.format("Armor/%1$s/%1$s-%2$s%3$s.%4$s",
		// def.getKey().getType(), r, suffix, ext);
		// String targetPath = String.format("Armor/%1$s/%1$s-%2$s%3$s.%4$s",
		// newDef.getKey().getType(), r, suffix, ext);
		copy(assetDir, sourcePath, targetPath);
	}
}
