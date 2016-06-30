package org.icedesign;

import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.icelib.Icelib;
import org.icescene.HUDMessageAppState;
import org.icescene.animation.AnimationOption;
import org.icescene.animation.AnimationSequence;
import org.iceui.controls.ElementStyle;
import org.iceui.controls.FancyButton;

import com.jme3.input.event.MouseButtonEvent;

import icetone.controls.buttons.ButtonAdapter;
import icetone.controls.buttons.CheckBox;
import icetone.controls.lists.ComboBox;
import icetone.controls.lists.FloatRangeSliderModel;
import icetone.controls.lists.Slider;
import icetone.controls.menuing.Menu;
import icetone.controls.text.Label;
import icetone.core.Container;
import icetone.core.Element;
import icetone.core.ElementManager;
import icetone.core.layout.mig.MigLayout;

public class PosePanel extends Element {

	private static final Logger LOG = Logger.getLogger(PosePanel.class.getName());
	private boolean adjusting;
	private final ComboBox<AnimationOption> preset;
	private AnimationOption customSequence;
	private final ButtonAdapter startPose;
	private final ButtonAdapter stopPose;
	private final Slider<Float> speed;
	private final CheckBox loop;
	private final FancyButton edit;
	private PoseEditWindow poseEditWindow;
	private Collection<String> animations;
	private final Label speedLabel;
	private AnimationOption editingPreset;
	private boolean active;

	public PosePanel(ElementManager screen) {
		super(screen);
		setAsContainerOnly();
		setLayoutManager(new MigLayout(screen, "wrap 1, fill", "[grow]", "[][][]push"));

		adjusting = true;
		recreateCustomSequence();

		// Preset
		preset = new ComboBox<AnimationOption>(screen) {
			@Override
			public void onChange(int selectedIndex, AnimationOption value) {
				if (!adjusting) {
					if (poseEditWindow != null) {
						editingPreset = (AnimationOption) value;
						// poseEditWindow.setSequence((AnimationOption) value);
					}
					if (active) {
						AnimationOption s = (AnimationOption) value;
						// if (s.getAnims() != null && !s.getAnims().isEmpty())
						// {
						// onStartPose();
						// }
						if (s != null) {
							onStartPose();
						}
					}
					setAvailable();
				}
			}
		};
		preset.setToolTipText(
				"Bipeds have a set of pre-defined sequences. For all other creatures, or to create your own sequence, choose 'Custom'");

		// Preset Panel
		Element presetPanel = new Element(screen);
		presetPanel.setLayoutManager(new MigLayout(screen, "fill", "[grow][shrink 0][shrink 0][shrink 0]"));
		presetPanel.addChild(preset, "growx");

		// Start
		startPose = new FancyButton(screen) {
			@Override
			public void onButtonMouseLeftDown(MouseButtonEvent evt, boolean toggle) {
				super.onButtonMouseLeftDown(evt, toggle);
				onStartPose();
			}
		};
		startPose.setText("Start");
		presetPanel.addChild(startPose);

		// Stop
		stopPose = new FancyButton(screen) {
			@Override
			public void onButtonMouseLeftDown(MouseButtonEvent evt, boolean toggle) {
				super.onButtonMouseLeftDown(evt, toggle);
				onStopPose();
			}
		};
		stopPose.setText("Stop");
		presetPanel.addChild(stopPose);

		// Edit
		edit = new FancyButton(screen) {
			@Override
			public void onButtonMouseLeftDown(MouseButtonEvent evt, boolean toggle) {
				HUDMessageAppState hud = app.getStateManager().getState(HUDMessageAppState.class);
				hud.message(Level.SEVERE,
						"This option is currently unavailable as the result of recent animation system rewrite. It will return!");
				// editingPreset = getPreset();
				// if (poseEditWindow == null) {
				// poseEditWindow = new PoseEditWindow(screen) {
				// @Override
				// protected void onSave(AnimationSequence seq) {
				// // editingPreset.populate(seq);
				// }
				// };
				// } else {
				// if (!poseEditWindow.getIsVisible()) {
				// poseEditWindow.showWithEffect();
				// }
				// }
				// poseEditWindow.setAnimations(animations);
				// poseEditWindow.setSequence(editingPreset);
			}
		};
		edit.setText("Edit");
		presetPanel.addChild(edit);

		// Speed
		speed = new Slider<Float>(screen, Slider.Orientation.HORIZONTAL, true) {
			@Override
			public void onChange(Float value) {
				speedLabel.setText(String.format("%2.1f", value));
				onSpeedChange(((Float) value).floatValue());
			}
		};
		speed.setSliderModel(new FloatRangeSliderModel(0, 10, 1, 0.1f));
		speed.setLockToStep(true);

		// Loop
		loop = new CheckBox(screen) {
			@Override
			public void onButtonMouseLeftUp(MouseButtonEvent evt, boolean toggled) {
			}
		};
		loop.setLabelText("Force Loop");

		// Options panel
		Container opts = new Container(screen);
		opts.setLayoutManager(new MigLayout(screen, "", "[shrink 0][fill, grow][shrink 0]48[shrink 0]", "[]"));
		opts.addChild(new Label("Speed", screen));
		opts.addChild(speed);
		opts.addChild(speedLabel = new Label(String.format("%2.1f", speed.getSliderModel().getValue().floatValue()), screen),
				"growx");
		ElementStyle.normal(screen, speedLabel, true, false);
		opts.addChild(loop);

		// This
		addChild(ElementStyle.medium(screen, new Label("Preset", screen)));
		addChild(presetPanel, "growx");
		addChild(opts, "growx");

		//
		setAvailable();
		adjusting = false;
	}

	public void setSelectedPreset(AnimationOption seq) {
		preset.setSelectedByValue(seq, false);
	}

	// public void playPreset(AnimationSequence seq) {
	public void playPreset(AnimationOption seq) {
		LOG.info(String.format("Playing preset %s", seq));
		preset.setSelectedByValue(seq, true);
		onStartPose();
	}

	public boolean isForceLoop() {
		return loop.getIsChecked();
	}

	public float getSpeed() {
		return speed.getSliderModel().getValue().floatValue();
	}

	protected void onSpeedChange(float newSpeed) {
	}

	protected void onStartAnimation() {
	}

	protected void onStartPose() {
	}

	protected void onStopPose() {
	}

	public AnimationOption getPreset() {
		final Menu menu = preset.getMenu();
		if (menu != null && preset.getSelectIndex() < menu.getMenuItems().size()) {
			return (AnimationOption) preset.getSelectedListItem().getValue();
		}
		return null;
	}

	protected void onSelectAnimationSequence(AnimationSequence seq) {
	}

	protected void onSelectAnimation(String anim, boolean loop, float speed) {
	}

	public void setPresets(Collection<AnimationOption> sequences) {
		adjusting = true;
		try {
			preset.removeAllListItems();
			if (sequences != null) {
				for (AnimationOption s : Icelib.sort(sequences)) {
					preset.addListItem(Icelib.camelToEnglish(s.getKey()), s, false, false);
				}
			}
			preset.addListItem("Custom", customSequence);
			preset.pack(false);
			preset.getMenu().setIgnoreGlobalAlpha(true);
			preset.getMenu().setGlobalAlpha(1f);
			recreateCustomSequence();
			// preset.getMenu().setHeight(200);
		} finally {
			adjusting = false;
		}
	}

	public void setAnimations(Collection<String> animations) {
		this.animations = animations;
		if (poseEditWindow != null) {
			poseEditWindow.setAnimations(animations);
		}
		recreateCustomSequence();
		setAvailable();
	}

	public void setActive(boolean active) {
		this.active = active;
		setAvailable();
	}

	private void setAvailable() {
		edit.setIsEnabled(preset.getSelectIndex() > -1);
		startPose.setIsEnabled(!active && preset.getSelectIndex() > -1);// &&
																		// !getPreset().getAnims().isEmpty());
		stopPose.setIsEnabled(active);
	}

	private void recreateCustomSequence() {
		if (customSequence == null) {
			// customSequence = new AnimationSequence("Custom");
			customSequence = new AnimationOption("Custom");
		}
		// customSequence.getAnims().clear();

		// if (pose != null && pose.getMenu() != null) {
		// for (MenuItem i : pose.getMenu().getMenuItems()) {
		// customSequence.addAnim(new
		// AnimationSequence.Anim(AnimationSequence.SubGroup.NONE,
		// AnimationSequence.Part.ALL, (String) i.getValue()));
		// break;
		// }
		// }
	}
}
