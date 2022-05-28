package modmake.util;

import arc.func.Cons;
import arc.func.Cons3;
import arc.func.Prov;
import arc.scene.style.Drawable;
import arc.scene.ui.layout.Cell;
import arc.scene.ui.layout.Table;
import arc.struct.ObjectMap;
import arc.util.Log;
import mindustry.Vars;
import mindustry.gen.Tex;
import modmake.components.constructor.MyObject;
import modmake.ui.styles;
import rhino.ScriptableObject;

import static modmake.components.dataHandle.settings;
import static modmake.util.BuildContent.or;

public class Fields {
	private static Drawable[] backgrounds;
	public MyObject map;
	public Table table;
	public Class<?> type;
	public Fields(MyObject value, Prov<Class<?>> type, Table table) throws IllegalArgumentException, NullPointerException {
		if (value == null) throw new NullPointerException("'value' can't be null");

		this.table = table;
		this.type = type.get();
		this.map = value;
	}

	int i = 0;
	ObjectMap<String, Cell<Table>> data = new ObjectMap<>();
	public void add(Table table, String key) {
		add(table, key, null);
	}
	public void add(Table table, String key, Object value) {
		if (value != null && !map.has(key)) {
			map.put(key, value);
		}
		var t = or(table, json(this, i++, key));
		t.name = key;
		t.defaults().fillX();
		data.put(key, this.table.add(t).fillX());

		this.table.row();
	}
	public void remove(String key) {
		this.map.remove(key);
		Cell<?> cell = data.get(key);
		data.remove(key);
		if (cell != null && cell.get() != null) {
			cell.get().remove();
		} else {
			Log.err("can't remove key: " + key);
		}
	}
	public void setTable(String key, Table table){
		var cell = data.get(key);
		if (cell != null) {
			cell.setElement(table);
		}
	}
	static Cons3<Fields, Table, String> build;
	public static void load(Cons3<Fields, Table, String> _build) {
		build = _build;
		backgrounds = new Drawable[]{
				styles.whiteui.tint(.6f, .2f, .6f, 1f),
				styles.whiteui.tint(.6f, .6f, .2f, 1f),
				styles.whiteui.tint(.2f, .6f, .2f, 1f),
				styles.whiteui.tint(.2f, .6f, .6f, 1f),
		};
	}
	public static Table build(int i, Cons<Table> cons) {
		Drawable bg;
		if (settings.getBool("colorful_table")) {
			bg = backgrounds[i % 4];
		} else {
			bg = Tex.pane;
		}
		return new Table(bg, cons);
	}

	static {
		var scope = Vars.mods.getScripts().scope;
		ScriptableObject.getProperty(scope,"buildContent");
	}
	public static Table json(Fields fields, int i, String key) {
		return build(i, table -> {
				table.left().defaults().left();
				build.get(fields, table, key);
//				buildContent.build(fields.type, fields, table, key, fields.map.get(key));
		});
	}
}