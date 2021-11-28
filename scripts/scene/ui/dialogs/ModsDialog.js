
const IntStyles = require('scene/styles')
const IntFunc = require('func/index')
const ModEditor = require('scene/ui/dialogs/ModEditor')
const ModMetaEditor = require('scene/ui/dialogs/ModMetaEditor')

function newMod(file) {
	let meta = IntFunc.HjsonParse(
		file.child('mod.json').exists() ? file.child('mod.json').readString() :
			file.child('mod.hjson').exists() ? file.child('mod.hjson').readString() : null);
	if (meta == null) return null;
	return {
		'file': file,
		'meta': meta,
		spritesAll() {
			return this.file.child('sprites').exists() ? this.file.child('sprites').findAll().toArray() : [];
		},
		displayName() {
			return this.meta.getDefault('displayName', this.meta.getDefault('name', ''));
		},
		logo() {
			try {
				return TextureRegion(Texture(this.file.child('sprites-override').child('logo.png')));
			} catch (err) {
				return Core.atlas.find('error');
			}
		}
	};
}

let dialog, pane;
exports.mods = []
let h = 110, w = Vars.mobile ? (Core.graphics.getWidth() > Core.graphics.getHeight() ? 50 : 0) + 440 : 524;
exports.load = function (name) {
	ModEditor.load()
	ModMetaEditor.load()
	exports.ui = dialog = new BaseDialog(name);

	style = Styles.defaultt;
	margin = 12;
	buttons = dialog.buttons;
	pane = new Table

	dialog.cont.add('$mod.advise').top().row();
	dialog.cont.table(Styles.none, cons(t => t.pane(pane).fillX().fillY())).row();

	buttons.button('$back', Icon.left, style, run(() => dialog.hide())).margin(margin).size(210, 60);
	buttons.button('$mod.add', Icon.add, style, run(() => {
		let dialog = new BaseDialog('$mod.add'),
			bstyle = Styles.cleart;

		dialog.cont.table(Tex.button, cons(t => {
			t.defaults().left().size(300, 70);
			t.margin(12);

			t.button('$mod.import.file', Icon.file, bstyle, run(() => {
				dialog.hide();

				Vars.platform.showMultiFileChooser(file => {
					this.import(file);
				}, 'zip', 'jar');
			})).margin(12).row();
			t.button('$mod.add', Icon.add, bstyle, run(() => {
				ModMetaEditor.constructor(dataDirectory.child('tmp').child('mod.hjson'));
			})).margin(12)
		}));
		dialog.addCloseButton();
		dialog.show();
	})).margin(margin).size(210, 64).row();

	if (!Vars.mobile) buttons.button('$mods.openfolder', Icon.link, style, run(() => {
		Core.app.openFolder(dataDirectory.absolutePath())
	})).margin(margin).size(210, 64);
	buttons.button('$quit', Icon.exit, style, run(() => Core.app.exit())).margin(margin).size(210, 64);

	dialog.addCloseListener();
}

exports.import = function (file) {
	try {
		let toFile = modsDirectory.child(file.nameWithoutExtension())
		let zipFile = new ZipFi(file), dirs = [];
		if (!zipFile.child('mod.json').exists() && !zipFile.child('mod.hjson').exists()) throw Error('请导入合法的mod')
		while (true) {
			zipFile.list().forEach(f => {
				if (f.isDirectory()) dirs.push(f)
				else toFile.child(f.name()).writeString(f.readString())
			})
			if (dirs.length == 0) break
			zipFile = dirs.shift()
			toFile = toFile.child(zipFile.name())
		}

		this.constructor();
	} catch (e) {
		let err = '[red][' + Core.bundle.get(e.name, e.name) + '][]' + e.message;
		Vars.ui.showErrorMessage(err);
	}
}

const dataDirectory = Vars.dataDirectory.child('mods(I hope...)')
const modsDirectory = dataDirectory.child('mods')
let style, margin, buttons;
exports.constructor = function () {
	Vars.ui.loadfrag.show();

	let p = pane
	p.clearChildren()
	this.mods = modsDirectory.list();
	if (this.mods.length == 0) {
		p.table(Styles.black6, cons(t => t.add('$mods.none'))).height(80);
		dialog.show()
		Vars.ui.loadfrag.hide();
		return;
	}

	this.mods.forEach(file => {
		let mod = newMod(file);
		if (mod == null) return;

		p.button(cons(b => {
			b.top().left();
			b.margin(12);
			b.defaults().left().top();

			b.table(cons(title => {
				title.left();

				let image = extend(BorderImage, {});
				if (mod.file.child('icon.png').exists()) {
					image.setDrawable(
						TextureRegion(Texture(mod.file.child('icon.png')))
					);
				} else {
					image.setDrawable(Tex.nomap);
				}
				image.border(Pal.accent);
				title.add(image).size(h - 8).padTop(-8).padLeft(-8).padRight(8);

				title.table(cons(text => {
					text.add('[accent]' + /*Strings.stripColors*/mod.displayName() + '\n[lightgray]v' +
						mod.meta.get('version')).wrap().width(300).growX().left();

				})).top().growX();

				title.add().growX().left();
			}));
			b.table(cons(right => {
				right.right();
				right.button(Icon.edit, Styles.clearPartiali, run(() =>
					ModMetaEditor.constructor(mod.file.child('mod.json').exists() ? mod.file.child('mod.json') : mod.file.child('mod.hjson'))
				)).size(50);
				right.button(Icon.trash, Styles.clearPartiali, run(() =>
					Vars.ui.showConfirm('$confirm', '$mod.remove.confirm', run(() => {
						file.deleteDirectory();
						this.constructor();
					}))
				)).size(50).row();
				right.button(Icon.upload, Styles.clearPartiali, run(() => {
					let file = Vars.modDirectory;
					function upload() {
						file.child(mod.file.name()).deleteDirectory()
						mod.file.copyTo(file);
					}
					if (file.child(mod.file.name()).exists())
						Vars.ui.showConfirm('替换', '同名文件已存在\n是否要替换', run(upload));
					else upload();
				})).size(50);
				right.button(Icon.link, Styles.clearPartiali, run(() => Core.app.openFolder(mod.file.absolutePath()))).size(50);
			})).growX().right().padRight(-8).padTop(-8);
		}), IntStyles.clearb, run(() => {
			ModEditor.constructor(mod);
		})).size(w, h).growX().pad(4).row();
	});

	dialog.show()
	Vars.ui.loadfrag.hide();
}