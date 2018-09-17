package org.icedesign;

import com.jme3.light.AmbientLight;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector2f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;

import icetone.controls.containers.OSRViewPort;
import icetone.core.BaseScreen;
import icetone.core.Size;
import icetone.core.Element;
import icetone.core.layout.FillLayout;

public class Preview extends Element {

	private final Node viewportNode;
	private final AmbientLight ambientLight;
	private Spatial spatial;

	public Preview(BaseScreen screen) {
		this(screen, null);
	}

	public Preview(BaseScreen screen, Size size) {
		// Viewport border
		super(screen);
		setIgnoreGlobalAlpha(true);
		setGlobalAlpha(1f);
		setLayoutManager(new FillLayout());
		if (size == null)
			size = new Size(calcPreferredSize());

		// Container node with light
		viewportNode = new Node();
		ambientLight = new AmbientLight();
		ambientLight.setColor(ColorRGBA.White);
		viewportNode.addLight(ambientLight);

		final Vector2f vpSize = size.toVector2f().subtract(getTotalPadding());

		// Viewport
		OSRViewPort vp = new OSRViewPort(screen, new Size(vpSize));
		vp.setIgnoreMouseWheel(false);
		vp.setIgnoreMouse(false);
		vp.setIgnoreMouseButtons(false);
		vp.setToolTipText("Mouse Wheel To Zoom");
		vp.setOSRBridge(viewportNode, (int) vpSize.x, (int) vpSize.y);
		vp.setUseCameraControlRotate(true);
		vp.move(0, vpSize.x / 2f, 0);
		addElement(vp);
	}

	public AmbientLight getAmbientLight() {
		return ambientLight;
	}

	public void setPreview(Spatial spatial) {
		if (this.spatial != null) {
			this.spatial.removeFromParent();
		}
		this.spatial = spatial;
		viewportNode.attachChild(spatial);
	}
}
