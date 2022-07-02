package modmake;

import arc.files.Fi;
import arc.graphics.Pixmap;
import arc.graphics.Texture;
import arc.graphics.g2d.TextureRegion;
import arc.util.Log;
import mindustry.Vars;
import mindustry.mod.Mods.LoadedMod;
import modmake.components.MyMod;

import java.util.concurrent.CompletableFuture;

import static arc.Core.atlas;
import static mindustry.Vars.ui;
import static modmake.IntUI.modDialog;

public class IntVars {
	public static String modName = "mod-make";
	public static LoadedMod mod = Vars.mods.getMod(modName);
	public static Fi data = mod.root.child("data");

	public static void load() {}

	public static void showException(Exception e, boolean b) {
		if (b) {
			ui.showException(e);
		} else {
			Log.err(e);
		}
	}

	public static void async(String text, Runnable runnable, Runnable callback) {
		async(text, runnable, callback, ui != null);
	}

	public static void async(String text, Runnable runnable, Runnable callback, boolean displayUI) {
		if (displayUI) ui.loadfrag.show(text);
		CompletableFuture<?> completableFuture = CompletableFuture.supplyAsync(() -> {
			try {
				runnable.run();
			} catch (Exception err) {
				showException(err, displayUI);
			}
			if (displayUI) ui.loadfrag.hide();
			callback.run();
			return 1;
		});
		try {
			completableFuture.get();
		} catch (Exception e) {
			showException(e, displayUI);
		}
	}
	/*public static void async(String text, Runnable runnable, Runnable callback) {
		if (ui != null) ui.loadfrag.show(text);
		var thread = new Thread(() -> {
			try {
				runnable.run();
			} finally {
				if (ui != null) ui.loadfrag.hide();
				callback.run();
			}
		});
		thread.start();
		thread.setName(text);
	}*/


	public static TextureRegion find(String name) {
		return find(modDialog.currentMod, name);
	}

	public static TextureRegion wrap(Pixmap pixmap) {
		return new TextureRegion(new Texture(pixmap));
	}

	// 查找图片
	public static TextureRegion find(MyMod mod, String name) {
		if (mod == null) return atlas.find("error");
		Pixmap pix = mod.sprites1.containsKey(name) ? mod.sprites1.get(name) : mod.sprites2.get(name);
		if (pix == null) return atlas.find("error");
		return wrap(pix);
	}

}
