package modmake.util;

import arc.Core;
import arc.files.Fi;
import arc.func.Cons2;
import arc.graphics.Pixmap;
import arc.graphics.Pixmaps;
import arc.graphics.Texture;
import arc.graphics.g2d.PixmapRegion;
import arc.graphics.g2d.TextureAtlas;
import arc.graphics.g2d.TextureAtlas.AtlasRegion;
import arc.graphics.g2d.TextureRegion;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.Log;
import arc.util.Time;
import arc.util.async.AsyncExecutor;
import arc.util.async.AsyncResult;
import mindustry.Vars;
import mindustry.ctype.Content;
import mindustry.ctype.ContentType;
import mindustry.ctype.UnlockableContent;
import mindustry.graphics.MultiPacker;
import mindustry.graphics.MultiPacker.PageType;
import mindustry.mod.Mods;
import mindustry.mod.Mods.LoadedMod;
import mindustry.type.ErrorContent;
import modmake.components.MyMod;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Locale;

import static mindustry.Vars.content;
import static mindustry.Vars.ui;
import static modmake.components.dataHandle.settings;
import static modmake.util.ContentSeq.parser;

public class LoadMod {
	public static Method loadMod, checkWarnings;
	public static AsyncExecutor async = new AsyncExecutor();
	public static TextureAtlas lastAtlas = Core.atlas;
	public static Seq<LoadedMod> mods;
	public static MyMod currentMod;
	public static LoadedMod lastMod;
	public static MultiPacker packer;

	public static void init() throws Exception {
		var clazz = Mods.class;
		loadMod = clazz.getDeclaredMethod("loadMod", Fi.class, java.lang.Boolean.TYPE);
		loadMod.setAccessible(true);

		checkWarnings = clazz.getDeclaredMethod("checkWarnings");
		checkWarnings.setAccessible(true);

		mods = Vars.mods.list();
	}

	public static void loadSprites() {
		Time.mark();

		/*for (AtlasRegion region : Core.atlas.getRegions()) {
			//TODO PageType completely breaks down with multiple pages.
			PageType type = getPage(region);
			if (!packer.has(type, region.name)) {
				packer.add(type, region.name, Core.atlas.getPixmap(region), region.splits, region.pads);
			}
		}*/
		var shadow = lastAtlas;
		ObjectMap<String, Pixmap> map = new ObjectMap<>();
		ObjectMap<String, AtlasRegion> atlasMap = new ObjectMap<>();
		var mod = currentMod;
		Cons2<Fi, Boolean> cons = (f, b) -> {
			try {
				var pix = new Pixmap(f);
				var region = new AtlasRegion(new TextureRegion(new Texture(pix)));
				var name = f.nameWithoutExtension();
				name = region.name = b ? mod.name() + "-" + name : name;
				map.put(name, pix);
				atlasMap.put(name, region);
				name = null;
			} catch (Exception err) {
				Log.err(err);
			}
		};
		mod.root.child("sprites").findAll(f -> f.extension().equalsIgnoreCase("png"))
				.each(f -> cons.get(f, true));
		mod.root.child("sprites-override").findAll(f -> f.extension().equalsIgnoreCase("png"))
				.each(f -> cons.get(f, false));

		Core.atlas = new TextureAtlas() {
			{
				error = shadow.find("error");
			}

			public AtlasRegion find(String name) {
				/*var base = map.get(name);

				var t = shadow.find(name);
				if (base != null) {
					t.texture = new Texture(base);
				}
				return t;*/
				var base = atlasMap.containsKey(name) ? atlasMap.get(name) : shadow.find(name);
				return base;
			}
			/*public TextureRegion find(String name, TextureRegion def){
                var base = find(name);
                return base == null || base == error ? def : base;
            }*/

			@Override
			public boolean isFound(TextureRegion region) {
				return region != error;
			}

			@Override
			public TextureRegion find(String name, TextureRegion def) {
				return !has(name) ? def : find(name);
			}

			@Override
			public boolean has(String s) {
//				return shadow.has(s) || packer.get(s) != null;
//				return shadow.has(s) || map.containsKey(s);
				return shadow.has(s) || atlasMap.containsKey(s);
			}

			//return the *actual* pixmap regions, not the disposed ones.
			@Override
			public PixmapRegion getPixmap(AtlasRegion region) {
//				PixmapRegion out = packer.get(region.name);
				var out = find(region.name);
				//this should not happen in normal situations
				if (out == null) return error.pixmapRegion;
				return out.pixmapRegion;
			}
		};

//		TextureFilter filter = Core.settings.getBool("linear", true) ? TextureFilter.linear : TextureFilter.nearest;

		Time.mark();
		//generate new icons
		for (Seq<Content> arr : content.getContentMap()) {
			arr.each(c -> {
				if (c instanceof UnlockableContent && c.minfo.mod != null) {
					var u = (UnlockableContent) c;
					u.load();
					u.loadIcon();
//					u.createIcons(new MultiPacker());
				}
			});
		}
		Log.debug("Time to generate icons: @", Time.elapsed());

		//dispose old atlas data
//		Core.atlas = packer.flush(filter, new TextureAtlas());

		Core.atlas.setErrorRegion("error");
		Log.debug("Total pages: @", Core.atlas.getTextures().size);

//		packer.dispose();
//		packer = null;
		Log.debug("Total time to generate & flush textures synchronously: @", Time.elapsed());
	}

	public static void loadSync() {
		if (packer == null) return;
		Time.mark();

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
					var u = (UnlockableContent) c;
					u.load();
					u.loadIcon();
					u.createIcons(packer);
				}
			});
		}
		Log.debug("Time to generate icons: @", Time.elapsed());

		//dispose old atlas data
		Core.atlas = packer.flush(filter, new TextureAtlas());

		Core.atlas.setErrorRegion("error");
		Log.debug("Total pages: @", Core.atlas.getTextures().size);

		packer.dispose();
		packer = null;
		Log.debug("Total time to generate & flush textures synchronously: @", Time.elapsed());
	}

	public static void loadAsync() {
		var mod = currentMod;
		Time.mark();

		packer = new MultiPacker();
		//all packing tasks to await
		var tasks = new Seq<AsyncResult<Runnable>>();


		Seq<Fi> sprites = mod.root.child("sprites").findAll(f -> f.extension().equals("png"));
		Seq<Fi> overrides = mod.root.child("sprites-override").findAll(f -> f.extension().equals("png"));

		packSprites(sprites, mod, true, tasks);
		packSprites(overrides, mod, false, tasks);

		Log.debug("Packed @ images for mod '@'.", sprites.size + overrides.size, mod.name());

		for (var result : tasks) {
			try {
				var packRun = result.get();
				if (packRun != null) { //can be null for very strange reasons, ignore if that's the case
					try {
						//actually pack the image
						packRun.run();
					} catch (Exception e) { //the image can fail to fit in the spritesheet
						Log.err("Failed to fit image into the spritesheet, skipping.");
						Log.err(e);
					}
				}
			} catch (Exception e) { //this means loading the image failed, log it and move on
				Log.err(e);
			}
		}

		Log.debug("Time to pack textures: @", Time.elapsed());
	}

	private static PageType getPage(AtlasRegion region) {
		return region.texture == Core.atlas.find("white").texture ? PageType.main :
				region.texture == Core.atlas.find("stone1").texture ? PageType.environment :
						region.texture == Core.atlas.find("clear-editor").texture ? PageType.editor :
								region.texture == Core.atlas.find("whiteui").texture ? PageType.ui :
										region.texture == Core.atlas.find("rubble-1-0").texture ? PageType.rubble :
												PageType.main;
	}

	private static PageType getPage(Fi file) {
		String path = file.path();
		return path.contains("sprites/blocks/environment") ? PageType.environment :
				path.contains("sprites/editor") ? PageType.editor :
						path.contains("sprites/rubble") ? PageType.editor :
								path.contains("sprites/ui") ? PageType.ui :
										PageType.main;
	}

	private static void packSprites(Seq<Fi> sprites, MyMod mod, boolean prefix, Seq<AsyncResult<Runnable>> tasks) {
		boolean linear = Core.settings.getBool("linear", true);

		for (Fi file : sprites) {
			//read and bleed pixmaps in parallel
			tasks.add(async.submit(() -> {
				try {
					Pixmap pix = new Pixmap(file.readBytes());
					//only bleeds when linear filtering is on at startup
					if (linear) {
						Pixmaps.bleed(pix, 2);
					}
					//this returns a *runnable* which actually packs the resulting pixmap; this has to be done synchronously outside the method
					return () -> {
						packer.add(getPage(file), (prefix ? mod.name() + "-" : "") + file.nameWithoutExtension(), new PixmapRegion(pix));
						pix.dispose();
					};
				} catch (Exception e) {
					//rethrow exception with details about the cause of failure
					throw new Exception("Failed to load image " + file + " for mod " + mod.name(), e);
				}
			}));
		}
	}

	private static LoadedMod loadContent() {
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

		var mod = currentMod;
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

	private static boolean loaded() throws Exception {
		var mod = currentMod;
		var fi = mod.root;

//		var _mod = (LoadedMod) loadMod.invoke(Vars.mods, fi, true);
//		mods.add(_mod);
//		_mod.state = Mods.ModState.enabled;

		content.clear();
		content.createBaseContent();
		var _mod = loadContent();
//		content.createModContent();
		var wrong = _mod.hasContentErrors();
		lastMod = _mod;

		if (settings.getBool("load_sprites") && mod.spritesFi() != null) {
			loadAsync();
//			Thread.sleep(200);
			loadSync();
//			loadSprites();
		}

		// 加载content
		content.init();
		content.load();
		content.loadColors();
		return !wrong;
	}

	public static boolean load(MyMod mod) {
		currentMod = mod;
		async.submit(() -> {
			ui.loadfrag.show("< 加载mod >");
			try {
				loaded();
			} catch (Exception e) {
				ui.showException("加载失败", e);
				return;
			}
			ui.showInfo("加载成功");
			ui.loadfrag.hide();
		});
		// Vars.ui.loadfrag.table
		if (settings.getBool("display_exception")) {
			try {
				checkWarnings.invoke(Vars.mods);
			} catch (IllegalAccessException | InvocationTargetException e) {
				ui.showException(e);
			}
		}
		if (lastMod != null) {
			mods.remove(lastMod);
		}
		return true;
	}
}
