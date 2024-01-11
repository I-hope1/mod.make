
importPackage(Packages.arc.files);


let lastAtlas = null;
let scripts = Vars.mods.getScripts();
let { scope } = scripts;
let loader = Vars.mods.mainLoader()
loader.loadClass("modmake.ui.dialog.MySettingsDialog")
let NativeJavaClass = Packages.rhino.NativeJavaClass
/*const ModMake = Packages.rhino.NativeJavaClass(scope, loadClass("modmake.ModMake"), true);
ModMake.runnable = () => {*/

const ContentSeq = NativeJavaClass(scope, loader.loadClass("modmake.util.load.ContentVars"), true);

const loadMod = (() => {

	let mods = Vars.mods.list();
	let parser = ContentSeq.parser

	let AtlasRegion = TextureAtlas.AtlasRegion

	function loadSprites(map) {
		let shadow = lastAtlas;

		let atlas = Core.atlas = extend(TextureAtlas, {
			find(name, arg2) {
				let base = map.containsKey(name) ? map.get(name) : shadow.find(name);

				if (base == this.error) {
					if (typeof arg2 == "string") return this.find(arguments[1])
					if (arg2 instanceof TextureRegion) return arguments[1]
				}
				return base;
			},

			isFound(region) {
				return region != this.error;
			},

			has(s) {
				return shadow.has(s) || this.isFound(map.getNull(s) || null)
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
	function* gen(mod, callback) {
		let fi = mod.root

		let _mod = ACLASS.lastMod

		Vars.content = new ContentLoader()
		Vars.content.createBaseContent()
		yield;

		ACLASS.loadContent()
		yield;
		let wrong = _mod.hasContentErrors()

		let ls = settings.getBool("load_sprites")
		let li = settings.getBool("load_icons")
		if (li) {
			ACLASS.loadIcons()
		}
		if (ls) {
			let map = mod.spriteAll()
			if (!map.isEmpty()) {
				loadSprites(map)
			}
		}

		// 加载content
		Vars.content.init()
		yield;
		Vars.content.load()
		yield;
		Vars.content.loadColors()
		callback.get(!wrong);
	}
	return function (mod, callback) {
		Vars.ui.loadfrag.show("加载mod");
		let f = gen(mod, callback);
		forceRun(() => {
			return f.next().done;
		})
		Vars.ui.loadfrag.hide();
		return true;
	}
})()
let AtlasRegion = TextureAtlas.AtlasRegion

const ACLASS = new NativeJavaClass(scope, loader.loadClass("modmake.util.load.LoadMod"), true);
ACLASS.boolf = new Boolf2({get:loadMod})
// ACLASS.boolf = boolf(() => false)
let { settings } = ACLASS

Events.run(ClientLoadEvent, () => {
	lastAtlas = Core.atlas;
})

function forceRun(func) {
	Timer.schedule(extend(Timer.Task, {
		run() {
			try {
				if (func()) this.cancel();
			} catch (e) {
				Log.err(e);
				this.cancel();
			}
		}
	}), 0, 1 / 60, -1);
}

/*
let lastReq = require
const hjson = require("hjson/hjson")
Log.debug(hjson.stringify(hjson.parse("a: 2//2\nb: 2\nc: {b: 1, d:3}", {keepWsc:true}), {keepWsc:true}))*/
