package modmake.ui.dialog;

import arc.Core;
import arc.files.Fi;
import arc.files.ZipFi;
import arc.graphics.Texture;
import arc.graphics.g2d.TextureRegion;
import arc.scene.ui.TextButton;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import arc.util.Log;
import mindustry.Vars;
import mindustry.gen.Icon;
import mindustry.gen.Tex;
import mindustry.graphics.Pal;
import mindustry.ui.BorderImage;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;
import modmake.components.MyMod;
import modmake.ui.styles;
import modmake.util.LoadMod;

import java.util.Objects;

import static mindustry.Vars.ui;
import static modmake.IntUI.modDialog;
import static modmake.IntUI.modMetaDialog;
import static modmake.components.DataHandle.settings;

public class ModsDialog extends BaseDialog {

	Table pane;
	Seq<Fi> mods = new Seq<>();
	float h = 110, w = Vars.mobile ? (Core.graphics.getWidth() > Core.graphics.getHeight() ? 50 : 0) + 440 : 524;

	public ModsDialog() {
		super("mods");
	}

	public void load() {
		modDialog.load();
		modMetaDialog.load();

		try {
			LoadMod.init();
		} catch (Exception e) {
			Log.err(e);
		}
		addCloseListener();

		style = Styles.defaultt;
		margin = 12;
		pane = new Table();
		pane.margin(10).top();

		cont.add("$mod.advise").top().row();
		cont.table(Styles.none, t -> t.pane(pane).scrollX(false).fillX().fillY()).row();

		buttons.button("$back", Icon.left, style, this::hide).margin(margin).size(210, 60);
		buttons.button("$mod.add", Icon.add, style, () -> {
			BaseDialog dialog = new BaseDialog("$mod.add");
			TextButton.TextButtonStyle bstyle = Styles.cleart;

			dialog.cont.table(Tex.button, t -> {
				t.defaults().left().size(300, 70);
				t.margin(12);

				t.button("$mod.import.file", Icon.file, bstyle, () -> {
					hide();

					Vars.platform.showMultiFileChooser(file -> {
						importMod(file);
						setup();
					}, "zip", "jar");
				}).margin(12).row();
				t.button("$mod.add", Icon.add, bstyle, () -> {
					modMetaDialog.show(modsDirectory.child("tmp").child("mod.hjson"));
					setup();
				}).margin(12);
			});
			dialog.addCloseButton();
			dialog.show();
		}).margin(margin).size(210, 64).row();

		if (!Vars.mobile) buttons.button("$mods.openfolder", Icon.link, style, () -> {
			Core.app.openFolder(dataDirectory.absolutePath());
		}).margin(margin).size(210, 64);
		buttons.button("$quit", Icon.exit, style, () -> Core.app.exit()).margin(margin).size(210, 64);

		setup();

	/* let { ParseListener } = Packages.rhino.NativeJavaClass(Vars.mods.scripts.scope, Packages.mindustry.mod.ContentParser);
	let times = 0
	Vars.mods.addParseListener(new ParseListener({
		parsed: () -> {
			// 每执行n次休眠1秒
			if (++times > settings.getBool("compiling_times_per_second")) Threads.sleep(1000);
		}
	})) */
	}

	public void importMod(Fi file) {
		Fi root = null, currentFile;
		try {
			Fi toFile = modsDirectory.child(file.nameWithoutExtension());
			if (!toFile.isDirectory()) toFile.delete();

			root = new ZipFi(file);
			Fi[] list = root.list();
			if (list.length == 1) {
				if (list[0].isDirectory()) {
					currentFile = list[0];
				} else {
					throw new IllegalArgumentException("文件内容不合法");
				}
			} else {
				currentFile = root;
			}
			if (!currentFile.child("mod.json").exists() && !currentFile.child("mod.hjson").exists()) {
				throw new IllegalArgumentException("没有mod.(h)json");
			}
			currentFile.copyTo(toFile.parent());
		} catch (Exception err) {
			ui.showException(err);
		} finally {
			if (root != null) root.delete();
		}
	}

	Fi dataDirectory = Vars.dataDirectory.child("mods(I hope...)");
	Fi modsDirectory = dataDirectory.child("mods");
	TextButton.TextButtonStyle style;
	float margin;

	public void setup() {
		Table p = pane;
		p.clearChildren();
		mods = new Seq<>(modsDirectory.list());
		if (mods.size == 0) {
			p.table(Styles.black6, t -> t.add("$mods.none")).height(80);
			return;
		}

		mods.each(file -> {
			if (Objects.equals(file.name(), "tmp")) return;
			MyMod mod = MyMod.set(file);
			if (mod == null) return;

			p.button(b -> {
				b.top().left();
				b.margin(12);
				b.defaults().left().top();

				b.table(title -> {
					title.left();

					var image = new BorderImage();
					if (mod.root.child("icon.png").exists()) {
						try {
							image.setDrawable(new TextureRegion(new Texture(mod.root.child("icon.png"))));
						} catch (Exception e) {
							image.setDrawable(Tex.nomap);
						}
					} else {
						image.setDrawable(Tex.nomap);
					}
					image.border(Pal.accent);
					title.add(image).size(h - 8).padTop(-8).padLeft(-8).padRight(8);

					title.table(text -> {
						text.add("[accent]" + mod.displayName() + "\n[lightgray]v" +
								mod.meta.getString("version", "???")).wrap().width(300).growX().left();

					}).top().growX();

					title.add().growX().left();
				});
				b.table(right -> {
					right.right();
					right.button(Icon.edit, Styles.clearPartiali, () -> {
						modMetaDialog.show(mod.root.child("mod.json").exists()
								? mod.root.child("mod.json") : mod.root.child("mod.hjson"));
					}).size(50);
					right.button(Icon.trash, Styles.clearPartiali, () ->
							ui.showConfirm("$confirm", "$mod.remove.confirm", () -> {
								file.deleteDirectory();
								setup();
							})
					).size(50).row();
					right.button(Icon.upload, Styles.clearPartiali, () -> {
						Fi dir = Vars.modDirectory;
						boolean enable = settings.getBool("auto_load_mod");
						Runnable upload = () -> {
							if (enable) {
								if (!LoadMod.load(mod)) {
									ui.showInfo("导出失败！");
									return;
								}
							} else {
								dir.child(mod.root.name()).deleteDirectory();
								mod.root.copyTo(dir);
							}
							ui.showInfo("导出成功！");
						};

						if (dir.child(mod.root.name()).exists() && !enable) {
							ui.showConfirm("替换", "同名文件已存在\n是否要替换", upload);
						} else upload.run();
					}).size(50).disabled(__ -> Vars.state.isGame() && settings.getBool("auto_load_mod"));
					right.button(Icon.link, Styles.clearPartiali, () -> Core.app.openFolder(mod.root.absolutePath())).size(50);
				}).growX().right().padRight(-8).padTop(-8);
			}, styles.clearpb, () -> {
				hide();
				modDialog.show(mod);
			}).size(w, h).growX().pad(4).row();
		});
	}
}
