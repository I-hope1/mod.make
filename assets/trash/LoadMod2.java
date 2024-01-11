package modmake.util;

import arc.files.Fi;
import arc.func.Boolf;
import arc.struct.Seq;
import arc.util.Log;
import arc.util.async.AsyncExecutor;
import mindustry.Vars;
import mindustry.ctype.Content;
import mindustry.ctype.ContentType;
import mindustry.ctype.UnlockableContent;
import mindustry.mod.Mods;
import mindustry.mod.Mods.LoadedMod;
import mindustry.type.ErrorContent;
import modmake.components.MyMod;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Locale;

import static mindustry.Vars.content;
import static mindustry.Vars.ui;
import static modmake.components.DataHandle.settings;
import static modmake.util.ContentSeq.parser;

public class LoadMod {
	public static Method loadMod, checkWarnings;
	public static AsyncExecutor  async = new AsyncExecutor();
	public static Seq<LoadedMod> mods;
	public static MyMod          currentMod;
	public static LoadedMod      lastMod;
	// 用main.js加载
	public static Boolf<MyMod>   imgLoad;


	public static void init() throws Exception {
		loadMod = Mods.class.getDeclaredMethod("loadMod", Fi.class, java.lang.Boolean.TYPE);
		loadMod.setAccessible(true);

		checkWarnings = Mods.class.getDeclaredMethod("checkWarnings");
		checkWarnings.setAccessible(true);

		mods = Vars.mods.list();
	}

	static LoadedMod loadContent() {
		content.setCurrentMod(null);

		class LoadRun implements Comparable<LoadRun> {
			final ContentType type;
			final Fi          file;

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
				String lower  = type.name().toLowerCase(Locale.ROOT);
				Fi     folder = contentRoot.child(lower + (lower.endsWith("s") ? "" : "s"));
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

	static boolean loaded() {
		var mod = currentMod;

		//		var _mod = (LoadedMod) loadMod.invoke(Vars.mods, fi, true);
		//		mods.add(_mod);
		//		_mod.state = Mods.ModState.enabled;

		content.clear();
		content.createBaseContent();
		var _mod = loadContent();
		//		content.createModContent();
		var wrong = _mod.hasContentErrors();
		lastMod = _mod;
		mods.add(_mod);

		if (settings.getBool("load_sprites") && (mod.spritesFi() != null || mod.root.child("sprites-override").exists())) {
			if (imgLoad != null) imgLoad.get(mod);
			Log.info(imgLoad);
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
				if (loaded()) Vars.ui.showInfo("加载成功");
			} catch (Exception e) {
				ui.showException("加载失败", e);
				return;
			}
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
