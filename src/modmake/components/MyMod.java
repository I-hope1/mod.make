package modmake.components;

import arc.Core;
import arc.files.Fi;
import arc.graphics.Texture;
import arc.graphics.g2d.TextureRegion;
import arc.util.serialization.JsonValue;

import java.util.Objects;

public class MyMod {
	public Fi file;
	public JsonValue meta;

	public static MyMod set(Fi file) {
		String json = file.child("mod.json").exists() ? file.child("mod.json").readString() :
				file.child("mod.hjson").exists() ? file.child("mod.hjson").readString() : null;
		if (json == null) return null;
		JsonValue meta = dataHandle.hjsonParse(json);
		if (meta == null) return null;
		return new MyMod(file, meta);
	}

	public MyMod(Fi file, JsonValue meta) {
		if (meta == null) throw new IllegalArgumentException("meta cannot be null");
		this.file = file;
		this.meta = meta;
	}

	public Fi spritesFi() {
		return file.child("sprites").exists() && file.isDirectory() ? file.child("sprites") : null;
	}

	public String displayName() {
		return meta.getString("displayName", meta.getString("name"));
	}

	public TextureRegion logo() {
		for (Fi f : file.child("sprites-override").findAll()) {
			if (Objects.equals(f.name(), "logo.png")) {
				return new TextureRegion(new Texture(f));
			}
		}
		return Core.atlas.find("error");
	}
}

