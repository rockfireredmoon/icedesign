package org.icedesign;

import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.icelib.Icelib;
import org.icescene.HUDMessageAppState;
import org.icescene.animation.AnimationOption;
import org.icescene.animation.AnimationSequence;
import org.iceui.controls.ElementStyle;

import icetone.controls.buttons.CheckBox;
import icetone.controls.buttons.PushButton;
import icetone.controls.lists.ComboBox;
import icetone.controls.lists.FloatRangeSliderModel;
import icetone.controls.lists.Slider;
import icetone.controls.menuing.Menu;
import icetone.controls.text.Label;
import icetone.core.BaseElement;
import icetone.core.BaseScreen;
import icetone.core.Orientation;
import icetone.core.StyledContainer;
import icetone.core.Element;
import icetone.core.ToolKit;
import icetone.core.layout.mig.MigLayout;

public class PosePanel extends Element {

	private static final Logger LOG = Logger.getLogger(PosePanel.class.getName());
	private boolean adjusting;
	private final ComboBox<AnimationOption> preset;
	private AnimationOption customSequence;
	private final PushButton startPose;
	private final PushButton stopPose;
	private final Slider<Float> speed;
	private final CheckBox loop;
	private final PushButton edit;
	private PoseEditWindow poseEditWindow;
	private Collection<String> animations;
	private Label speedLabel;
	private AnimationOption editingPreset;
	private boolean active;

	public PosePanel(BaseScreen screen) {
		super(screen);
		setAsContainerOnly();
		setLayoutManager(new MigLayout(screen, "wrap 1, fill", "[grow]", "[][][]push"));

		adjusting = true;
		recreateCustomSequence();

		// Preset
		preset = new ComboBox<AnimationOption>(screen);
		preset.onChange(evt -> {
			if (!adjusting) {
				if (poseEditWindow != null) {
					editingPreset = evt.getNewValue();
					// poseEditWindow.setSequence((AnimationOption) value);
				}
				if (active) {
					AnimationOption s = evt.getNewValue();
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
		});
		preset.setToolTipText(
				"Bipeds have a set of pre-defined sequences. For all other creatures, or to create your own sequence, choose 'Custom'");

		// Preset Panel
		BaseElement presetPanel = new BaseElement(screen);
		presetPanel.setLayoutManager(new MigLayout(screen, "fill", "[grow][shrink 0][shrink 0][shrink 0]"));
		presetPanel.addElement(preset, "growx");

		// Start
		startPose = new PushButton(screen) {
			{
				setStyleClass("fancy");
			}
		};
		startPose.onMouseReleased(evt -> onStartPose());
		startPose.setText("Start");
		presetPanel.addElement(startPose);

		// Stop
		stopPose = new PushButton(screen) {
			{
				setStyleClass("fancy");
			}
		};
		stopPose.onMouseReleased(evt -> onStopPose());
		stopPose.setText("Stop");
		presetPanel.addElement(stopPose);

		// Edit
		edit = new PushButton(screen) {
			{
				setStyleClass("fancy");
			}
		};
		edit.onMouseReleased(evt -> {

			HUDMessageAppState hud = ToolKit.get().getApplication().getStateManager().getState(HUDMessageAppState.class);
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
		});
		edit.setText("Edit");
		presetPanel.addElement(edit);

		// Speed
		speed = new Slider<Float>(screen, Orientation.HORIZONTAL);
		speed.onChanged(evt -> {
			speedLabel.setText(String.format("%2.1f", evt.getNewValue()));
			onSpeedChange(evt.getNewValue());
		});
		speed.setSliderModel(new FloatRangeSliderModel(0, 10, 1, 0.1f));
		speed.setLockToStep(true);

		// Loop
		loop = new CheckBox(screen);
		loop.setText("Force Loop");

		// Options panel
		StyledContainer opts = new StyledContainer(screen);
		opts.setLayoutManager(new MigLayout(screen, "", "[shrink 0][fill, grow][shrink 0]48[shrink 0]", "[]"));
		opts.addElement(new Label("Speed", screen));
		opts.addElement(speed);
		opts.addElement(
				speedLabel = new Label(String.format("%2.1f", speed.getSliderModel().getValue().floatValue()), screen),
				"growx");
		ElementStyle.normal(speedLabel, true, false);
		opts.addElement(loop);

		// This
		addElement(ElementStyle.medium(new Label("Preset", screen)));
		addElement(presetPanel, "growx");
		addElement(opts, "growx");

		//
		setAvailable();
		adjusting = false;
	}

	public void setSelectedPreset(AnimationOption seq) {
		preset.runAdjusting(() -> preset.setSelectedByValue(seq));
	}

	// public void playPreset(AnimationSequence seq) {
	public void playPreset(AnimationOption seq) {
		LOG.info(String.format("Playing preset %s", seq));
		preset.setSelectedByValue(seq);
		onStartPose();
	}

	public boolean isForceLoop() {
		return loop.isChecked();
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
		final Menu<?> menu = preset.getMenu();
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
					preset.addListItem(Icelib.camelToEnglish(s.getKey()), s);
				}
			}
			preset.addListItem("Custom", customSequence);
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
		edit.setEnabled(preset.getSelectIndex() > -1);
		startPose.setEnabled(!active && preset.getSelectIndex() > -1);// &&
																		// !getPreset().getAnims().isEmpty());
		stopPose.setEnabled(active);
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
