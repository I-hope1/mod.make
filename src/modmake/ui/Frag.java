package modmake.ui;

import arc.Core;
import arc.graphics.Color;
import arc.scene.ui.layout.Table;
import arc.util.Log;
import mindustry.gen.Tex;
import mindustry.ui.Styles;
import modmake.Contents;
import modmake.components.MoveListener;

public class Frag extends Table {
	public void load() {

		new MoveListener(image().color(Color.sky).margin(0f).pad(0f).padBottom(-4f).fillX().height(40f)
				.get(), this);
		row();
		table(Tex.whiteui, t -> Contents.all.each(cont -> {
			if (cont == null || !cont.loadable())
				return;

			cont.btn = t.button(cont.localizedName(), Styles.cleart, cont::build).size(120f, 40f).get();
			cont.load();
			t.row();
		})).row();
		left().bottom();

		// auto exit
		/*Fi fi = dataDirectory.child("mods(I hope...)").child("exit-mzolamkakqna");
		if (fi.exists()) {
			fi.delete();
		}
		update(() -> {
			if (fi.exists()) {
				fi.delete();
				Core.app.exit();
			}
		});*/

		Core.scene.add(this);
		Log.info(this);
	}
}