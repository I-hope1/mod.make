
importPackage(Packages.arc.files);
let lastAtlas = null;
const loadMod = (() => {
	let clazz = Vars.mods.getClass()
	let loadModM = clazz.getDeclaredMethod("loadMod", Fi, java.lang.Boolean.TYPE)
	loadModM.setAccessible(true)

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
		let shadow = lastAtlas;

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
				return shadow.has(s) || this.isFound(map.get(s) || null)
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

		let _mod = loadModM.invoke(Vars.mods, fi, true)
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
    			Vars.ui.loadfrag.hide()
                Vars.ui.showInfo("加载" + (v != null && v.value ? "成功" : "失败"))
			    // Vars.ui.loadfrag.table
			    if (settings.getBool("display_exception")) {
			    	checkWarnings.invoke(Vars.mods)
			    }
			    if (lastMod != null) {
			    	mods.remove(lastMod)
			    }
    		}
    	})
    	Core.scene.add(el);
		return true;
	}
})()

let scripts = Vars.mods.scripts;
let { scope } = scripts;
const ACLASS = Packages.rhino.NativeJavaClass(scope, Vars.mods.mainLoader().loadClass("modmake.ui.dialog.ModsDialog")).ACLASS;
ACLASS.boolf = boolf(loadMod)
let settings = ACLASS.settings
Events.run(ClientLoadEvent, () => {
	lastAtlas = Core.atlas;
//	Log.info("lastAtlas: " + lastAtlas)
})