package org.icedesign;

import com.jme3.light.AmbientLight;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector4f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;

import icetone.controls.extras.OSRViewPort;
import icetone.core.Element;
import icetone.core.ElementManager;
import icetone.core.layout.FillLayout;
import icetone.core.layout.LUtil;
import icetone.core.utils.UIDUtil;

public class Preview extends Element {
    
    private final Node viewportNode;
    private final AmbientLight ambientLight;
    private Spatial spatial;
    
    public Preview(ElementManager screen) {
        this(screen, screen.getStyle("Preview").getVector2f("defaultSize"));
    }
    
    public Preview(ElementManager screen, Vector2f size) {
        // Viewport border
        super(screen, UIDUtil.getUID(), size,
                screen.getStyle("Preview").getVector4f("resizeBorders"),
                screen.getStyle("Preview").getString("defaultImg"));
        setIgnoreGlobalAlpha(true);
        setGlobalAlpha(1f);
        setLayoutManager(new FillLayout());

        // Container node with light
        viewportNode = new Node();
        ambientLight = new AmbientLight();
        ambientLight.setColor(ColorRGBA.White);
        viewportNode.addLight(ambientLight);
        
        if(size.equals(LUtil.LAYOUT_SIZE))
        	size = screen.getStyle("Preview").getVector2f("defaultSize");
        final Vector2f vpSize = new Vector2f(size.x - borders.y - borders.z, size.y - borders.x - borders.w);

        // Viewport
        OSRViewPort vp = new OSRViewPort(screen, Vector2f.ZERO, vpSize, Vector4f.ZERO, null);
        vp.setIgnoreMouseWheel(false);
        vp.setIgnoreMouse(false);
        vp.setIgnoreMouseButtons(false);
        vp.setToolTipText("Mouse Wheel To Zoom");
        vp.setOSRBridge(viewportNode, (int) vpSize.x, (int) vpSize.y);
        vp.setUseCameraControlRotate(true);
        vp.move(0, vpSize.x / 2f, 0);
        addChild(vp);
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
