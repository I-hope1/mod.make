package modmake.ui.dialog;

import arc.Core;
import arc.files.Fi;
import arc.graphics.Color;
import arc.graphics.Texture;
import arc.graphics.g2d.TextureRegion;
import arc.input.KeyCode;
import arc.scene.event.InputEvent;
import arc.scene.event.InputListener;
import arc.scene.ui.Dialog;
import arc.scene.ui.Image;
import arc.scene.ui.Label;
import arc.scene.ui.TextField;
import arc.scene.ui.layout.Table;
import arc.util.Align;
import arc.util.Log;
import mindustry.Vars;
import mindustry.gen.Icon;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;
import modmake.IntUI;

import java.util.Objects;

import static modmake.IntUI.imgDialog;

public class SpriteDialog extends BaseDialog {
	Table _cont = new Table(Table::top);
	public Runnable hiddenRun = null;
	public Fi root;

	public SpriteDialog() {
		super("图片库");

		cont.pane(_cont).fillX().fillY();
		addCloseButton();
		imgDialog.hiddenRun = () -> {
			hide();
			setup(root);
		};
		buttons.button("@add", Icon.add, () -> IntUI.createDialog("添加图片",
				"新建图片", "默认32*32", Icon.add, (Runnable) () -> {
					new Dialog("") {{
						var name = new TextField();

						cont.table((t -> {
							t.add("$name");
							t.add(name).growX().valid(__ -> !root.child(name.getText() + ".png").exists());
						})).growX().row();

						buttons.button("$back", this::hide).size(150, 64);
						buttons.button("$ok", (() -> {
							Fi file = root.child(name.getText() + ".png");
							imgDialog.beginEditImg(file);
							hide();
						})).size(150, 64).disabled(__ -> !name.isValid());
						closeOnBack();

						show();
					}};
					/*Fi fi1;
					int i = 0;
					do {
						fi1 = root.child("new(" + i + ").png");
					} while (fi1.exists());
					imgDialog.beginEditImg(fi1);*/
				},
				"导入图片", "仅限png", Icon.download, (Runnable) () -> Vars.platform.showFileChooser(true, "import file to add sprite", "png", f -> {
					Fi toFile = root.child(f.name());
					Runnable go = () -> {
						try {
							buildImage(_cont, f);
						} catch (Exception err) {
							Vars.ui.showException("文件可能损坏", err);
							return;
						}
						f.copyTo(toFile);
					};
					if (toFile.exists()) Vars.ui.showConfirm("$confirm", "是否要覆盖", go);
					else go.run();
				})
		)).size(210, 64);

		hidden(() -> {
			if (hiddenRun != null) hiddenRun.run();

			/*InputEvent inputEvent = new InputEvent();
			inputEvent.keyCode = KeyCode.enter;
			inputEvent.type = InputEvent.InputEventType.keyUp;
			Core.scene.getKeyboardFocus().fire(inputEvent);*/
		});
	}

	public void setup(Fi all) {
		root = all;
		_cont.clearChildren();
		if (all != null) {
			all.walk(f -> {
				try {
					buildImage(_cont, f);
				} catch (Exception e) {
					Log.err(e);
				}
			});
		}
		show();
	}


	public void buildImage(Table table, Fi fi) {
		if (!Objects.equals(fi.extension(), "png")) return;
		Fi[] file = {fi};
		table.table(t -> {
			t.left();

			var label = new Label(file[0].nameWithoutExtension());
			label.setAlignment(Align.left, Align.center);
			var field = new TextField();
			var cell = t.add(label).growX();
			field.addListener(new InputListener() {
				@Override
				public boolean keyUp(InputEvent event, KeyCode key) {
					if (key == KeyCode.enter) {
						Fi toFile;
						String toName = field.getText();
						try {
							toFile = file[0].sibling(toName + ".png");
						} catch (Exception e) {
							Vars.ui.showException("文件名称不合法", e);
							return false;
						}
						file[0].moveTo(toFile);
						file[0] = toFile;
						label.setText(toName);
						cell.setElement(label);
					}
					return false;
				}
			});
			field.update(() -> {
				if (Core.scene.getKeyboardFocus() != field) {
					InputEvent inputEvent = new InputEvent();
					inputEvent.keyCode = KeyCode.enter;
					inputEvent.type = InputEvent.InputEventType.keyUp;
					field.fire(inputEvent);
				}
			});
			label.clicked(() -> {
				field.setText(file[0].nameWithoutExtension());
				Core.scene.setKeyboardFocus(field);
				cell.setElement(field);
			});
			t.button("", Icon.trash, Styles.cleart, () -> {
				t.remove();
				file[0].delete();
			});
			t.row();
			t.image().color(Color.gray).minWidth(440).row();
			Image img = new Image(new TextureRegion(new Texture(file[0])));
			img.clicked(() -> {
				hide();
				imgDialog.beginEditImg(file[0]);
			});
			t.pane(p -> p.add(img)).growX().height(96);
		}).padTop(10).left().row();
	}
}
