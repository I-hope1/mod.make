package modmake.ui.dialog;

import arc.Core;
import arc.files.Fi;
import arc.graphics.Color;
import arc.scene.event.VisibilityListener;
import arc.scene.ui.Label;
import arc.scene.ui.layout.Table;
import mindustry.Vars;
import mindustry.gen.Icon;
import mindustry.ui.Styles;
import modmake.components.*;
import modmake.ui.MyStyles;
import modmake.util.tools.FilterTable;

public class JsonDialog extends Window {
	Label label;
	Table p;
	Fi    file;
	MyMod mod;
	float w = Core.graphics.getWidth(),
	 h      = Core.graphics.getHeight(),
	 bw     = w > h ? 550 : 450,
	 bh     = w > h ? 200 : Vars.mobile ? 300 : 350;

	public JsonDialog() {
		super("json", 120, 80, true, false);
	}


	public void load() {
		label = new Label("");
		p = new Table();
		p.update(() -> {
			if (file == null || !file.exists()) hide();
		});
		p.center();
		p.defaults().padTop(10).left();
		p.add("@editor.sourceCode", Color.gray).padRight(10).padTop(0).row();
		p.table(t -> {
			t.right();
			t.button(Icon.paste, MyStyles.clearPartiali, () -> Vars.ui
			 .showConfirm("@paste", "@confirm.paste", () -> {
				 file.writeString(Core.app.getClipboardText());
				 label.setText(getText());
			 })
			).padRight(2);
			t.button(Icon.copy, MyStyles.clearPartiali, () -> {
				Core.app.setClipboardText(this.file.readString());
			});
		}).growX().right().row();
		p.pane(p -> p.left().add(label).grow()).minSize(bw, bh).grow();
		cont.add(p).grow().row();

		// buttons.button("@back", Icon.left, Styles.defaultt, () -> {
		// 	hide();
		// }).size(bw / 2, 55);

		Editor[] editor = {null};
		hidden(() -> {
			if (editor[0] != null) editor[0].find(t -> {
				if (t instanceof FilterTable) {
					Core.app.post(t::clear);
				}
				return true;
			});
		});
		var listener = new VisibilityListener() {
			public boolean hidden() {
				file = editor[0].file;
				title.setText(file.nameWithoutExtension());
				editor[0].file = null;
				label.setText(getText());
				editor[0].removeListener(this);
				return false;
			}
		};
		buttons.button("@edit", Icon.edit, Styles.defaultt, () -> {
			 checkset(editor);
			 editor[0].edit(file, mod);
			 editor[0].addListener(listener);
		 }).size(bw / 2f, 55)
		 .disabled(b -> editor[0] != null && editor[0].isShown());
		// buttons.row();
		buttons.button("文本编辑", Icon.edit, Styles.defaultt, () -> {
			 checkset(editor);
			 editor[0].edit(file, mod, true);
			 editor[0].addListener(listener);
		 }).size(bw / 2f, 55)
		 .disabled(b -> editor[0] != null && editor[0].isShown());

		// closeOnBack();
	}
	private static void checkset(Editor[] editor) {
		if (editor[0] == null) {
			editor[0] = new Editor();
			editor[0].load();
		}
	}
	public String getText() {
		return file.readString().replaceAll("\\r", "\n").replaceAll("\\t", "  ")
		 .replaceAll("\\[(#?\\w*)]", "[\u0001$1]");
	}

	public JsonDialog show(Fi file, MyMod mod) {
		if (!file.extEquals("hjson") && !file.extEquals("json")) return null;
		this.file = file;
		this.mod = mod;

		title.setText(file.name() != null ? file.name() : "");

		label.setText(getText());

		show();
		return this;
	}
}
