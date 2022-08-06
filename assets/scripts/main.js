
importPackage(Packages.arc.files);


let lastAtlas = null;
let scripts = Vars.mods.scripts;
let { scope } = scripts;
let loader = Vars.mods.mainLoader()
loader.loadClass("modmake.ui.dialog.MySettingsDialog")
let NativeJavaClass = Packages.rhino.NativeJavaClass
/*const ModMake = Packages.rhino.NativeJavaClass(scope, loadClass("modmake.ModMake"), true);
ModMake.runnable = () => {*/

const ContentSeq = NativeJavaClass(scope, loader.loadClass("modmake.util.load.ContentSeq"), true);

const loadMod = (() => {

	let mods = Vars.mods.list();
	let parser = ContentSeq.parser

	let AtlasRegion = TextureAtlas.AtlasRegion

	function loadSprites(map) {
		let shadow = lastAtlas;

		let atlas = Core.atlas = extend(TextureAtlas, {
			find(name) {
				var base = map.containsKey(name) ? map.get(name) : shadow.find(name);

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
	function* gen(mod) {
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
//			Log.info(map)
			if (!map.isEmpty()) {
				loadSprites(map)
			}
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
		Vars.ui.loadfrag.show("加载mod");
	    let el = new Element()
	    let v;
	    el.update(() => {
		    try {
    			v = g.next()
    		} catch (err) {
    		    Log.err(err)

    		    let text = err.message

                let ui = new Dialog("");
                let { cont } = ui
                let message = err.message;

                ui.setFillParent(true);
                cont.margin(15);
                cont.add("@error.title").colspan(2);
                cont.row();
                cont.image().width(300).pad(2).colspan(2).height(4).color(Color.scarlet);
                cont.row();
                cont.add((text.startsWith("@") ? Core.bundle.get(text.substring(1)) : text) + (message == null ? "" : "\n[lightgray](" + message + ")")).colspan(2).wrap().growX().center().get().setAlignment(Align.center);
                cont.row();

                let col = new Collapser(base => base.pane(t => t.margin(14).add(err.stack).color(Color.lightGray).left()), true);

                cont.button("@details", Styles.togglet, () => col.toggle()).size(180, 50).checked(b => !col.isCollapsed()).fillX().right();
                cont.button("@ok", () => ui.hide()).size(110, 50).fillX().left();
                cont.row();
                cont.add(col).colspan(2).pad(2);
                ui.closeOnBack();

                ui.show();
    			v = null;
    		}
	    	if (v == null || v.done) {
		    	el.remove()
    			el.update(null)
    		}
    	})
    	Core.scene.add(el);
		return true;
	}
})()
let AtlasRegion = TextureAtlas.AtlasRegion

const ACLASS = NativeJavaClass(scope, loader.loadClass("modmake.util.load.LoadMod"), true);
ACLASS.boolf = boolf(loadMod)
// ACLASS.boolf = boolf(() => false)
let { settings } = ACLASS

Events.run(ClientLoadEvent, () => {
	lastAtlas = Core.atlas;
})

let lastReq = require
const hjson = require("hjson/hjson")
Log.debug(hjson.stringify(hjson.parse("a: 2//2\nb: 2\nc: {b: 1, d:3}", {keepWsc:true}), {keepWsc:true}))