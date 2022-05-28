package modmake.components;

import arc.Core;
import arc.files.Fi;
import arc.graphics.Texture;
import arc.graphics.g2d.TextureRegion;
import arc.util.serialization.JsonValue;
import mindustry.mod.Mod;
import mindustry.mod.Mods;

import java.util.Objects;

import static modmake.components.dataHandle.json;

public class MyMod {
	public Fi root;
	public JsonValue meta;
	public String name() {
		return meta.getString("name");
	};

	public static MyMod set(Fi file) {
		String json = file.child("mod.json").exists() ? file.child("mod.json").readString() :
				file.child("mod.hjson").exists() ? file.child("mod.hjson").readString() : null;
		if (json == null) return null;
		JsonValue meta = dataHandle.hjsonParse(json);
		if (meta == null) return null;
		return new MyMod(file, meta);
	}

	public MyMod(Fi root, JsonValue meta) {
		if (meta == null) throw new IllegalArgumentException("meta cannot be null");
		this.root = root;
		this.meta = meta;
	}

	public Fi spritesFi() {
		return root.child("sprites").exists() && root.isDirectory() ? root.child("sprites") : null;
	}

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

	public Mods.LoadedMod toMod(){
		return new Mods.LoadedMod(root, root, new Mod() {}, null, json.fromJson(Mods.ModMeta.class, meta + ""));
	}
}

