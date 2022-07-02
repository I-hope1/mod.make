package modmake.components;

import arc.Core;
import arc.files.Fi;
import arc.graphics.Pixmap;
import arc.graphics.Texture;
import arc.graphics.g2d.TextureAtlas.AtlasRegion;
import arc.graphics.g2d.TextureRegion;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.serialization.JsonValue;
import mindustry.mod.Mod;
import mindustry.mod.Mods;
import modmake.IntVars;

import java.util.Objects;

import static modmake.components.DataHandle.json;
import static modmake.components.DataHandle.settings;
import static modmake.ui.dialog.ModsDialog.dataDirectory;

public class MyMod {
	public Fi root;
	public JsonValue meta;

	public String name() {
		return meta.getString("name");
	}

	public static MyMod set(Fi file) {
		String json = file.child("mod.json").exists() ? file.child("mod.json").readString() :
				file.child("mod.hjson").exists() ? file.child("mod.hjson").readString() : null;
		if (json == null) return null;
		JsonValue meta = DataHandle.hjsonParse(json);
		if (meta == null) return null;
		return new MyMod(file, meta);
	}

	public MyMod(Fi root, JsonValue meta) {
		if (meta == null) throw new IllegalArgumentException("meta cannot be null");
		this.root = root;
		this.meta = meta;

		if ("启动时加载一次".equals(settings.get("auto_load_sprites"))) loadSprites();
	}

	public void loadSprites() {
		sprites1.clear();
		sprites2.clear();
		IntVars.async("loadSprite", () -> {
//			Log.info("step 1");
			Fi all = root.child("sprites");
			int max = (int) settings.getFloat("max_load_sprite_size");
//			Log.info("step 2");
			if (all.exists()) all.walk(f -> {
//				if (Objects.equals(f.name(), ".hidden")) Log.info(f.readString());
				if (!f.extEquals("png") || f.file().length() > max) return;
				keys.add(name() + "-" + f.nameWithoutExtension());
//				Log.info(f.nameWithoutExtension());
				try {
					sprites1.put(f.nameWithoutExtension(), new Pixmap(f));
				} catch (Exception ignored) {}
			});
//			Log.info("step 3");
			all = root.child("sprites-override");
			if (all.exists()) all.walk(f -> {
				if (!f.extEquals("png") || f.file().length() > max) return;
				keys.add(f.nameWithoutExtension());
//				Log.info(f.nameWithoutExtension());
				try {
					sprites2.put(f.nameWithoutExtension(), new Pixmap(f));
				} catch (Exception ignored) {}
			});
		}, () -> {
//			Log.info(sprites1);
		});
	}

	public ObjectMap<String, AtlasRegion> spriteAll() {
		ObjectMap<String, AtlasRegion> map = new ObjectMap<>();
		String prefix = name() + "-";
		sprites1.each((k, v) -> {
			var region = new AtlasRegion(new TextureRegion(new Texture(v)));
//			Log.info(k);
			region.name = prefix + k;
			map.put(region.name, region);
		});
		sprites2.each((k, v) -> {
			var region = new AtlasRegion(new TextureRegion(new Texture(v)));
			region.name = k;
			map.put(k, region);
		});
//		Log.info(map);
		return map;
	}

	/*public Pixmap loadSprite(Fi fi, boolean override) {
		Pixmap pixmap = new Pixmap(fi);
		(override ? sprites2 : sprites1).put(fi.nameWithoutExtension(), pixmap);
		return pixmap;
	}*/

	public Seq<String> keys = new Seq<>();
	public ObjectMap<String, Pixmap> sprites1 = new ObjectMap<>();
	public ObjectMap<String, Pixmap> sprites2 = new ObjectMap<>();

	public String displayName() {
		return meta.getString("displayName", meta.getString("name"));
	}

	public TextureRegion logo() {
		for (Fi f : root.child("sprites-override").findAll()) {
			if (Objects.equals(f.name(), "logo.png")) {
				return new TextureRegion(new Texture(f));
			}
		}
		return Core.atlas.find("error");
	}

	public Mods.LoadedMod toMod() {
		Fi to = dataDirectory.child("tmp").child(name());
		root.copyTo(to);
		return new Mods.LoadedMod(to, to, new Mod() {}, null, json.fromJson(Mods.ModMeta.class, meta + ""));
	}
}

