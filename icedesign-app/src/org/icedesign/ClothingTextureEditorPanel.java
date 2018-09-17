package org.icedesign;

import java.util.prefs.Preferences;

import org.icelib.ClothingTemplate;
import org.icelib.Icelib;
import org.icelib.Region;
import org.icescene.assets.Assets;

import icetone.controls.text.Label;
import icetone.core.BaseScreen;
import icetone.core.layout.mig.MigLayout;

public class ClothingTextureEditorPanel extends AbstractTextureEditorPanel<ClothingTemplate> {

	private final Label regionText;
	private final Label type;
	private final Label bodyType;
	private final Label regionPriority;
	private final Label part;
	private Region region;

	public ClothingTextureEditorPanel(Assets assets, BaseScreen screen, Preferences pref) {
		super(assets, screen, pref);

		setLayoutManager(new MigLayout(screen, "wrap 2, fill", "[shrink 0][fill, grow]", "[][][][][][shrink 200,fill,grow][]"));
		addElement(new Label("Type:", screen));
		addElement(type = new Label(screen));
		addElement(new Label("Region:", screen));
		addElement(regionText = new Label(screen));
		addElement(new Label("Body Type:", screen));
		addElement(bodyType = new Label(screen));
		addElement(new Label("Part:", screen));
		addElement(part = new Label(screen));
		addElement(new Label("Region Priority:", screen));
		addElement(regionPriority = new Label(screen));
		addElement(table, "growx, span 2");

		addElement(actions, "growx, span 2");

		setAvailable();
	}

	public void setClothingDefinition(ClothingTemplate def, Region region) {
		this.region = region;

		setValue(def);
	}
	
	public void setValue(ClothingTemplate def) {
		super.setValue(def);

		regionPriority.setText(def.getRegionPriority().get(region));
		this.regionText.setText(region.name());
		part.setText(def.getRegions().get(region));
		type.setText(def.getKey().getName());
		bodyType.setText(Icelib.toEnglish(def.getBodyType()));

		addImage("Diffuse", "");
		addImage("Tint", "-Tint");
		addImage("Map", "-Clothing_Map");

		layoutChildren();
	}


	private void addImage(String name, String suffix) {
		String imgPath = String.format("%1$s/%2$s/%2$s-%3$s%4$s.png", Icelib.toEnglish(value.getKey().getType()),
				value.getKey().getItemName(), value
				.getRegions().get(region), suffix);
		addImagePath(name, imgPath, imgPath);
	}

	@Override
	protected String getDirectoryPath() {
		return String.format("%1$s/%2$s", Icelib.toEnglish(value.getKey().getType()), value.getKey().getItemName());
	}

	private void XXaddImage(String name, String suffix) {
		String imgPath = String.format("Armor/%s/%s-%s%s.png", value.getKey().getType(), value.getKey().getType(), value
				.getRegions().get(region), suffix);
		addImagePath(name, imgPath, imgPath);
	}

	protected String XXgetDirectoryPath() {
		return String.format("Armor/%s", value.getKey().getType());
	}

}
