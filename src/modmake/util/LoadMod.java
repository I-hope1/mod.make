package modmake.util;

import arc.Core;
import arc.files.Fi;
import arc.func.Boolf;
import arc.graphics.g2d.TextureAtlas;
import arc.struct.Seq;
import arc.struct.StringMap;
import arc.util.Log;
import arc.util.async.AsyncExecutor;
import mindustry.Vars;
import mindustry.ctype.Content;
import mindustry.ctype.ContentType;
import mindustry.ctype.UnlockableContent;
import mindustry.mod.Mods;
import mindustry.mod.Mods.LoadedMod;
import mindustry.type.ErrorContent;
import modmake.components.DataHandle;
import modmake.components.MyMod;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Locale;

import static mindustry.Vars.content;
import static mindustry.Vars.ui;
import static modmake.util.ContentSeq.parser;

public class LoadMod {

	public static Method loadMod, checkWarnings;
	public static AsyncExecutor async = new AsyncExecutor();
	public static Seq<LoadedMod> mods;
	public static MyMod currentMod;
	public static LoadedMod lastMod;
	public static TextureAtlas lastAtlas;
	// 用main.js加载
//	public static Cons<MyMod> imgLoad;
	public static StringMap settings = DataHandle.settings;
	public static Boolf<MyMod> boolf;

	public static void init() throws Exception {
		loadMod = Mods.class.getDeclaredMethod("loadMod", Fi.class, java.lang.Boolean.TYPE);
		loadMod.setAccessible(true);

		checkWarnings = Mods.class.getDeclaredMethod("checkWarnings");
		checkWarnings.setAccessible(true);

		mods = Vars.mods.list();
		lastAtlas = Core.atlas;
		/*imgLoad = mod -> {
			Fi spritesFi = mod.spritesFi();
			Fi spritesFi2 = mod.root.child("sprites-override");

			ObjectMap<String, TextureAtlas.AtlasRegion> map = new ObjectMap<>();
			if (spritesFi != null) spritesFi.walk(f -> {
				try {
					var pix = new Pixmap(f);
					var region = new TextureAtlas.AtlasRegion(new TextureRegion(new Texture(pix)));
					region.name = lastMod.meta.name + "-" + f.nameWithoutExtension();
//			region.pixmapRegion = new PixmapRegion(pix)
					map.put(region.name, region);
				} catch (Exception err) {
					Log.err(err);
				}
			});
			if (spritesFi2.exists()) spritesFi2.walk(f -> {
				try {
					var pix = new Pixmap(f);
					var region = new TextureAtlas.AtlasRegion(new TextureRegion(new Texture(pix)));
					region.name = f.nameWithoutExtension();
//			region.pixmapRegion = new PixmapRegion(pix)
					map.put(region.name, region);
				} catch (Exception err) {
					Log.err(err);
				}
			});
			TextureAtlas shadow = lastAtlas;

			var atlas = new TextureAtlas() {
				@Override
				public AtlasRegion find(String name) {
					AtlasRegion r = shadow.find(name);
                    if(r == null || !r.equals(error)) {
						r = map.get(name);
						if (r == null) // throw new IllegalArgumentException("The region \"" + name + "\" does not exist!");
							return error;
                    }
                    return r;
				}


				public TextureRegion find(String name, TextureRegion def) {
					TextureRegion region = map.get(name);
					if (region != null && region != error) return region;
					region = shadow.find(name);
					if (region != null && region != error) return region;
					return def;
				}

				@Override
				public boolean has(String s) {
					return shadow.has(s) || isFound(map.getNull(s));
				}

				@Override
				public PixmapRegion getPixmap(AtlasRegion region) {
					var out = find(region.name);
					//this should not happen in normal situations
					if (out.equals(error)) return error.pixmapRegion;
					return out.pixmapRegion;
				}

				@Override
				public boolean isFound(TextureRegion region) {
					return region != null && region.equals(this.error);
				}

				{
					error = shadow.find("error");
				}
			};
			Core.atlas = atlas;
		};*/
	}


	public static LoadedMod loadContent() {
		content.setCurrentMod(null);

		class LoadRun implements Comparable<LoadRun> {
			final ContentType type;
			final Fi file;

			public LoadRun(ContentType type, Fi file) {
				this.type = type;
				this.file = file;
			}

			@Override
			public int compareTo(LoadRun l) {
				return this.file.name().compareTo(l.file.name());
			}
		}

		Seq<LoadRun> runs = new Seq<>();

		MyMod mod = currentMod;
		if (mod.root.child("content").exists()) {
			Fi contentRoot = mod.root.child("content");
			for (ContentType type : ContentType.all) {
				String lower = type.name().toLowerCase(Locale.ROOT);
				Fi folder = contentRoot.child(lower + (lower.endsWith("s") ? "" : "s"));
				if (folder.exists()) {
					for (Fi file : folder.findAll(f -> f.extension().equals("json") || f.extension().equals("hjson"))) {
						runs.add(new LoadRun(type, file));
					}
				}
			}
		}

		//make sure mod content is in proper order
		runs.sort();
		var loadedMod = mod.toMod();
		for (LoadRun l : runs) {
			Content current = content.getLastAdded();
			try {
				//this binds the content but does not load it entirely
				Content loaded = parser.parse(loadedMod, l.file.nameWithoutExtension(), l.file.readString("UTF-8"), l.file, l.type);
				Log.debug("[@] Loaded '@'.", mod.name(), (loaded instanceof UnlockableContent ? ((UnlockableContent) loaded).localizedName : loaded));
			} catch (Throwable e) {
				if (current != content.getLastAdded() && content.getLastAdded() != null) {
					parser.markError(content.getLastAdded(), loadedMod, l.file, e);
				} else {
					ErrorContent error = new ErrorContent();
					parser.markError(error, loadedMod, l.file, e);
				}
			}
		}

		//this finishes parsing content fields
		parser.finishParsing();
		return loadedMod;
	}

	static boolean loaded() throws InvocationTargetException, IllegalAccessException {
		var mod = currentMod;

		var _mod = (LoadedMod) loadMod.invoke(Vars.mods, mod.root, true);
		mods.add(_mod);
		_mod.state = Mods.ModState.enabled;
		lastMod = _mod;

		return boolf.get(currentMod);

		/*
		content.clear();
		content.createBaseContent();
		var _mod = loadContent();
//		content.createModContent();
		var wrong = _mod.hasContentErrors();
		lastMod = _mod;
		mods.add(_mod);

		if (settings.getBool("load_sprites") && (mod.spritesFi() != null || mod.root.child("sprites-override").exists())) {
			if (imgLoad != null) imgLoad.get(mod);
		}

		// 加载content
		content.init();
		content.load();
		content.loadColors();
		return !wrong;*/
	}

	public static boolean load(MyMod mod) {
		currentMod = mod;
		async.submit(() -> {
			ui.loadfrag.show("< 加载mod >");
			try {
				if (loaded()) Vars.ui.showInfo("加载成功");
			} catch (Exception e) {
				ui.showException("加载失败", e);
				return;
			}
			ui.loadfrag.hide();
			// Vars.ui.loadfrag.table
			if (settings.getBool("display_exception")) {
				try {
					checkWarnings.invoke(Vars.mods);
				} catch (Exception e) {
					ui.showException(e);
				}
			}

			if (lastMod != null) {
				mods.remove(lastMod);
			}
		});
		return true;
	}
}
