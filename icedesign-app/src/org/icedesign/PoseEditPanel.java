package org.icedesign;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.icelib.Icelib;
import org.icescene.animation.AnimationSequence;
import org.iceui.controls.FancyButton;

import com.jme3.animation.LoopMode;
import com.jme3.input.event.MouseButtonEvent;

import icetone.controls.buttons.CheckBox;
import icetone.controls.lists.ComboBox;
import icetone.controls.lists.FloatRangeSpinnerModel;
import icetone.controls.lists.Spinner;
import icetone.controls.lists.Table;
import icetone.controls.menuing.Menu;
import icetone.controls.text.Label;
import icetone.controls.text.TextField;
import icetone.core.Container;
import icetone.core.Element;
import icetone.core.ElementManager;
import icetone.core.layout.BorderLayout;
import icetone.core.layout.mig.MigLayout;

public class PoseEditPanel extends Container {

	private final Table animationSequence;
	private final ComboBox<String> pose;
	private final CheckBox loop;
	private final Spinner<Float> speed;
	private final FloatRangeSpinnerModel speedModel;
	private boolean adjusting;
	private AnimationSequence seq;
	private final TextField nameInput;
	private final FancyButton up;
	private final FancyButton test;
	private final FancyButton remove;
	private final FancyButton add;
	private final FancyButton down;

	public PoseEditPanel(ElementManager screen) {
		super(screen);

		setLayoutManager(new MigLayout(screen, "wrap 1, fill", "[grow]", "[shrink 0][grow][shrink 0]"));

		// Name
		Container north = new Container(screen);
		north.setLayoutManager(new BorderLayout());
		north.addChild(new Label("Name", screen), BorderLayout.Border.WEST);
		north.addChild(nameInput = new TextField(screen), BorderLayout.Border.CENTER);
		addChild(north, "growx");

		// Current sequence
		animationSequence = new Table(screen) {
			@Override
			public void onChange() {
				rebuildEdit();
			}
		};
		animationSequence.addColumn("Animation");
		animationSequence.setHeadersVisible(false);

		// Sequences buttons
		Element sequenceActions = new Element(screen);
		sequenceActions.setLayoutManager(new MigLayout(screen, "fill, wrap 1", "[grow]", "[align top]"));

		// Add new animation to sequence
		add = new FancyButton(screen) {
			@Override
			public void onButtonMouseLeftDown(MouseButtonEvent evt, boolean toggle) {
				super.onButtonMouseLeftDown(evt, toggle);
				AnimationSequence.SubGroup group = AnimationSequence.SubGroup.NONE;
				AnimationSequence.Part part = AnimationSequence.Part.ALL;
				AnimationSequence.Anim currentAnim = getAnim();
				if (currentAnim != null) {
					group = currentAnim.getSubGroup();
					part = currentAnim.getPart();
				}
				String pose = getPose();
				AnimationSequence.Anim anim = new AnimationSequence.Anim(group, part, pose);
				seq.addAnim(anim);
				rebuildSequence();
			}
		};
		add.setText("Add");
		sequenceActions.addChild(add, "growx");

		// Remove animation from sequence
		remove = new FancyButton(screen) {
			@Override
			public void onButtonMouseLeftDown(MouseButtonEvent evt, boolean toggle) {
				super.onButtonMouseLeftDown(evt, toggle);
				AnimationSequence.Anim anim = getAnim();
				seq.removeAnim(anim);
				rebuildSequence();
			}
		};
		remove.setText("Remove");
		sequenceActions.addChild(remove, "growx");

		// Test
		test = new FancyButton(screen) {
			@Override
			public void onButtonMouseLeftDown(MouseButtonEvent evt, boolean toggle) {
				super.onButtonMouseLeftDown(evt, toggle);
				onStartAnimation();
			}
		};
		test.setText("Test");
		sequenceActions.addChild(test, "growx");

		// Up
		up = new FancyButton(screen) {
			@Override
			public void onButtonMouseLeftDown(MouseButtonEvent evt, boolean toggle) {
				super.onButtonMouseLeftDown(evt, toggle);
				AnimationSequence.Anim anim = getAnim();
				if (anim != null) {
					List<AnimationSequence.Anim> s = seq.getAnims().get(anim.getSubGroup());
					int idx = s.indexOf(anim);
					if (idx > 0) {
						AnimationSequence.Anim other = s.get(idx - 1);
						s.set(idx - 1, anim);
						s.set(idx, other);
						rebuildSequence();
					}
				}
			}
		};
		up.setText("Up");
		sequenceActions.addChild(up, "growx");

		// Down
		down = new FancyButton(screen) {
			@Override
			public void onButtonMouseLeftDown(MouseButtonEvent evt, boolean toggle) {
				super.onButtonMouseLeftDown(evt, toggle);
				AnimationSequence.Anim anim = getAnim();
				if (anim != null) {
					List<AnimationSequence.Anim> s = seq.getAnims().get(anim.getSubGroup());
					int idx = s.indexOf(anim);
					if (idx < s.size() - 1) {
						AnimationSequence.Anim other = s.get(idx + 1);
						s.set(idx + 1, anim);
						s.set(idx, other);
						rebuildSequence();
					}
				}
			}
		};
		down.setText("Down");
		sequenceActions.addChild(down, "growx");

		// Sequence Panel
		Element seqPanel = new Element(screen);
		seqPanel.setLayoutManager(new MigLayout(screen, "fill, wrap 2, ins 0", "[grow][]", "[grow]"));
		seqPanel.addChild(animationSequence, "growx, growy");
		seqPanel.addChild(sequenceActions);

		// Edit animation element panel
		Element edit = new Element(screen);
		edit.setLayoutManager(new MigLayout(screen, "wrap 2", "[][:200:]", "[][]"));

		// Pose
		edit.addChild(new Label("Animation", screen));
		edit.addChild(pose = new ComboBox<String>(screen) {
			@Override
			public void onChange(int selectedIndex, String value) {
				if (!adjusting) {
					getAnim().setAnimName(value);
					rebuildSequence();
				}
			}
		}, "growx");

		// Speed
		edit.addChild(new Label("Speed", screen));
		edit.addChild(speed = new Spinner<Float>(screen, Orientation.HORIZONTAL, true) {
			@Override
			public void onChange(Float value) {
				getAnim().setSpeed(value);
			}
		}, "");
		speedModel = new FloatRangeSpinnerModel(0.1f, 10f, 0.1f, 1f);
		speed.setSpinnerModel(speedModel);

		// Loop
		edit.addChild(loop = new CheckBox(screen) {
			@Override
			public void onMouseLeftPressed(MouseButtonEvent evt) {
				super.onMouseLeftPressed(evt);
				getAnim().setLoopMode(getIsChecked() ? LoopMode.Loop : LoopMode.DontLoop);
			}
		}, "span 2, growx");
		loop.setLabelText("Loop");

		// This
		addChild(seqPanel, "growx");
		addChild(edit, "growx");
	}

	public void onStartAnimation() {
	}

	public AnimationSequence.Anim getAnim() {
		List<Table.TableRow> sel = animationSequence.getSelectedRows();
		if (!sel.isEmpty()) {
			Object o = sel.get(0).getValue();
			if (o instanceof AnimationSequence.Anim) {
				return (AnimationSequence.Anim) o;
			}
		}
		return null;
	}

	public String getPose() {
		final Menu<String> menu = pose.getMenu();
		if (menu != null && pose.getSelectIndex() < menu.getMenuItems().size()) {
			return pose.getSelectedListItem().getValue();
		}
		return null;
	}

	public boolean isLoop() {
		return loop.getIsChecked();
	}

	public float getSpeed() {
		return speedModel.getCurrentValue();
	}

	public void setSequence(AnimationSequence seq) {
		this.seq = seq.clone();
		nameInput.setText(seq.getName());
		rebuildSequence();
	}

	public void setAnimations(Collection<String> animations) {
		adjusting = true;
		try {
			pose.removeAllListItems();
			if (animations != null && !animations.isEmpty()) {
				for (String a : Icelib.sort(animations)) {
					pose.addListItem(a, a);
				}
			}
			setAvailable();
		} finally {
			adjusting = false;
		}
	}

	private void setAvailable() {
		add.setIsEnabled(pose.getMenu() != null && !pose.getListItems().isEmpty());
		remove.setIsEnabled(animationSequence.getSelectedRow() != null);
		up.setIsEnabled(animationSequence.getSelectedRow() != null);
		down.setIsEnabled(animationSequence.getSelectedRow() != null);
		test.setIsEnabled(animationSequence.getSelectedRow() != null);
	}

	private void rebuildSequence() {
		adjusting = true;
		try {
			// Get the current selection objects
			List<Object> sel = animationSequence.getSelectedObjects();

			animationSequence.removeAllRows();
			AnimationSequence.SubGroup g = null;
			Table.TableRow r = null;
			for (Map.Entry<AnimationSequence.SubGroup, List<AnimationSequence.Anim>> en : seq.getAnims().entrySet()) {
				if ((g == null || !en.getKey().equals(g)) && !en.getKey().equals(AnimationSequence.SubGroup.NONE)) {
					r = new Table.TableRow(screen, animationSequence, en.getKey());
					r.setExpanded(true);
					r.setLeaf(false);
					Table.TableCell c = new Table.TableCell(screen, Icelib.toEnglish(en.getKey()), en.getKey());
					r.addChild(c);
					animationSequence.addRow(r);
					g = en.getKey();
				}
				for (AnimationSequence.Anim s : en.getValue()) {
					String n = s.getAnimName();
					Table.TableRow cr = new Table.TableRow(screen, animationSequence, s);
					Table.TableCell c = new Table.TableCell(screen, n, s);
					cr.addChild(c);
					if (r != null) {
						r.addRow(cr);
					} else {
						animationSequence.addRow(cr);
					}
				}
			}
			animationSequence.pack();
			if (sel.isEmpty()) {
				if (animationSequence.getRowCount() > 0) {
					animationSequence.setSelectedRowIndex(0);
				}
			} else {
				animationSequence.setSelectedRowObjects(sel);
			}
		} finally {
			adjusting = false;
		}
	}

	private void rebuildEdit() {
		AnimationSequence.Anim a = getAnim();
		if (a != null) {
			if (pose.getMenu() != null && !pose.getListItems().isEmpty()) {
				pose.setSelectedByValue(a.getAnimName(), false);
			}
			loop.setIsCheckedNoCallback(a.getLoopMode().equals(LoopMode.Loop));
			speed.setSelectedValue(a.getSpeed());
		}
		setAvailable();
	}

	public AnimationSequence getSequence() {
		return seq;
	}
}
