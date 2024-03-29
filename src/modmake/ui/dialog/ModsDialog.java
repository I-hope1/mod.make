package modmake.ui.dialog;

import arc.Core;
import arc.files.*;
import arc.graphics.Texture;
import arc.graphics.g2d.TextureRegion;
import arc.scene.ui.TextButton;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import mindustry.Vars;
import mindustry.gen.*;
import mindustry.graphics.Pal;
import mindustry.ui.*;
import mindustry.ui.dialogs.BaseDialog;
import modmake.components.*;
import modmake.ui.MyStyles;
import modmake.util.load.LoadMod;

import java.util.Objects;

import static mindustry.Vars.ui;
import static modmake.components.DataHandle.*;

public class ModsDialog extends Window {

	Table   pane;
	Seq<Fi> mods = new Seq<>();
	float   h    = 110, w = Vars.mobile ? (Core.graphics.getWidth() > Core.graphics.getHeight() ? 50 : 0) + 440 : 524;

	public ModsDialog() {
		super("mods", 120, 80, true, false);
	}

	public void load() {
		// modDialog.load();
		// modMetaDialog.load();
		// jsonDialog.load();
		// modMetaDialog.hidden(this::setup);

		// addCloseListener();

		style = Styles.defaultt;
		margin = 12;
		pane = new Table();
		pane.margin(10).top();

		cont.add("@mod.advise").top().row();
		cont.table(Styles.none, t -> t.pane(pane).scrollX(false).fillX().fillY()).row();

		buttons.button("@back", Icon.left, style, this::hide).margin(margin).size(210, 60);
		buttons.button("@mod.add", Icon.add, style, () -> {
			BaseDialog                 dialog = new BaseDialog("@mod.add");
			TextButton.TextButtonStyle bstyle = Styles.cleart;

			dialog.cont.table(Tex.button, t -> {
				t.defaults().left().size(300, 70);
				t.margin(12);

				t.button("@mod.import.file", Icon.file, bstyle, () -> {
					Vars.platform.showMultiFileChooser(file -> {
						importMod(file);
						setup();
					}, "zip", "jar");
				}).margin(12).row();
				t.button("@mod.add", Icon.add, bstyle, () -> {
					var d = new ModMetaDialog();
					d.load();
					d.show(modsDirectory.child("tmp").child("mod.hjson"));
				}).margin(12);
			});
			dialog.addCloseButton();
			dialog.show();
		}).margin(margin).size(210, 64).row();

		if (!Vars.mobile) buttons.button("@mods.openfolder", Icon.link, style, () -> {
			Core.app.openFolder(dataDirectory.absolutePath());
		}).margin(margin).size(210, 64);
		buttons.button("@quit", Icon.exit, style, () -> Core.app.exit()).margin(margin).size(210, 64);

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
			resolveMod(root, toFile, list);
		} catch (Exception err) {
			ui.showException(err);
		} finally {
			if (root != null) root.delete();
		}
	}
	private static void resolveMod(Fi root, Fi toFile, Fi[] list) {
		Fi currentFile;
		if (list.length == 1) {
			if (list[0].isDirectory()) {
				currentFile = list[0];
			} else {
				throw new IllegalArgumentException(Core.bundle.get("file.content.illegal"));
			}
		} else {
			currentFile = root;
		}
		if (!currentFile.child("mod.json").exists() && !currentFile.child("mod.hjson").exists()) {
			throw new IllegalArgumentException("没有mod.(h)json");
		}
		currentFile.copyTo(toFile.parent());
	}
	public static final Fi modsDirectory = dataDirectory.child("mods");
	TextButton.TextButtonStyle style;
	float                      margin;

	public void setup() {
		Table p = pane;
		p.clearChildren();
		mods = new Seq<>(modsDirectory.list());
		if (mods.size == 0) {
			p.table(Styles.black6, t -> t.add("@mods.none")).height(80);
			return;
		}

		mods.each(file -> {
			if (Objects.equals(file.name(), "tmp")) return;
			MyMod mod = MyMod.set(file);
			if (mod == null) return;

			buildMod(p, file, mod);
		});
	}
	private void buildMod(Table p, Fi file, MyMod mod) {
		p.button(b -> {
			b.top().left();
			b.margin(12);
			b.defaults().left().top();

			b.table(title -> {
				buildTitle(mod, title);
			});
			b.table(right -> {
				buildRight(file, mod, right);
			}).growX().right().padRight(-8).padTop(-8).fill();
		}, MyStyles.clearpb, () -> {
			// hide();
			var d = new ModDialog();
			d.load();
			d.show(mod);
		}).size(w, h).growX().pad(4).row();
	}
	private void buildRight(Fi file, MyMod mod, Table right) {
		//					right.fillParent = true;
		right.right();
		right.button(Icon.edit, MyStyles.clearPartiali, () -> {
			var d = new ModMetaDialog();
			d.load();
			d.show(mod.root.child("mod.json").exists()
			 ? mod.root.child("mod.json") : mod.root.child("mod.hjson"));
		}).size(50);
		right.button(Icon.trash, MyStyles.clearPartiali, () ->
		 ui.showConfirm("@confirm", "@mod.remove.confirm", () -> {
			 file.deleteDirectory();
			 setup();
		 })
		).size(50).row();
		right.button(Icon.upload, MyStyles.clearPartiali, () -> {
			exportMod(mod);
		}).size(50).disabled(__ -> Vars.state.isGame() && dsettings.getBool("auto_load_mod"));
		right.button(Icon.link, MyStyles.clearPartiali, () -> Core.app.openFolder(mod.root.absolutePath())).size(50);
	}
	private static void exportMod(MyMod mod) {
		Fi      dir    = Vars.modDirectory;
		boolean enable = dsettings.getBool("auto_load_mod");
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
	}
	private void buildTitle(MyMod mod, Table title) {
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
	}
}
