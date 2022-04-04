
const IntFunc = require('func/index')
const ModEditor = require('ui/dialogs/ModEditor')
const ModMetaEditor = require('ui/dialogs/ModMetaEditor')
const IntSettings = require('content/settings');
const IntStyles = require('ui/styles');

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
			let region = Core.atlas.find("error")
			try {
				this.file.child('sprites-override').walk(cons(f => {
					if (f.name() == "logo.png") {
						region = new TextureRegion(new Texture(f))
						throw ''
					}
				}))
			} catch (e) { }
			return region
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
					this.setup()
				}, 'zip', 'jar');
			}).margin(12).row();
			t.button('$mod.add', Icon.add, bstyle, () => {
				ModMetaEditor.constructor(modsDirectory.child('tmp').child('mod.hjson'));
				this.setup()
			}).margin(12);
		}));
		dialog.addCloseButton();
		dialog.show();
	}).margin(margin).size(210, 64).row();

	if (!Vars.mobile) buttons.button('$mods.openfolder', Icon.link, style, () => {
		Core.app.openFolder(dataDirectory.absolutePath())
	}).margin(margin).size(210, 64);
	buttons.button('$quit', Icon.exit, style, () => Core.app.exit()).margin(margin).size(210, 64);

	this.setup()
}

exports.import = function (file) {
	let root, curretFile;
	try {
		let toFile = modsDirectory.child(file.nameWithoutExtension())
		if (!toFile.isDirectory()) toFile.delete()

		root = new ZipFi(file)
		let list = root.list()
		if (list.length == 1) {
			if (list[0].isDirectory()) {
				curretFile = list[0]
			} else {
				throw new Error('文件内容不合法')
			}
		} else {
			curretFile = root
		}
		list = null
		if (!curretFile.child('mod.json').exists() && !curretFile.child('mod.hjson').exists()) {
			throw Error('没有mod.(h)json')
		}
		curretFile.copyTo(toFile.parent())
	} catch (e) {
		let err = '[red][' + Core.bundle.get(e.name, e.name) + '][]' + e.message;
		Vars.ui.showErrorMessage(err);
	} finally {
		if (root != null) root.delete()
	}
}

const dataDirectory = Vars.dataDirectory.child('mods(I hope...)')
const modsDirectory = dataDirectory.child('mods')
let style, margin, buttons;
exports.show = function () {
	dialog.show()
}

const loadMod = (() => {
	let clazz = Vars.mods.getClass()
	let method = clazz.getDeclaredMethod("loadMod", Fi, java.lang.Boolean.TYPE)
	method.setAccessible(true)

	let field = clazz.getDeclaredField("mods")
	field.setAccessible(true)
	let mods = field.get(Vars.mods);

	let AtlasRegion = TextureAtlas.AtlasRegion

	function loadSprites(map) {
		let shadow = lastAtlats[0];

		let atlas = Core.atlas = extend(TextureAtlas, {
			find(name) {
				var base = map.has(name) ? map.get(name) : shadow.find(name);

				if (base == this.error) {
					if (typeof arguments[1] == "string") return this.find(arguments[1])
					if (arguments[1] instanceof TextureRegion) return arguments[1]
				}
				return base;
			},

			isFound(region) {
				return region != this.error;
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

	function* gen(mod) {
		let fi = mod.file

		let _mod = method.invoke(Vars.mods, fi, true)
		mods.add(_mod)
		_mod.state = Packages.mindustry.mod.Mods.ModState.enabled;

		Vars.content.clear()
		Vars.content.createBaseContent()
		yield;
		Vars.content.createModContent()
		yield;
		mods.remove(_mod)
		let wrong = _mod.hasContentErrors()

		if (IntSettings.getValue("loadMod", "load_sprites") && mod.spritesFi() != null) {
			let spritesFi = mod.spritesFi();

			let map = new Map()
			spritesFi.walk(cons(f => {
				try {
					let region = new AtlasRegion(new TextureRegion(new Texture(f)))
					region.name = _mod.meta.name + "-" + f.nameWithoutExtension()
					map.set(region.name, region);
				} catch (err) {
					Log.err(err)
				}
			}))
			yield;
			loadSprites(map)
		}
		yield;

		// 加载content
		Vars.content.init()
		yield;
		Vars.content.load()
		yield;
		Vars.content.loadColors()
		return !wrong;
	}
	return function (mod) {
		let g = gen(mod);
		IntFunc.async("加载mod", g, v => {
			Vars.ui.showInfo("加载" + (v != null && v.value ? "成功" : "失败"))
		})
		return true;
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
					text.add('[accent]' + mod.displayName() + '\n[lightgray]v' +
						mod.meta.getString('version', '???')).wrap().width(300).growX().left();

				})).top().growX();

				title.add().growX().left();
			}));
			b.table(cons(right => {
				right.right();
				right.button(Icon.edit, Styles.clearPartiali, () => {
					ModMetaEditor.constructor(mod.file.child('mod.json').exists() ? mod.file.child('mod.json') : mod.file.child('mod.hjson'))
				}).size(50);
				right.button(Icon.trash, Styles.clearPartiali, () =>
					Vars.ui.showConfirm('$confirm', '$mod.remove.confirm', () => {
						file.deleteDirectory();
						this.setup();
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
		}), IntStyles.clearpb, () => {
			ModEditor.constructor(mod);
		}).size(w, h).growX().pad(4).row();
	});
}