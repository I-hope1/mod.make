package modmake.ui.dialog;

import arc.Core;
import arc.files.Fi;
import arc.graphics.Color;
import arc.scene.event.VisibilityListener;
import arc.scene.ui.Dialog;
import arc.scene.ui.Label;
import arc.scene.ui.layout.Table;
import mindustry.Vars;
import mindustry.gen.Icon;
import mindustry.ui.Styles;
import modmake.components.MyMod;
import modmake.ui.styles;

import static modmake.IntUI.editor;

public class JsonDialog extends Dialog {
	Label label;
	Table p;
	Fi file;
	MyMod mod;
	float w = Core.graphics.getWidth(),
			h = Core.graphics.getHeight(),
			bw = w > h ? 550 : 450,
			bh = w > h ? 200 : Vars.mobile ? 300 : 350;

	public void load() {
		label = new Label("");
		p = new Table();
		p.update(() -> {
			if (file == null || !file.exists()) hide();
		});
		p.center();
		p.defaults().padTop(10).left();
		p.add("$editor.sourceCode", Color.gray).padRight(10).padTop(0).row();
		p.table(t -> {
			t.right();
			t.button(Icon.paste, styles.clearPartiali, () -> Vars.ui
					.showConfirm("粘贴", "是否要粘贴", () -> {
						file.writeString(Core.app.getClipboardText());
						label.setText(getText());
					})
			).padRight(2);
			t.button(Icon.copy, styles.clearPartiali, () -> {
				Core.app.setClipboardText(this.file.readString());
			});
		}).growX().right().row();
		p.pane(p -> p.left().add(label)).width(bw).height(bh);
		cont.add(p).grow().row();

		buttons.button("$back", Icon.left, Styles.defaultt, () -> {
			hide();
		}).size(bw / 2, 55);

		var listener = new VisibilityListener() {
			@Override
			public boolean hidden() {
				file = editor.file;
				title.setText(file.nameWithoutExtension());
				label.setText(getText());
				editor.removeListener(this);
				return false;
			}
		};
		buttons.button("$edit", Icon.edit, Styles.defaultt, () -> {
			editor.edit(file, mod);
			editor.addListener(listener);
		}).size(bw / 2f, 55);
		closeOnBack();
	}

	public String getText() {
		return file.readString().replaceAll("\\r", "\n").replaceAll("\\t", "  ")
				.replaceAll("\\[(#?\\w+)]", "[\u0001$1]");
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
