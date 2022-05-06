
const IntFunc = require('func/index')
const findClass = require('func/findClass')
const IniHandle = findClass("components.dataHandle");
const IntModsDialog = require('ui/dialogs/ModsDialog');
const modsDirectory = Vars.dataDirectory.child('mods(I hope...)').child('mods');

const write = mod => {
	if (!isNull) {
		file.parent().moveTo(mod)
	}
	let str = []
	str.push('minGameVersion: ' + Fields.minGameVersion.getText())

	for (let item of arr) {
		let text = Fields[item].getText()
		str.push(item + ': ' + (text.replace(/\s+/, '') === '' ? '""' : text))
	}
	mod.child(isNull ? 'mod.json' : 'mod.' + file.extension()).writeString(str.join('\n'));
	IntModsDialog.setup()

	ui.hide();
}
const arr = ['name', 'displayName', 'description', 'author', 'version', 'main', 'repo'],
	Fields = {}

function MyTextField(name) {
	let field = new TextField(name)
	if (Vars.mobile) field.removeInputDialog()
	return field
}

let ui, cont, buttons
let isNull, obj, file
ModMetaEditor.load = function () {
	ui = new BaseDialog("");
	cont = ui.cont
	cont.pane(cons(p => cont = p)).growY()
	buttons = ui.buttons
	let FieldArray = []

	let errorText = '';
	buttons.table(cons(err => {
		err.label(() => '[red]' + errorText).get()
	})).row()
	buttons.table(cons(b => {
		b.button('$back', Icon.left, run(() => ui.hide()))
			.size(210, 64);
		b.button('$ok', Icon.ok, run(() => {
			let mod = modsDirectory.child(Fields['fileName'].getText());
			if (mod.path() != file.parent().path() && mod.exists()) {
				Vars.ui.showConfirm('覆盖', '同名文件已存在\n是否要覆盖', run(() => {
					mod.deleteDirectory();
					write(mod);
				}));
			}
			else write(mod);
		})).size(210, 64).disabled(boolf(() => {
			for (let f of FieldArray) {
				if (!f.isValid()) return true
			}
			errorText = ''
			return false
		}))
	}));

	cont.add('$mod.fileName');
	cont.add(Fields.fileName = MyTextField('')).valid(extend(TextField.TextFieldValidator, {
		valid(text) {
			let valid = true
			if (text.replace(/\s/g, '') === '') {
				errorText = '文件名不能为空'
			} else if (text == 'tmp') {
				errorText = "文件名不能为'tmp'"
			} else valid = false
			return !valid
		}
	})).row()
	FieldArray.push(Fields.fileName)

	cont.add('$minGameVersion');
	cont.add(Fields.minGameVersion = MyTextField()).valid(extend(TextField.TextFieldValidator, {
		valid(text) {
			let num = +text
			let valid = true
			if (isNaN(num)) {
				errorText = "'最小游戏版本'必须为数字";
			} else if (num < 105) {
				errorText = "'最小游戏版本'不能小于105"
			} else if (num > Version.build) {
				errorText = "'最小游戏版本'不能大于 " + Version.build;
			} else {
				valid = false
			}

			return !valid
		}
	})).row()
	FieldArray.push(Fields.minGameVersion)


	for (let i of arr) {
		cont.add(Core.bundle.get(i, i));
		let field = MyTextField('');
		IntFunc.longPress(field, 600, longPress => {
			if (longPress)
				IntFunc.showTextArea(field);
		});
		Fields[i] = field
		cont.add(field).row()
	}

	ui.hidden(() => {
		IntModsDialog.show()
		modsDirectory.child("tmp").deleteDirectory()
	})
	ui.closeOnBack();
}

function ModMetaEditor(f) {
	modsDirectory.child("tmp").deleteDirectory()
	file = f;
	isNull = !f.exists()
	if (isNull) file.writeString('')
	obj = IniHandle.hjsonParse(file.readString());
	ui.title.setText(isNull ? '$mod.create' : '$edit');

	Fields.fileName.setText(isNull ? '' : f.parent().name())
	Fields.minGameVersion.setText('' + obj.getString('minGameVersion', '105'));

	for (let item of arr) {
		Fields[item].setText(obj.has(item) ? obj.getString(item, '').replace(/\n|\r/g, '\\n') : '')
	}

	ui.show();
}
module.exports = ModMetaEditor