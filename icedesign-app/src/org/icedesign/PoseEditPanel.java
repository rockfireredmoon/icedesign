package org.icedesign;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.icelib.Icelib;
import org.icescene.animation.AnimationSequence;

import com.jme3.animation.LoopMode;

import icetone.controls.buttons.CheckBox;
import icetone.controls.buttons.PushButton;
import icetone.controls.lists.ComboBox;
import icetone.controls.lists.FloatRangeSpinnerModel;
import icetone.controls.lists.Spinner;
import icetone.controls.menuing.Menu;
import icetone.controls.table.Table;
import icetone.controls.table.TableCell;
import icetone.controls.table.TableRow;
import icetone.controls.text.Label;
import icetone.controls.text.TextField;
import icetone.core.BaseElement;
import icetone.core.BaseScreen;
import icetone.core.Orientation;
import icetone.core.StyledContainer;
import icetone.core.layout.Border;
import icetone.core.layout.BorderLayout;
import icetone.core.layout.mig.MigLayout;

public class PoseEditPanel extends StyledContainer {

	private final Table animationSequence;
	private final ComboBox<String> pose;
	private final CheckBox loop;
	private final Spinner<Float> speed;
	private final FloatRangeSpinnerModel speedModel;
	private boolean adjusting;
	private AnimationSequence seq;
	private final TextField nameInput;
	private final PushButton up;
	private final PushButton test;
	private final PushButton remove;
	private final PushButton add;
	private final PushButton down;

	public PoseEditPanel(BaseScreen screen) {
		super(screen);

		setLayoutManager(new MigLayout(screen, "wrap 1, fill", "[grow]", "[shrink 0][grow][shrink 0]"));

		// Name
		StyledContainer north = new StyledContainer(screen);
		north.setLayoutManager(new BorderLayout());
		north.addElement(new Label("Name", screen), Border.WEST);
		north.addElement(nameInput = new TextField(screen), Border.CENTER);
		addElement(north, "growx");

		// Current sequence
		animationSequence = new Table(screen);
		animationSequence.onChanged(evt -> rebuildEdit());
		animationSequence.addColumn("Animation");
		animationSequence.setHeadersVisible(false);

		// Sequences buttons
		BaseElement sequenceActions = new BaseElement(screen);
		sequenceActions.setLayoutManager(new MigLayout(screen, "fill, wrap 1", "[grow]", "[align top]"));

		// Add new animation to sequence
		add = new PushButton(screen) {
			{
				setStyleClass("fancy");
			}
		};
		add.onMouseReleased(evt -> {
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
		});
		add.setText("Add");
		sequenceActions.addElement(add, "growx");

		// Remove animation from sequence
		remove = new PushButton(screen) {
			{
				setStyleClass("fancy");
			}
		};
		remove.onMouseReleased(evt -> {
			AnimationSequence.Anim anim = getAnim();
			seq.removeAnim(anim);
			rebuildSequence();
		});
		remove.setText("Remove");
		sequenceActions.addElement(remove, "growx");

		// Test
		test = new PushButton(screen) {
			{
				setStyleClass("fancy");
			}
		};
		test.onMouseReleased(evt -> onStartAnimation());
		test.setText("Test");
		sequenceActions.addElement(test, "growx");

		// Up
		up = new PushButton(screen) {
			{
				setStyleClass("fancy");
			}
		};
		up.onMouseReleased(evt -> {
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
		});
		up.setText("Up");
		sequenceActions.addElement(up, "growx");

		// Down
		down = new PushButton(screen) {
			{
				setStyleClass("fancy");
			}
		};
		down.onMouseReleased(evt -> {

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
		});
		down.setText("Down");
		sequenceActions.addElement(down, "growx");

		// Sequence Panel
		BaseElement seqPanel = new BaseElement(screen);
		seqPanel.setLayoutManager(new MigLayout(screen, "fill, wrap 2, ins 0", "[grow][]", "[grow]"));
		seqPanel.addElement(animationSequence, "growx, growy");
		seqPanel.addElement(sequenceActions);

		// Edit animation element panel
		BaseElement edit = new BaseElement(screen);
		edit.setLayoutManager(new MigLayout(screen, "wrap 2", "[][:200:]", "[][]"));

		// Pose
		edit.addElement(new Label("Animation", screen));
		edit.addElement(pose = new ComboBox<String>(screen).onChange(evt -> {
			if (!adjusting) {
				getAnim().setAnimName(evt.getNewValue());
				rebuildSequence();
			}
		}), "growx");

		// Speed
		edit.addElement(new Label("Speed", screen));
		edit.addElement(speed = new Spinner<Float>(screen, Orientation.HORIZONTAL, true)
				.onChange(evt -> getAnim().setSpeed(evt.getNewValue())), "");
		speedModel = new FloatRangeSpinnerModel(0.1f, 10f, 0.1f, 1f);
		speed.setSpinnerModel(speedModel);

		// Loop
		edit.addElement(loop = new CheckBox(screen), "span 2, growx");
		loop.onChange((evt) -> {
			if (!evt.getSource().isAdjusting())
				getAnim().setLoopMode(loop.isChecked() ? LoopMode.Loop : LoopMode.DontLoop);
		});
		loop.setText("Loop");

		// This
		addElement(seqPanel, "growx");
		addElement(edit, "growx");
	}

	public void onStartAnimation() {
	}

	public AnimationSequence.Anim getAnim() {
		List<TableRow> sel = animationSequence.getSelectedRows();
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
		return loop.isChecked();
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
		add.setEnabled(pose.getMenu() != null && !pose.getListItems().isEmpty());
		remove.setEnabled(animationSequence.getSelectedRow() != null);
		up.setEnabled(animationSequence.getSelectedRow() != null);
		down.setEnabled(animationSequence.getSelectedRow() != null);
		test.setEnabled(animationSequence.getSelectedRow() != null);
	}

	private void rebuildSequence() {
		adjusting = true;
		try {
			// Get the current selection objects
			List<Object> sel = animationSequence.getSelectedObjects();
			animationSequence.invalidate();
			animationSequence.removeAllRows();
			AnimationSequence.SubGroup g = null;
			TableRow r = null;
			for (Map.Entry<AnimationSequence.SubGroup, List<AnimationSequence.Anim>> en : seq.getAnims().entrySet()) {
				if ((g == null || !en.getKey().equals(g)) && !en.getKey().equals(AnimationSequence.SubGroup.NONE)) {
					r = new TableRow(screen, animationSequence, en.getKey());
					r.setExpanded(true);
					r.setLeaf(false);
					TableCell c = new TableCell(screen, Icelib.toEnglish(en.getKey()), en.getKey());
					r.addElement(c);
					animationSequence.addRow(r);
					g = en.getKey();
				}
				for (AnimationSequence.Anim s : en.getValue()) {
					String n = s.getAnimName();
					TableRow cr = new TableRow(screen, animationSequence, s);
					TableCell c = new TableCell(screen, n, s);
					cr.addElement(c);
					if (r != null) {
						r.addRow(cr);
					} else {
						animationSequence.addRow(cr);
					}
				}
			}
			animationSequence.validate();
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
				pose.runAdjusting(() -> pose.setSelectedByValue(a.getAnimName()));
			}
			loop.runAdjusting(() -> loop.setChecked(a.getLoopMode().equals(LoopMode.Loop)));
			speed.setSelectedValue(a.getSpeed());
		}
		setAvailable();
	}

	public AnimationSequence getSequence() {
		return seq;
	}
}
