
const IntFunc = require('func/index')
const ModEditor = require('ui/dialogs/ModEditor')
const ModMetaEditor = require('ui/dialogs/ModMetaEditor')
const IntSettings = require('content/settings');

const lastAtlats = [];

function newMod(file) {
	let meta = IntFunc.hjsonParse(
		file.child('mod.json').exists() ? file.child('mod.json').readString() :
			file.child('mod.hjson').exists() ? file.child('mod.hjson').readString() : null);
	if (meta == null) return null;
	return {
		'file': file,
		'meta': meta,
		spritesFi() {
			return this.file.child('sprites').exists() && this.file.isDirectory() ? this.file.child('sprites') : null;
		},
		displayName() {
			let meta = this.meta
			return meta.getString('displayName', meta.getString("name"));
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

	lastAtlats[0] = Core.atlas;

	exports.ui = dialog = new BaseDialog(name);
	dialog.addCloseListener()

	style = Styles.defaultt;
	margin = 12;
	buttons = dialog.buttons;
	pane = new Table
	pane.margin(10).top()

	dialog.cont.add('$mod.advise').top().row();
	dialog.cont.table(Styles.none, cons(t => t.pane(pane).scrollX(false).fillX().fillY())).row();

	buttons.button('$back', Icon.left, style, () => dialog.hide()).margin(margin).size(210, 60);
	buttons.button('$mod.add', Icon.add, style, () => {
		let dialog = new BaseDialog('$mod.add'),
			bstyle = Styles.cleart;

		dialog.cont.table(Tex.button, cons(t => {
			t.defaults().left().size(300, 70);
			t.margin(12);

			t.button('$mod.import.file', Icon.file, bstyle, () => {
				dialog.hide();

				Vars.platform.showMultiFileChooser(file => {
					this.import(file);
				}, 'zip', 'jar');
			}).margin(12).row();
			t.button('$mod.add', Icon.add, bstyle, () => {
				ModMetaEditor.constructor(modsDirectory.child('tmp').child('mod.hjson'));
			}).margin(12);
		}));
		dialog.addCloseButton();
		dialog.show();
	}).margin(margin).size(210, 64).row();

	if (!Vars.mobile) buttons.button('$mods.openfolder', Icon.link, style, () => {
		Core.app.openFolder(dataDirectory.absolutePath())
	}).margin(margin).size(210, 64);
	buttons.button('$quit', Icon.exit, style, () => Core.app.exit()).margin(margin).size(210, 64);
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
exports.show = function () {
	Vars.ui.loadfrag.show()
	this.setup();

	Vars.ui.loadfrag.hide()

	dialog.show()
}

const loadMod = (() => {
	let clazz = Vars.mods.getClass()
	let method = clazz.getDeclaredMethod("loadMod", Fi, java.lang.Boolean.TYPE)
	method.setAccessible(true)

	let field = clazz.getDeclaredField("mods")
	field.setAccessible(true)
	let mods = field.get(Vars.mods);

	return function (mod) {
		try {
			let fi = mod.file
			Vars.content.clear()
			Vars.content.createBaseContent()
			let _mod = method.invoke(Vars.mods, fi, true)
			mods.add(_mod)
			_mod.state = Packages.mindustry.mod.Mods.ModState.enabled;
			if (IntSettings.getValue("loadMod", "load_sprites")) {
				let shadow = lastAtlats[0], spritesFi = mod.spritesFi();

				let AtlasRegion = TextureAtlas.AtlasRegion
				let map = new Map()
				spritesFi.walk(cons(f => {
					let region = new AtlasRegion(new TextureRegion(new Texture(f)))
					region.name = _mod.meta.name + "-" + f.nameWithoutExtension()
					map.set(region.name, region);
				}))

				let atlas = Core.atlas = extend(TextureAtlas, {
					find(name) {
						// var base = IntFunc.findSprites(spritesFi, name)
						var base = map.get(name)
						// var base = this.error
						/* if (base != "error") {
							var reg = new AtlasRegion(base);
							reg.name = name;
							reg.pixmapRegion = base;
							return reg;
						} */

						if (base == null) base = shadow.find(name);

						if (base == this.error) {
							if (typeof arguments[1] == "string") return this.find(arguments[1])
							if (arguments[1] instanceof TextureRegion) return arguments[1]
						}
						return base;
					},

					isFound(region) {
						return region != "error"
					},

					has(s) {
						return shadow.has(s) || this.isFound(IntFunc.findSprites(s))
					},

					//return the *actual* pixmap regions, not the disposed ones.
					getPixmap(region) {
						let out = find(region.name)
						//this should not happen in normal situations
						if (out == null) return this.error;
						return out;
					},

					__load() {
						this.error = shadow.find("error")
					}
				})
				atlas.__load()
			}
			let wrong = _mod.hasContentErrors()
			Vars.content.createModContent()
			mods.remove(_mod)
			Vars.content.init()
			Vars.content.load()
			Vars.content.loadColors()
			return !wrong
		} catch (e) {
			Vars.ui.showErrorMessage(e)
			return false;
		}
	}
})()

exports.setup = function () {
	let p = pane
	p.clearChildren()
	this.mods = modsDirectory.list();
	if (this.mods.length == 0) {
		p.table(Styles.black6, cons(t => t.add('$mods.none'))).height(80);
		dialog.show()
		return;
	}

	this.mods.forEach(file => {
		if (file.name() == 'tmp') return;
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
					try {
						image.setDrawable(
							new TextureRegion(new Texture(mod.file.child('icon.png')))
						);
					} catch (e) {
						image.setDrawable(Tex.nomap);
					}
				} else {
					image.setDrawable(Tex.nomap);
				}
				image.border(Pal.accent);
				title.add(image).size(h - 8).padTop(-8).padLeft(-8).padRight(8);

				title.table(cons(text => {
					text.add('[accent]' + /*Strings.stripColors*/mod.displayName() + '\n[lightgray]v' +
						mod.meta.getString('version', '???')).wrap().width(300).growX().left();

				})).top().growX();

				title.add().growX().left();
			}));
			b.table(cons(right => {
				right.right();
				right.button(Icon.edit, Styles.clearPartiali, () =>
					ModMetaEditor.constructor(mod.file.child('mod.json').exists() ? mod.file.child('mod.json') : mod.file.child('mod.hjson'))
				).size(50);
				right.button(Icon.trash, Styles.clearPartiali, () =>
					Vars.ui.showConfirm('$confirm', '$mod.remove.confirm', () => {
						file.deleteDirectory();
						this.constructor();
					})
				).size(50).row();
				right.button(Icon.upload, Styles.clearPartiali, () => {
					let file = Vars.modDirectory;
					let enable = IntSettings.getValue("base", "auto_load_mod")
					function upload() {
						if (enable) {
							if (!loadMod(mod)) {
								Vars.ui.showInfo("导出失败！");
								return
							}
						} else {
							file.child(mod.file.name()).deleteDirectory()
							mod.file.copyTo(file);
						}
						Vars.ui.showInfo("导出成功！");
					}

					if (file.child(mod.file.name()).exists() && !enable)
						Vars.ui.showConfirm('替换', '同名文件已存在\n是否要替换', upload);
					else upload();
				}).size(50).disabled(boolf(() => Vars.state.isGame() && IntSettings.getValue("base", "auto_load_mod")));
				right.button(Icon.link, Styles.clearPartiali, () => Core.app.openFolder(mod.file.absolutePath())).size(50);
			})).growX().right().padRight(-8).padTop(-8);
		}), new Button.ButtonStyle(Styles.clearPartialt), () => {
			ModEditor.constructor(mod);
		}).size(w, h).growX().pad(4).row();
	});
}