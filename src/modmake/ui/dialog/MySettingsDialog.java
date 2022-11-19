package modmake.ui.dialog;

import arc.func.Boolc;
import arc.func.Boolp;
import arc.graphics.Color;
import arc.math.geom.Vec2;
import arc.scene.Element;
import arc.scene.event.Touchable;
import arc.scene.ui.CheckBox;
import arc.scene.ui.Label;
import arc.scene.ui.Slider;
import arc.scene.ui.Tooltip;
import arc.scene.ui.layout.Table;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.*;
import mindustry.graphics.Pal;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;
import mindustry.ui.dialogs.SettingsMenuDialog;
import modmake.ui.MyStyles;
import modmake.util.img.Stack;

import java.util.Objects;

import static arc.Core.bundle;
import static modmake.components.DataHandle.settings;

public class MySettingsDialog extends BaseDialog {
	static Table pane = new Table();

	public MySettingsDialog() {
		super("@settings");

		cont.pane(MyStyles.nonePane, p -> p.add(pane).width(400).get()).fillX();
		pane.left().defaults().left();

		addCloseButton();
	}
	/*
	class BaseSetting<T> {
		public String name, key, info;
		public OrderedMap<String, BaseSetting> parent;
		public T value;

		public BaseSetting(String objName, String key, String name, T defaultValue, String info) {
			object.get(objName).set(key, this);

			this.key = key;
			this.name = name;
			this.parent = object.get(objName);
			var v = settings.get(modName + key);
			this.value = v != null ? (T) v : defaultValue;
			this.info = info;
		}

		public Element build() {
			return new Label("???");
		}

		public void set(T value) {
			this.value = value;
		}
	}

	class CheckBox extends BaseSetting<Boolean> {
		public CheckBox(String objName, String key, String name, boolean defaultValue, String info) {
			super(objName, key, name, defaultValue, info);
		}

		arc.scene.ui.CheckBox elem;

		public Element build() {
			elem = new arc.scene.ui.CheckBox(name);
			elem.changed(() -> set(elem.isChecked()));
			elem.setChecked(value);
			elem.update(() -> elem.setDisabled(parent.disabled));
			elem.setOrigin(Align.left);
			return elem;
		}

		public void set(boolean value) {
			this.value = value;
			settings.set(modName + key, value);
			elem.setChecked(value);
		}
	}

	class Slider extends BaseSetting{
		public Slider(objName, key, name, defaultValue, min, max, step, sp, info) {
		BaseSetting.call(this, objName, key, name, defaultValue, info);
		let v = +this.value;
		this.value = isNaN(v) ? defaultValue : v;

		let elem;
		this.build = function() {
			let content = new Table()

			let slider = elem = new Slider(min, max, step, false);
			slider.setValue(this.value);

			let label = new Label("", Styles.outlineLabel);
			content.add(this.name, Styles.outlineLabel).left().growX().wrap();
			content.add(label).padLeft(10).right();
			content.margin(3, 20, 3, 20);
			content.touchable = Touchable.disabled;

			slider.changed(() = > {
					this.set(slider.getValue());
			label.setText("" + sp(slider.getValue()));
		});
			slider.change();

			let t = new Table()
			t.defaults().growX()
			t.stack(slider, content).width(360).left().padTop(4).growX();

			return t;
		}
		this.set = function(value) {
			this.value = value;
			settings.set(mod_Name + key, value);
			elem.setValue(value);
		}
	}


	function getValue(objName, key) {
		return !object['obj_' + objName].disabled && object['obj_' + objName][key].value;
	}

	function setValue(objName, key, value) {
		object['obj_' + objName][key].set(value);
	}

	let object = {
			name:'settings',tables:{},
		obj_base:{},
		obj_editor:{},
		obj_loadMod:{},
		load(){
		let dialog=this.ui=new BaseDialog("设置");
		let cont
		dialog.cont.pane(cons(p=>cont=p.table().width(400).get())).fillX()
		cont.left().defaults().left()
		function addSetting(displayName,obj){
		cont.add(displayName).color(Pal.accent).row()
		cont.table(cons(t=>{
		t.left().defaults().left()
		let obj1=obj
		for(let k in obj1){
		let setting=obj1[k];
		if(typeof setting.info=="string")t.add(setting.info).padTop(5).row();
		t.add(setting.build()).left().row()
		}
		})).fillX().padLeft(16).row();
		}
		addSetting("基础",this.obj_base)
		addSetting("编辑器",this.obj_editor)
		addSetting("加载mod",this.obj_loadMod)

		dialog.addCloseButton();
		},
		buildConfiguration(){
		this.ui.show();
		}
		}*/

	public static void addSetting(String displayName, Boolp dis, Setting... settings) {
		for (var setting : settings) {
			setting.bp = dis;
			all.put(setting.name, setting);
		}
		Time.runTask(0, () -> {
			pane.add(displayName).growY().left().color(Pal.accent).row();
			pane.table(t -> {
				t.left().defaults().left();
				for (var setting : settings) {
					setting.add(t);
				}
			}).growX().padLeft(16).row();
		});
	}

	public static ObjectMap<String, Setting> all = new ObjectMap<>();

	static {
		addSetting("基础", null,
				new CheckSetting("display-content-sprite", true, b -> {}),
				new CheckSetting("display_mod_logo", false, b -> {}),
				new CheckSetting("not_show_again", false, b -> {}),
				new CheckSetting("auto_load_mod", false, b -> {}));

		addSetting("编辑器", null,
				new CheckSetting("auto_fold_code", false, b -> {}),
				new CheckSetting("display_deprecated", false, b -> {}),
				new CheckSetting("point_out_unknown_field", false, b -> {}),
				new CheckSetting("colorful_table", false, b -> {}),
				new RadioSetting("format", "json",
						Seq.with("hjsonMin", "hjson", "jsonMin", "json"))
		);

		addSetting("图集", null,
				new CheckSetting("auto_save_image", false, b -> {}),
				new RadioSetting("auto_load_sprites", "不加载", Seq.with("启动时加载一次", "打开项目加载一次", "不自动加载"))
				, new SliderSetting("max_load_sprite_size", 10000, 0, 100000, 100, v -> (v / 1024) + " [lightgray]KB")
				, new SliderSetting("max_img_stack_buffer_size", 15, 0, 100, 1, v -> {
					Stack.maxSize = v;
					return v + "";
				})
		);

		addSetting("加载mod", () -> !settings.getBool("auto_load_mod"),
				new CheckSetting("load_sprites", false, b -> {}),
				new CheckSetting("load_icons", false, b -> {}),
				new CheckSetting("display_exception", true, b -> {}));
// new _Slider("loadMod", "compiling_times_per_second", "每秒最多编译次数", 1000, 500, 10000, 10, v => v + "/次");
	}


	public abstract static class Setting {
		public String name;
		public String title;
		public @Nullable
		String description;
		public Boolp bp;

		public Setting(String name) {
			this.name = name;
			String winkey = "setting." + name + ".name.windows";
			title = OS.isWindows && bundle.has(winkey) ? bundle.get(winkey) : bundle.get("setting." + name + ".name", name);
			description = bundle.getOrNull("setting." + name + ".description");
		}

		public abstract void add(Table table);

		public void addDesc(Element elem) {
			if (description == null) return;

			elem.addListener(new Tooltip(t -> t.background(Styles.black8).margin(4f).add(description).color(Color.lightGray)) {
				{
					allowMobile = true;
				}

				@Override
				protected void setContainerPosition(Element element, float x, float y) {
					this.targetActor = element;
					Vec2 pos = element.localToStageCoordinates(Tmp.v1.set(0, 0));
					container.pack();
					container.setPosition(pos.x, pos.y, Align.topLeft);
					container.setOrigin(0, element.getHeight());
				}
			});
		}
	}

	public static class CheckSetting extends Setting {
		public boolean def;
		Boolc changed;

		public CheckSetting(String name, boolean def, Boolc changed) {
			super(name);
			this.def = def;
			this.changed = changed;
		}

		@Override
		public void add(Table table) {
			CheckBox box = new CheckBox(title);

			box.update(() -> box.setChecked(settings.getBool(name)));
			box.setDisabled(bp);

			box.changed(() -> {
				settings.put(name, String.valueOf(box.isChecked()));
				if (changed != null) {
					changed.get(box.isChecked());
				}
			});

			box.left();
			table.add(box).left().padTop(3f);
			addDesc(box);
			table.row();
		}
	}

	public static class RadioSetting extends Setting {
		//		ButtonGroup<CheckBox> group = new ButtonGroup<>();
		Seq<String> children;
		String value;

		public RadioSetting(String name, Seq<String> children) {
			this(name, children.get(0), children);
		}

		public RadioSetting(String name, String def, Seq<String> children) {
			super(name);
			this.children = children;
			if (settings.containsKey(name)) {
				value = settings.get(name, def);
			} else {
				settings.put(name, value = def);
			}
		}

		@Override
		public void add(Table table) {
			table.add(title, Color.gold).row();
			table.table(t -> {
				t.left().defaults().left();
				Table[] cont = {t.table().get()};
				int[] c = {0};
				children.each(child -> {
					String title = bundle.get("setting." + child + ".name", child);
					var box = new CheckBox(title);
					box.changed(() -> settings.put(name, value = child));
					box.update(() -> box.setChecked(Objects.equals(value, child)));
//					group.add(box);
					cont[0].add(box).padRight(4f);
					if (++c[0] % 2 == 0) {
						t.row();
						cont[0] = t.table().get();
					}
				});
			}).padLeft(4f).row();
		}
	}

	public static class SliderSetting extends Setting {
		int def, min, max, step;
		SettingsMenuDialog.StringProcessor sp;

		public SliderSetting(String name, int def, int min, int max, int step, SettingsMenuDialog.StringProcessor s) {
			super(name);
			this.def = def;
			this.min = min;
			this.max = max;
			this.step = step;
			this.sp = s;
		}

		@Override
		public void add(Table table) {
			Slider slider = new Slider(min, max, step, false);
			if (bp != null) slider.update(() -> slider.setDisabled(bp.get()));

			slider.setValue(settings.getInt(name, def));

			Label value = new Label("", Styles.outlineLabel);
			Table content = new Table();
			content.add(title, Styles.outlineLabel).left().growX().wrap();
			content.add(value).padLeft(10f).right();
			content.touchable = Touchable.disabled;

			slider.changed(() -> {
				settings.put(name, String.valueOf(slider.getValue()));
				value.setText(sp.get((int) slider.getValue()));
			});
			slider.change();

			var cell = table.stack(slider, content).growX().left().padTop(4f);
			addDesc(cell.get());
			table.row();
		}
	}
}
