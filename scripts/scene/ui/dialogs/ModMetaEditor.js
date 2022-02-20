
const IntFunc = require('func/index')
const IntModsDialog = require('scene/ui/dialogs/ModsDialog');
const modsDirectory = Vars.dataDirectory.child('mods(I hope...)').child('mods');

const write = mod => {
	if (!isNull) file.moveTo(mod);
	let str = []
	str.push('minGameVersion: ' + Fields.minGameVersion.getText())

	for (let item of arr) {
		let text = Fields[item].getText()
		str.push(item + ': ' + (text.replace(/\s+/, '') == '' ? '""' : text))
	}
	mod.child(isNull ? 'mod.json' : 'mod.' + file.extension()).writeString(str.join('\n'));
	IntModsDialog.constructor()

	ui.hide();
}
const arr = ['name', 'displayName', 'description', 'author', 'version', 'main', 'repo'],
	Fields = {}

let ui, cont, buttons
let isNull, obj, file
exports.load = function () {
	ui = new Dialog;
	cont = ui.cont
	buttons = ui.buttons
	let w = Core.graphics.getWidth(),
		h = Core.graphics.getHeight();
	let FieldArray = []

	buttons.button('$back', Icon.left, run(() => ui.hide()))
		.size(Math.max(w, h) * 0.1, Math.min(w, h) * 0.1);
	buttons.button('$ok', Icon.ok, run(() => {
		let mod = modsDirectory.child(Fields['fileName'].getText());
		if (mod.path() != file.parent().path() && mod.exists()) {
			Vars.ui.showConfirm('覆盖', '同名文件已存在\n是否要覆盖', run(() => {
				mod.deleteDirectory();
				write(mod);
			}));
		}
		else write(mod);
	})).size(Math.max(w, h) * 0.1, Math.min(w, h) * 0.1).disabled(boolf(() => {
		for (let f of FieldArray) {
			if (!f.isValid()) return true
		}
		return false
	}))

	cont.add('$mod.fileName');
	cont.add(Fields.fileName = new TextField).valid(extend(TextField.TextFieldValidator, {
		valid(text) {
			let valid
			valid = (text.replace(/\s/g, '') == '');
			return !valid
		}
	})).row()
	FieldArray.push(Fields.fileName)

	cont.add('$minGameVersion');
	cont.add(Fields.minGameVersion = new TextField).valid(extend(TextField.TextFieldValidator, {
		valid(text) {
			let num = +text
			let valid
			valid = isNaN(num) || (num < 105) || (num > Version.build)
			return !valid
		}
	})).row()
	FieldArray.push(Fields.minGameVersion)


	for (let i of arr) {
		cont.add(Core.bundle.get(i, i));
		let field = new TextField;
		IntFunc.longPress(field, 600, longPress => {
			if (longPress)
				IntFunc.showTextArea(field);
		});
		Fields[i] = field
		cont.add(field).row()
	}

	ui.hidden(() => modsDirectory.child("tmp").deleteDirectory())
	ui.closeOnBack();
}

exports.constructor = function (f) {
	file = f;
	isNull = !f.exists()
	if (isNull) file.writeString('')
	obj = IntFunc.HjsonParse(file.readString());
	ui.title.setText(isNull ? '$mod.create' : '$edit');

	Fields.fileName.setText(isNull ? '' : f.parent().name())
	Fields.minGameVersion.setText('' + obj.getString('minGameVersion', '105'));

	for (let item of arr) {
		Fields[item].setText(obj.has(item) ? obj.getString(item, '').replace(/\n|\r/g, '\\n') : '')
	}

	ui.show();
}