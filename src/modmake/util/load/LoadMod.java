package modmake.util.load;

import arc.Core;
import arc.files.Fi;
import arc.func.Boolf;
import arc.func.Cons2;
import arc.graphics.Pixmap;
import arc.graphics.Pixmaps;
import arc.graphics.Texture;
import arc.graphics.g2d.PixmapRegion;
import arc.graphics.g2d.TextureAtlas;
import arc.graphics.g2d.TextureAtlas.AtlasRegion;
import arc.graphics.g2d.TextureRegion;
import arc.struct.Seq;
import arc.struct.StringMap;
import arc.util.Log;
import arc.util.Time;
import mindustry.Vars;
import mindustry.ctype.Content;
import mindustry.ctype.ContentType;
import mindustry.ctype.UnlockableContent;
import mindustry.graphics.MultiPacker;
import mindustry.graphics.MultiPacker.PageType;
import mindustry.mod.Mods;
import mindustry.mod.Mods.LoadedMod;
import mindustry.type.ErrorContent;
import mindustry.world.Block;
import modmake.IntVars;
import modmake.components.DataHandle;
import modmake.components.MyMod;
import modmake.util.MyReflect;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Locale;

import static mindustry.Vars.content;
import static mindustry.Vars.ui;
import static modmake.components.DataHandle.dataDirectory;
import static modmake.util.load.ContentSeq.parser;

public class LoadMod {

	public static Method loadMod, checkWarnings;
	public static Seq<LoadedMod> mods;
	public static MyMod currentMod;
	public static LoadedMod lastMod;
	public static TextureAtlas lastAtlas;
	// 用main.js加载
//	public static Cons<MyMod> imgLoad;
	public static StringMap settings = DataHandle.settings;
	public static MultiPacker packer;
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


	public static void loadContent() {
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
				int mod = ordered.get(l.type) - ordered.get(type);
				if (mod != 0) return mod;
				return file.name().compareTo(l.file.name());
			}

			@Override
			public String toString() {
				return type + "-" + file.name();
			}
		}

		Seq<LoadRun> runs = new Seq<>();

		LoadedMod mod = lastMod;
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
//		Log.info(runs);
		for (LoadRun l : runs) {
			Content current = content.getLastAdded();
			try {
				//this binds the content but does not load it entirely
				Content loaded = parser.parse(mod, l.file.nameWithoutExtension(), l.file.readString("UTF-8"), l.file, l.type);
				Log.debug("[@] Loaded '@'.", mod.meta.name, loaded instanceof UnlockableContent ? ((UnlockableContent) loaded).localizedName : loaded);
			} catch (Throwable e) {
				if (current != content.getLastAdded() && content.getLastAdded() != null) {
					parser.markError(content.getLastAdded(), mod, l.file, e);
				} else {
					ErrorContent error = new ErrorContent();
					parser.markError(error, mod, l.file, e);
				}
			}
		}

		//this finishes parsing content fields
		parser.finishParsing();
	}

	static PageType getPage(AtlasRegion region) {
		return region.texture == Core.atlas.find("white").texture ? PageType.main
				: region.texture == Core.atlas.find("stone1").texture ? PageType.environment
				: region.texture == Core.atlas.find("clear-editor").texture ? PageType.editor
				: region.texture == Core.atlas.find("whiteui").texture ? PageType.ui
				: region.texture == Core.atlas.find("rubble-1-0").texture ? PageType.rubble
				: PageType.main;
	}

	static PageType getPage(String path) {
		return path.contains("sprites/blocks/environment") ? PageType.environment
				: path.contains("sprites/editor") ? PageType.editor
				: path.contains("sprites/rubble") ? PageType.editor
				: path.contains("sprites/ui") ? PageType.ui
				: PageType.main;
	}

	public static void packSprite() {
		boolean linear = Core.settings.getBool("linear", true);

		boolean[] prefix = {false};
		Cons2<String, Pixmap> cons = (key, pix) -> {
			//read and bleed pixmaps in parallel
//            async.submit(() -> {
			try {
				//only bleeds when linear filtering is on at startup
				if (linear) {
					Pixmaps.bleed(pix, 2);
				}
				//this returns a *runnable* which actually packs the resulting pixmap; this has to be done synchronously outside the method
//                    return (Runnable)() -> {
				packer.add(PageType.main, prefix[0] ? lastMod.name + "-" + key : key, new PixmapRegion(pix));
				pix.dispose();
//                    };
			} catch (Exception e) {
				//rethrow exception with details about the cause of failure
				Log.err(new Exception("Failed to load image " + key + " for mod " + currentMod.name(), e));
			}
//            });
		};
		currentMod.sprites1.each(cons);
		prefix[0] = true;
		currentMod.sprites2.each(cons);
	}

	public static void loadSprites() {
		packer = new MultiPacker();
		packSprite();
		/*ObjectMap<String, Pixmap> packer = new ObjectMap<>();
		packer.putAll(currentMod.sprites1);

		packer.putAll(currentMod.sprites2);*/
		for (AtlasRegion region : Core.atlas.getRegions()) {
			//TODO PageType completely breaks down with multiple pages.
			PageType type = getPage(region);
			if (!packer.has(type, region.name)) {
				packer.add(type, region.name, Core.atlas.getPixmap(region), region.splits, region.pads);
			}
		}

		Core.atlas.dispose();

		//dead shadow-atlas for getting regions, but not pixmaps
		var shadow = Core.atlas;
		//dummy texture atlas that returns the 'shadow' regions; used for mod loading
		Core.atlas = new TextureAtlas() {
			{
				//needed for the correct operation of the found() method in the TextureRegion
				error = shadow.find("error");
			}

			@Override
			public AtlasRegion find(String name) {
				var base = packer.get(name);

				if (base != null) {
					var reg = new AtlasRegion(shadow.find(name).texture, base.x, base.y, base.width, base.height);
					reg.name = name;
					reg.pixmapRegion = base;
					return reg;
				}

				return shadow.find(name);
			}

			@Override
			public boolean isFound(TextureRegion region) {
				return region != shadow.find("error");
			}

			@Override
			public TextureRegion find(String name, TextureRegion def) {
				return !has(name) ? def : find(name);
			}

			@Override
			public boolean has(String s) {
				return shadow.has(s) || packer.get(s) != null;
			}

			//return the *actual* pixmap regions, not the disposed ones.
			@Override
			public PixmapRegion getPixmap(AtlasRegion region) {
				PixmapRegion out = packer.get(region.name);
				//this should not happen in normal situations
				if (out == null) return packer.get("error");
				return out;
			}
		};

		Texture.TextureFilter filter = Core.settings.getBool("linear", true) ? Texture.TextureFilter.linear : Texture.TextureFilter.nearest;

		Time.mark();
		//generate new icons
		for (Seq<Content> arr : content.getContentMap()) {
			arr.each(c -> {
				if (c instanceof UnlockableContent && c.minfo.mod != null) {
					c.load();
					c.loadIcon();
					((UnlockableContent) c).createIcons(packer);
				}
			});
		}
		Log.debug("Time to generate icons: @", Time.elapsed());

		//dispose old atlas data
		Core.atlas = packer.flush(filter, new TextureAtlas());

		Core.atlas.setErrorRegion("error");
		Log.debug("Total pages: @", Core.atlas.getTextures().size);

		/*packer.dispose();
		packer = null;*/

		Log.debug("Total time to generate & flush textures synchronously: @", Time.elapsed());
	}

	private static void loadIcons(UnlockableContent c) throws Throwable {
		if (c instanceof Block) {
			Method method = Block.class.getDeclaredMethod("icons");
			method.setAccessible(true);
			TextureRegion[] regions = (TextureRegion[]) method.invoke(c);

			Pixmap main = null;
			for (var reg : regions) {
				var data = reg.texture.getTextureData();

				Pixmap pixmap = MyReflect.getValue(data, "pixmap");
				if (pixmap != null) {
					if (main == null) {
						main = pixmap;
						continue;
					}
					main.draw(pixmap);
				}
			}

			c.fullIcon = c.uiIcon = new TextureRegion(new Texture(main));
		}
	}

	public static void loadIcons() {
		if (currentMod.sprites1.isEmpty() && currentMod.sprites2.isEmpty()) return;
		//generate new icons
		for (Seq<Content> arr : content.getContentMap()) {
			arr.each(c -> {
				if (c instanceof UnlockableContent && c.minfo.mod != null) {
					try {
						loadIcons((UnlockableContent) c);
					} catch (Throwable e) {
						Log.err(e);
					}
				}
			});
		}
//		Log.info("loadIcons");
	}

	static boolean loaded() throws Exception {
		var mod = currentMod;

		Fi to = dataDirectory.child("tmp").child(mod.name());
		mod.root.copyTo(to);
		var _mod = (LoadedMod) loadMod.invoke(Vars.mods, to, true);
		mods.add(_mod);
		_mod.state = Mods.ModState.enabled;
		lastMod = _mod;
//		if (mod.sprites1.isEmpty() && mod.sprites2.isEmpty()) loadSprites();

		return boolf.get(currentMod);
		/*content.clear();
		content.createBaseContent();
		loadContent();
//		Log.info(content.items());
		async.submit(LoadMod::loadSprites);

		return !_mod.hasContentErrors();*/

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
		Fi tmpDir = dataDirectory.child("tmp");
		if (tmpDir.exists()) {
			if (tmpDir.isDirectory()) tmpDir.deleteDirectory();
			else tmpDir.delete();
		}
		if (settings.getBool("load_sprites")) mod.loadSprites();
		IntVars.async("< 加载mod >", () -> {
			try {
				if (loaded()) Vars.ui.showInfo("加载成功");
			} catch (Exception e) {
				ui.showException("加载失败", e);
			}
		}, () -> {
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
		// Vars.ui.loadfrag.table
		return true;
	}


	public static HashMap<ContentType, Integer> ordered = new HashMap<>();

	static {
		ordered.put(ContentType.item, 0);
		ordered.put(ContentType.liquid, 0);
		ordered.put(ContentType.bullet, 1);
		ordered.put(ContentType.unit, 2);
		ordered.put(ContentType.weather, 2);
		ordered.put(ContentType.status, 3);
		ordered.put(ContentType.block, 4);
		ordered.put(ContentType.sector, 4);
		ordered.put(ContentType.planet, 5);
	}
}
