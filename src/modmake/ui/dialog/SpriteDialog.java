package modmake.ui.dialog;

import arc.files.Fi;
import arc.func.Cons;
import arc.graphics.Pixmap;
import arc.graphics.Texture;
import arc.graphics.g2d.TextureRegion;
import arc.scene.ui.Button;
import arc.scene.ui.Tooltip;
import arc.scene.ui.layout.Table;
import arc.struct.ObjectMap;
import arc.util.Log;
import mindustry.Vars;
import mindustry.gen.Icon;
import mindustry.gen.Tex;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;
import modmake.IntUI;
import modmake.components.MyMod;

import java.util.regex.Pattern;

import static modmake.IntUI.*;

public class SpriteDialog extends BaseDialog {
	Table p;
	public Runnable hiddenRun = null;
	public Fi root;

	public ObjectMap<String, Pixmap> spriteMap;

	public SpriteDialog() {
		super("图片库");

		addCloseButton();
		imgDialog.hiddenRun = () -> {
			hide();
			setup(root);
		};
		buttons.button("@add", Icon.add, () -> IntUI.createDialog("添加图片",
				"新建图片", "默认32*32", Icon.add, (Runnable) () -> {
					nameDialog.show(text -> {
						Fi file = root.child(text + ".png");
						imgDialog.beginEditImg(file);
					}, text -> !root.child(text + ".png").exists());
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
						f.copyTo(toFile);
						try {
							buildImage(p, toFile);
							spriteMap.put(toFile.nameWithoutExtension(), new Pixmap(toFile));
						} catch (Exception err) {
							Vars.ui.showException("文件可能损坏", err);
						}
					};
					if (toFile.exists()) {
						Vars.ui.showConfirm("$confirm", "是否要覆盖", go);
					} else {
						go.run();
					}
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
		cont.clearChildren();
		root = all;
		if (all != null) {
			MyMod mod = modDialog.currentMod;
			if (root.name().equals("sprites")) {
				spriteMap = mod.sprites1;
			} else if (root.name().equals("sprites-override")) {
				spriteMap = mod.sprites2;
			}
			searchTable(cont, (p, text) -> {
				p.clearChildren();
				this.p = p;
				Pattern pattern = Pattern.compile(text, Pattern.CASE_INSENSITIVE);
				all.walk(f -> {
					try {
						if (!pattern.matcher(f.nameWithoutExtension()).find()) return;
						buildImage(p, f);
					} catch (Exception e) {
						Log.err(e);
					}
				});
			});
		}
		show();
	}


	public void buildImage(Table table, Fi fi) {
		if (!fi.extEquals("png")) return;
		Fi[] file = {fi};
		/*table.table(t -> {
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
			BorderImage img = new BorderImage(new Texture(file[0]));
			img.border(Pal.accent);
			img.clicked(() -> {
				hide();
				imgDialog.beginEditImg(file[0]);
			});
			t.pane(p -> p.add(img)).growX().minHeight(96);
		}).padTop(10).left().row();*/

		var ref = new Object() {
			Cons<Table> setup = null;
		};
		ref.setup = t -> {
			t.clearChildren();
			Button button = new Button(Styles.defaultb);

			button.left();
			Texture texture = new Texture(file[0]);
			float w = texture.getTextureData().getWidth();
			float h = texture.getTextureData().getHeight();
			float srcW, srcH;
			if (w > h) {
				srcW = 45;
				srcH = 45 * h / w;
			} else {
				srcW = 45 * w / h;
				srcH = 45;
			}
			TextureRegion region = new TextureRegion(texture);
			Table image = button.table(b1 -> {
				b1.image(region).size(srcW, srcH);
			}).size(45).pad(4).get();
			image.addListener(new Tooltip(tool -> tool.background(Tex.button)
					.image(region).pad(4)));
			button.add(file[0].nameWithoutExtension()).padLeft(4);
			t.add(button).size(360, 64);
			IntUI.longPress(button, 600, b -> {
				if (b) {
//					if (Core.input.useKeyboard()) return;

					nameDialog.show(text -> {
						Fi toFile = file[0].sibling(text + ".png");
						file[0].moveTo(toFile);
						file[0] = toFile;
						ref.setup.get(t);
					}, text -> !file[0].sibling(text + ".png").exists(), file[0].nameWithoutExtension());
				} else {
					hide();
					imgDialog.beginEditImg(file[0]);
				}
			});
			t.button(b -> {
				b.image(Icon.trash);
			}, Styles.defaultb, () -> {
				file[0].delete();
				spriteMap.remove(file[0].nameWithoutExtension());
				t.remove();
			}).growX().growY();
		};

		table.table(ref.setup).pad(10).padBottom(3).padTop(0).row();
	}
}
