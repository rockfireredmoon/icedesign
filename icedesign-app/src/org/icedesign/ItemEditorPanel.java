package org.icedesign;

import java.util.prefs.Preferences;

import org.icelib.AttachableTemplate;
import org.icelib.Icelib;
import org.icescene.ServiceRef;
import org.icescene.assets.Assets;
import org.icescene.configuration.ColourPalettes;

import icetone.controls.buttons.PushButton;
import icetone.core.BaseScreen;
import icetone.core.layout.mig.MigLayout;

public class ItemEditorPanel extends AbstractTextureEditorPanel<AttachableTemplate> {

	private PushButton definition;
	private AttachableDefEditor definitionWindow;

	@ServiceRef
	private static ColourPalettes colourPalettes;

	public ItemEditorPanel(Assets assets, BaseScreen screen, Preferences pref) {
		super(assets, screen, pref);

		definition = new PushButton(screen) {
			{
				setStyleClass("fancy");
			}
		};
		definition.onMouseReleased(evt -> editDefinition());
		definition.setText("Def");
		definition.setEnabled(false);
		definition.setToolTipText(
				"Edit the definition for this item - DISABLED FOR NOW, AS NOT COMPLETE. \nYOU CAN MANUALLY EDIT THE .JS / .NUT FILES TO CHANGE DEFINITION");
		actions.addElement(definition);

		setLayoutManager(new MigLayout(screen, "wrap 2, fill", "[shrink 0][fill, grow]", "[shrink 200,fill,grow][]"));
		addElement(table, "growx, span 2");
		addElement(actions, "growx, span 2");
		setAvailable();
	}

	@Override
	public void setValue(AttachableTemplate value) {
		super.setValue(value);

		addImage("Diffuse", ".png");
		addImage("Tint", "-Tint.png");
		addImagePath("Model", null,
				String.format("%1$s/%2$s/%3$s.mesh.xml", Icelib.toEnglish(value.getKey().getType()),
						value.getKey().getItemName(), value.getKey().getName())).getImage()
								.addStyleClass("icon icon-mesh");
		addImagePath("Blender", null,
				String.format("%1$s/%2$s/%3$s.blend", Icelib.toEnglish(value.getKey().getType()),
						value.getKey().getItemName(), value.getKey().getName())).getImage()
								.addStyleClass("icon icon-blend");

		layoutChildren();
	}

	private void editDefinition() {
		if (definitionWindow == null) {
			definitionWindow = new AttachableDefEditor(screen, pref) {
				@Override
				protected void onCloseWindow() {
					definitionWindow = null;
				}
			};
		}
		definitionWindow.setValue(value);
	}

	private void addImage(String name, String suffix) {
		String imgPath = String.format("%1$s/%2$s/%2$s-%3$s%4$s", Icelib.toEnglish(value.getKey().getType()),
				value.getKey().getItemName(), value.getKey().getTemplate(), suffix);
		addImagePath(name, imgPath, imgPath);
	}

	@Override
	protected String getDirectoryPath() {
		return String.format("%1$s/%2$s", Icelib.toEnglish(value.getKey().getType()), value.getKey().getItemName());
	}

}
