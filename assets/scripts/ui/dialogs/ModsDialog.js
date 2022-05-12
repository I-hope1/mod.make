
const IntFunc = require('func/index')
const ModEditor = require('ui/dialogs/ModEditor')
const ModMetaEditor = require('ui/dialogs/ModMetaEditor')

const findClass = require('func/findClass');
const IntStyles = findClass('ui.styles');
const IniHandle = findClass("components.dataHandle");
const { settings } = IniHandle;
const MyMod = findClass("components.MyMod");

const lastAtlats = [];

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
				ModMetaEditor(modsDirectory.child('tmp').child('mod.hjson'));
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

	/* let { ParseListener } = Packages.rhino.NativeJavaClass(Vars.mods.scripts.scope, Packages.mindustry.mod.ContentParser);
	let times = 0
	Vars.mods.addParseListener(new ParseListener({
		parsed: () => {
			// 每执行n次休眠1秒
			if (++times > settings.getBool("compiling_times_per_second")) Threads.sleep(1000);
		}
	})) */
}

exports.import = function (file) {
	let root, currentFile;
	try {
		let toFile = modsDirectory.child(file.nameWithoutExtension())
		if (!toFile.isDirectory()) toFile.delete()

		root = new ZipFi(file)
		let list = root.list()
		if (list.length == 1) {
			if (list[0].isDirectory()) {
				currentFile = list[0]
			} else {
				throw new Error('文件内容不合法')
			}
		} else {
			currentFile = root
		}
		list = null
		if (!currentFile.child('mod.json').exists() && !currentFile.child('mod.hjson').exists()) {
			throw Error('没有mod.(h)json')
		}
		currentFile.copyTo(toFile.parent())
	} catch (err) {
		IntFunc.showException(err)
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
	let loadMod = clazz.getDeclaredMethod("loadMod", Fi, java.lang.Boolean.TYPE)
	loadMod.setAccessible(true)

	let checkWarnings = clazz.getDeclaredMethod("checkWarnings")
	checkWarnings.setAccessible(true)

	let field = clazz.getDeclaredField("mods")
	field.setAccessible(true)
	let mods = field.get(Vars.mods);

	field = clazz.getDeclaredField("parser")
	field.setAccessible(true)
	let parser = field.get(Vars.mods)

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


	let lastMod;
	function* gen(mod) {
		let fi = mod.file

		let _mod = loadMod.invoke(Vars.mods, fi, true)
		mods.add(_mod)
		_mod.state = Packages.mindustry.mod.Mods.ModState.enabled;

		Vars.content.clear()
		Vars.content.createBaseContent()
		yield;

		Vars.content.createModContent()
		yield;
		yield;
		yield;
		let wrong = _mod.hasContentErrors()
		lastMod = _mod

		if (settings.getBool("load_sprites") && mod.spritesFi() != null) {
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
			if (map) loadSprites(map)
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
		Time.run(1, () => IntFunc.async("加载mod", g, v => {
			Vars.ui.showInfo("加载" + (v != null && v.value ? "成功" : "失败"))
			// Vars.ui.loadfrag.table
			if (settings.getBool("display_exception")) {
				checkWarnings.invoke(Vars.mods)
			}
			if (lastMod != null) {
				mods.remove(lastMod)
			}
		}))
		return true;
	}
})()

exports.setup = function () {
	let p = pane
	p.clearChildren()
	this.mods = modsDirectory.list();
	if (this.mods.length == 0) {
		p.table(Styles.black6, cons(t => t.add('$mods.none'))).height(80);
		return;
	}

	this.mods.forEach(file => {
		if (file.name() == 'tmp') return;
		let mod = MyMod.set(file);
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
					ModMetaEditor(mod.file.child('mod.json').exists() ? mod.file.child('mod.json') : mod.file.child('mod.hjson'))
				}).size(50);
				right.button(Icon.trash, Styles.clearPartiali, () =>
					Vars.ui.showConfirm('$confirm', '$mod.remove.confirm', () => {
						file.deleteDirectory();
						this.setup();
					})
				).size(50).row();
				right.button(Icon.upload, Styles.clearPartiali, () => {
					let file = Vars.modDirectory;
					let enable = settings.getBool("auto_load_mod")
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
				}).size(50).disabled(boolf(() => Vars.state.isGame() && settings.getBool("auto_load_mod")));
				right.button(Icon.link, Styles.clearPartiali, () => Core.app.openFolder(mod.file.absolutePath())).size(50);
			})).growX().right().padRight(-8).padTop(-8);
		}), IntStyles.clearpb, () => {
			dialog.hide()
			ModEditor(mod);
		}).size(w, h).growX().pad(4).row();
	});
}