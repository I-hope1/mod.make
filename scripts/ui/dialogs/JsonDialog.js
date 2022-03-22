const Editor = require('ui/dialogs/Editor')

let dialog, label, p
exports.load = function () {
	dialog = new Dialog
	label = new Label('')
	p = new Table
	p.center();
	p.defaults().padTop(10).left();
	p.add('$editor.sourceCode', Color.gray).padRight(10).padTop(0).row();
	p.table(cons(t => {
		t.right();
		t.button(Icon.download, Styles.clearPartiali, run(() => Vars.ui
			.showConfirm(
				'粘贴', '是否要粘贴', run(() => {
					this.file.writeString(Core.app.getClipboardText());
					label.setText(this.getText());
				}))
		));
		t.button(Icon.copy, Styles.clearPartiali, run(() => {
			Core.app.setClipboardText(this.file.readString());
		}));
	})).growX().right().row();
	p.pane(cons(p => p.left().add(label))).width(bw).height(bh);
	dialog.cont.add(p).grow().row();

	dialog.buttons.button('$back', Icon.left, Styles.defaultt, run(() => {
		dialog.hide();
	})).size(bw / 2, 55);

	let listener = extend(VisibilityListener, {
		hidden: () => {
			this.file = Editor.file
			dialog.title.setText(this.file.nameWithoutExtension())
			label.setText(this.getText())
			Editor.ui.removeListener(listener)
			return false;
		}
	})
	dialog.buttons.button('$edit', Icon.edit, Styles.defaultt, run(() => {
		Editor.edit(this.file, this.mod)
		Editor.ui.addListener(listener)
	})).size(bw / 2, 55);
	dialog.closeOnBack();
}
exports.getText = function () {
	return this.file.readString().replace(/\r/g, '\n').replace(/\t/g, '  ').replace(/\[\s*([^]*?)\s*\]/g, '[ $1 ]')
}

let w = Core.graphics.getWidth(),
	h = Core.graphics.getHeight(),
	bw = w > h ? 550 : 450,
	bh = w > h ? 200 : Vars.mobile ? 300 : 350;
exports.constructor = function (file, mod) {
	if (!/^h?json$/.test(file.extension())) return null;
	this.file = file, this.mod = mod

	dialog.title.setText(file.name() != null ? file.name() : '');

	label.setText(this.getText());

	dialog.show();
	return dialog;
}