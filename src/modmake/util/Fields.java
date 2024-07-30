package modmake.util;

import arc.func.*;
import arc.input.KeyCode;
import arc.scene.*;
import arc.scene.event.*;
import arc.scene.style.Drawable;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.OrderedMap;
import arc.util.*;
import arc.util.Timer.Task;
import mindustry.gen.Tex;
import mindustry.ui.BorderImage;
import modmake.components.MoveListener;
import modmake.components.constructor.MyObject;
import modmake.components.limit.LimitTable;
import modmake.ui.MyStyles;
import modmake.ui.dialog.Editor;
import modmake.util.tools.FilterTable;

import java.lang.reflect.Field;

import static modmake.components.DataHandle.dsettings;
import static modmake.util.BuildContent.*;
import static modmake.util.Tools.or;

public class Fields {
	private static Drawable[]               backgrounds;
	public         MyObject<Object, Object> map;
	public         Table                    children;

	public Class<?> type;
	public int      i = 0;

	public int nextId() {
		return i++;
	}

	public Fields(MyObject value, Prov<Class<?>> type, Table table)
	 throws IllegalArgumentException, NullPointerException {
		if (value == null) throw new NullPointerException("'value' can't be null");

		this.children = table;
		this.type = type.get();
		table.update(() -> this.type = type.get());
		this.map = value;

		new MyDragListener();
	}

	OrderedMap<Object, Cell<Table>> data = new OrderedMap<>();

	public void add(Table table, Object key) {
		add(table, key, null);
	}

	public void add(Table table, Object key, Object value) {
		add(table, getField(type, key), key, value);
	}

	Element placeholder = new BorderImage();
	Table   fireElem;
	Object  fromKey, toKey;
	Cell<?> placeholderCell;

	public void add(Table table, Field field, Object key, Object value) {
		if (value != null && !map.has(key)) {
			map.put(key, value);
		}
		var t = or(table, () -> json(this, nextId(), field, key, value));
		t.name = String.valueOf(key);
		t.defaults().growX();
		t.touchable = Touchable.enabled;
		Cell<Table> cell = this.children.add(t).growX();
		t.addCaptureListener(new FieldInputListener(t, cell, key));

		data.put(key, cell);

		this.children.row();
	}

	public void remove(Object key) {
		map.remove(key);
		Cell<?> cell = data.get(key);
		data.remove(key);
		if (cell != null && cell.get() != null) {
			cell.get().remove();
		} else {
			Log.err("can't remove key: " + key);
		}
	}

	public void setTable(Object key, Table table) {
		var cell = data.get(key);
		if (cell != null) {
			cell.setElement(table);
		}
	}

	public static void load() {
		backgrounds = new Drawable[]{
		 MyStyles.whiteui.tint(.6f, .2f, .6f, 1f),
		 MyStyles.whiteui.tint(.6f, .6f, .2f, 1f),
		 MyStyles.whiteui.tint(.2f, .6f, .2f, 1f),
		 MyStyles.whiteui.tint(.2f, .6f, .6f, 1f),
		 };
	}

	public static Table build(int i, Cons<FilterTable<String>> cons) {
		Drawable bg;
		if (dsettings.getBool("colorful_table")) {
			bg = backgrounds[i % 4];
		} else {
			bg = Tex.pane;
		}
		return new LimitTable(t -> t.add(new FilterTable<>(bg, cons)).grow());
	}

	public static Table json(Fields fields, int i, Object key) {
		return json(fields, i, key, null);
	}

	public static Table json(Fields fields, int i, Object key, Object value) {
		return json(fields, i, null, key, value);
	}

	public static Table json(Fields fields, int i, Field field, Object key, Object value) {
		return build(i, table -> {
			table.left().defaults().left();
			String strK = "" + key;
			table.bind(strK);
			BuildContent.build(fields.type, field, fields, table, key, or(value, () -> fields.map.get(key)));
			Time.runTask(0, () -> {
				Editor editor = getEditor(table);
				table.addUpdateListener(() -> editor.pattern);
			});
			table.unbind();
		});
	}

	private class MyDragListener extends MoveListener {
		public MyDragListener() {
			super(children, children);
			children.removeListener(this);
			children.getCaptureListeners().insert(0, this);
		}

		Task task = TaskManager.newTask(() -> { });

		public boolean touchDown(InputEvent event, float x, float y, int pointer, KeyCode button) {
			Element actor = event.targetActor;
			while (true) {
				if (actor == null || actor instanceof Button || actor instanceof TextField) return false;
				if (actor instanceof Table && actor.getCaptureListeners().contains((Boolf<EventListener>) l -> l instanceof FieldInputListener)) {
					if (actor.parent != children) return false;
					fireElem = (Table) actor;
					fromKey = data.findKey(children.getCell(fireElem), true);
					break;
				}
				actor = actor.parent;
			}
			TaskManager.reSchedule(0.6f, task);
			return fromKey != null;
		}

		boolean hasDragged;

		public void touchDragged(InputEvent event, float x, float y, int pointer) {
			if (task.isScheduled()) return;
			ScrollPane pane = findPane(children);
			if (pane != null) pane.cancel();
			if (!hasDragged) {
				Element     t    = fireElem;
				Cell<Table> cell = children.getCell(t);
				cell.size(t.getWidth() / Scl.scl(), t.getHeight() / Scl.scl());
				cell.setElement(placeholder);
				placeholderCell = cell;
				children.addChild(t);
				lastMain.set(t.x, t.y);
				hasDragged = true;
			}

			main = fireElem;
			touch = children;
			fireElem.touchable = Touchable.disabled;
			fireElem.toFront();
			hasDragged = true;
		}

		public void touchUp(InputEvent event, float x, float y, int pointer, KeyCode button) {
			if (!hasDragged || placeholderCell == null) return;
			placeholderCell.size(Float.NEGATIVE_INFINITY);
			placeholderCell.setElement(fireElem);
			fireElem.touchable = Touchable.enabled;
			if (toKey != null && fromKey != toKey && data.orderedKeys().contains(toKey)) {
				data.orderedKeys().swap(data.orderedKeys().indexOf(fromKey), data.orderedKeys().indexOf(toKey));
				map.orderedKeys().swap(map.orderedKeys().indexOf(fromKey), map.orderedKeys().indexOf(toKey));
			}
			if (hasDragged) {
				event.stop();
			}
			placeholderCell = null;
			fireElem = null;
			hasDragged = false;
		}

		public void display(float x, float y) {
			main.x = x;
			main.y = y;
		}
	}
	private static ScrollPane findPane(Element actor) {
		while (actor != null) {
			actor = actor.parent;
			if (actor instanceof ScrollPane) return (ScrollPane) actor;
		}
		return null;
	}


	private class FieldInputListener extends InputListener {
		private final Table       t;
		private       Cell<Table> cell;
		private final Object      key;
		public        float       lastTime;

		public FieldInputListener(Table t, Cell<Table> cell, Object key) {
			this.t = t;
			this.cell = cell;
			this.key = key;
		}

		public void enter(InputEvent event, float x, float y, int pointer, Element fromActor) {
			if (placeholderCell == null || placeholderCell == cell ||
			    fromActor == t || lastTime == Time.globalTime ||
			    (fromActor != null && fromActor.parent != Fields.this.children)) return;
			lastTime = Time.globalTime;

			cell.size(t.getWidth() / Scl.scl(), t.getHeight() / Scl.scl());
			placeholderCell.clearElement();
			cell.clearElement();
			var tmp = placeholderCell.setElement(t);
			placeholderCell = cell.setElement(placeholder);
			cell = tmp;
			cell.size(Float.NEGATIVE_INFINITY);

			toKey = key;
		}
	}
}
