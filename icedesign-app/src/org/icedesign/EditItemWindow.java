package org.icedesign;

import java.io.File;
import java.util.prefs.Preferences;

import org.icelib.AttachableTemplate;
import org.icelib.Icelib;
import org.icescene.assets.Assets;
import org.iceui.HPosition;
import org.iceui.VPosition;
import org.iceui.controls.FancyPersistentWindow;
import org.iceui.controls.FancyWindow;
import org.iceui.controls.SaveType;

import com.jme3.math.Vector2f;

import icetone.core.Element;
import icetone.core.ElementManager;
import icetone.core.layout.FillLayout;

public class EditItemWindow extends FancyPersistentWindow {

	private ItemEditorPanel itemTextureEditor;

	public EditItemWindow(ElementManager screen, Assets assets, Preferences prefs) {
		super(screen, DesignConfig.ATTACHMENT_FILES_EDITOR, 8, VPosition.MIDDLE, HPosition.RIGHT, new Vector2f(320, 300),
				FancyWindow.Size.SMALL, true, SaveType.POSITION_AND_SIZE, prefs);

		setIsResizable(true);
		final Element windowContent = getContentArea();
		windowContent.setLayoutManager(new FillLayout());
		itemTextureEditor = new ItemEditorPanel(assets, screen, prefs) {
			@Override
			protected void textureFileUpdated(File file) {
				onTextureFileUpdated();
			}
		};
		windowContent.addChild(itemTextureEditor);
	}

	protected void onTextureFileUpdated() {
	}

	public void setValue(AttachableTemplate def) {
		setWindowTitle(String.format("Item - %s", Icelib.getBaseFilename(def.getKey().getName())));
		itemTextureEditor.setValue(def);
		if (!getIsVisible()) {
			showWithEffect();
		}
	}
}
