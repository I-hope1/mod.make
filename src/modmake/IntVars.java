package modmake;

import arc.files.Fi;
import arc.graphics.Texture;
import arc.graphics.g2d.TextureRegion;
import mindustry.Vars;
import mindustry.mod.Mods.LoadedMod;
import modmake.components.MyMod;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import static arc.Core.atlas;
import static mindustry.Vars.ui;

public class IntVars {
	public static String modName = "mod-make";
	public static Fi data = Vars.mods.locateMod(modName).root.child("data");
	public static LoadedMod mod = Vars.mods.locateMod(modName);

	public static void async(String text, Runnable runnable, Runnable callback) {
		ui.loadfrag.show(text);
		CompletableFuture<?> completableFuture = CompletableFuture.supplyAsync(() -> {
			try {
				runnable.run();
			} catch (Exception err) {
				ui.showException(err);
			}
			ui.loadfrag.hide();
			callback.run();
			return 1;
		});
		try {
			completableFuture.get();
		} catch (Exception e) {
			ui.showException(e);
		}
	}


	// 查找图片
	public static TextureRegion find(MyMod mod, String name) {
		Fi fi = mod.spritesFi();
		return fi != null ? findSprites(fi, name) : atlas.find("error");
	}

	public static TextureRegion findSprites(Fi all, String name) {
		TextureRegion[] region = {atlas.find("error")};
		for (Fi f : all.findAll()) {

			if (Objects.equals(f.name(), name + ".png")) {
				region[0] = new TextureRegion(new Texture(f));
				break;
			}
		}
		return region[0];
	}
}
