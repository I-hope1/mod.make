package modmake.ui.dialog;

import arc.Core;
import arc.files.Fi;
import arc.func.Cons2;
import arc.func.Prov;
import arc.graphics.Color;
import arc.scene.style.TextureRegionDrawable;
import arc.scene.ui.TextButton;
import arc.scene.ui.TextField;
import arc.scene.ui.Tooltip;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import mindustry.Vars;
import mindustry.content.TechTree;
import mindustry.gen.Icon;
import mindustry.gen.Tex;
import mindustry.graphics.Pal;
import mindustry.type.*;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;
import mindustry.world.Block;
import modmake.IntUI;
import modmake.components.AddFieldBtn;
import modmake.components.DataHandle;
import modmake.components.MyMod;
import modmake.components.TypeSelection;
import modmake.components.constructor.MyObject;
import modmake.util.Classes;
import modmake.util.Fields;

import java.util.Objects;
import java.util.StringJoiner;
import java.util.regex.Pattern;

import static arc.Core.bundle;
import static modmake.components.DataHandle.formatPrint;
import static modmake.util.load.ContentSeq.cTypeMap;
import static modmake.util.load.ContentSeq.types;
import static modmake.util.Tools.*;

@SuppressWarnings("ALL")
public class Editor extends BaseDialog {
	TextField fileName;
	Table topTable;
	Table pane;
	TextButton checkBtn;
	Result result = new Result();

	class Result {
		public Prov<String> value, typeNameProv;
		Prov<Class<?>> typeProv;
		// for json
		MyObject obj;

		public Class<?> type() {
			return typeProv == null ? null : typeProv.get();
		}

		public String typeName() {
			return typeNameProv == null ? null : typeNameProv.get();
		}

		public void check(){}
	}

	Fi file;
	MyMod mod;

	public Editor() {
		super(bundle.get("code-editor", "code editor"));
	}

	public void load() {
		// 为了更好看，仿写 CustomGameDialog
		clearChildren();
		add(titleTable).growX().row();
		stack(cont, buttons).grow();
		buttons.bottom();

		cont.top().defaults().padTop(0).top();

		fileName = new TextField();
		topTable = cont.table().get();

		cont.row();
		cont.pane(p -> pane = p).fillX().fillY().grow().row();

		// to do
		/*checkBtn = new TextButton("检查", Styles.defaultt);
		checkBtn.add(new Image(icons.get("inspect-code"))).size(32);
        checkBtn.getCells().reverse();
        checkBtn.clicked(result::check);*/

		buttons.button("$back", Icon.left, this::hide).size(220, 70);

		buttons.button("$ok", Icon.ok, () -> {
			parse();
			if (topTable.getChildren().size > 0) {
				var toFile = file.sibling(fileName.getText() + "." + file.extension());
				file.moveTo(toFile);
				file = toFile;
			}
			hide();
		}).size(220, 70);

		addCloseListener();
	}

	public void edit(Fi file, MyMod mod) {
		if (!file.exists()) file.writeString("");

		this.file = file;
		this.mod = mod;

		if (!Objects.equals(file.extension(), "properties")) {
			if (topTable.getChildren().size == 0) {
				topTable.add("$fileName");
				topTable.add(fileName);
				topTable.add(checkBtn).padLeft(6);
			}
			fileName.setText(file.nameWithoutExtension());
		} else topTable.clearChildren();

		build();

		show();
	}

	public void buildJson(Fi file) {

		MyObject obj = as(DataHandle.parse(file.readString()));
		result.value = () -> "" + obj;
		result.obj = obj;

		String parentName = ((Prov<String>) () -> {
			Fi contentRoot = mod.root.child("content");

			Fi f = file;
			String res = f.path().replace(contentRoot.path(), "");
			if (res.equals(f.path())) return "block";
			while (!f.parent().equals(contentRoot)) {
				f = f.parent();
			}
			return cTypeMap.get(f.name());
		}).get();
		String typeName;
		if (obj.has("type") && Pattern.compile("^block|weather$").matcher(parentName).find()) {
			typeName = "" + obj.remove("type");
		} else if (types.get(parentName) != null && types.get(parentName).get(0) != null) {
			typeName = types.get(parentName).get(0).getSimpleName();
		} else typeName = "none";
		// 转换为首字母大小
		typeName = typeName.substring(0, 1).toUpperCase() + typeName.substring(1);

		var selection = new TypeSelection(Classes.get(typeName), typeName, types.get(parentName), true);
		pane.add(selection.table).padBottom(4).row();
		result.typeProv = selection::type;
		result.typeNameProv = selection::typeName;

		var table = new Table();
		var fields = new Fields(obj, selection::type, table);

		pane.table(Tex.button, t -> {
			t.add(table).fillX().pad(4).row();

			// 添加接口
			t.add(new AddFieldBtn(obj, fields, () -> Classes.get(selection.typeName()))).fillX().growX();
			t.row();
			// 研究
			t.table(research -> {
				var k = "research";
				Object o = obj.get(k);
				MyObject researchObj = o instanceof MyObject ? (MyObject) o : o instanceof String ? MyObject.of("parent", "" + o) : null;
				String value = researchObj == null ? "" : "" + researchObj.get("parent");
				research.add(bundle.get(k, k));
				research.add(":");

				var techs = TechTree.all;

				var btn = new TextButton(!value.equals("") ?
						or(nullCheck(techs.find(node -> node.content.name.equals(value)), node -> node.content.localizedName), value) :
						"$none", Styles.cleart);
				btn.clicked(() -> IntUI.showSelectTable(btn, (p, hide, v) -> {
					p.clearChildren();
					p.button("$none", Styles.cleart, () -> {
						obj.remove(k);
						btn.setText("$none");
						hide.run();
					}).growX().height(32).checked(__ -> obj.has(k)).row();
					p.image(Tex.whiteui, Pal.accent).growX().height(3).pad(4).row();

					var cont = p.table().get();
					int length = 6;
					Table[] tableArr = {new Table(), new Table(), new Table(),
							new Table(), new Table(), new Table()};


					var reg = Pattern.compile(v, Pattern.CASE_INSENSITIVE);
					int i = 0;
					var cols = Vars.mobile ? 6 : 10;

					// 遍历所有tech
					for (var node : techs) {
						var content = node.content;
						if (!reg.matcher(content.name).find() && !reg.matcher(content.localizedName).find()) continue;

						var index = content instanceof Item ? 0
								: content instanceof Liquid ? 1
								: content instanceof Block ? 2
								: content instanceof UnitType ? 3
								: content instanceof SectorPreset ? 4
								: 5;
						var table1 = tableArr[index];
						var button = table1.button(new TextureRegionDrawable(content.uiIcon),
								Styles.clearToggleTransi, 32, () -> {
									obj.put(k, content.name);
									btn.setText(content.localizedName);
									hide.run();
								}).size(42).get();
						button.update(() -> button.setChecked(obj.get(k, "").equals(content.name)));

//						if (!Vars.mobile)
						button.addListener(new Tooltip(tool -> tool.background(Tex.button)
								.add(content.localizedName)));

						if (table1.getChildren().size % cols == 0) {
							table1.row();
						}
					}

					for (int j = 0; j < length; j++) {
						Table table1 = tableArr[j];
						cont.add(table1).growX().left().row();
						if (table1.getChildren().size != 0 && j < length - 2) {
							cont.image(Tex.whiteui, Pal.accent).growX().height(3).pad(4).row();
						}
					}
				}, true));
				research.add(btn).size(150, 60);
			}).fillX();
		}).fillX().row();

		fields.map.each((k, v) -> {
			if ((k + "").equals("research")) return;

			fields.add(null, k);
		});

	}

	public void buildPro(Fi file) {
		var content = file.readString();
		var str = content.split("\n");
		var cont = pane.table(Styles.none).padLeft(3).get();
		class Bundle {
			public String name;
		}
		Seq<Bundle> arr = new Seq<>();
		Cons2<String, String> fun = (from, to) -> {
			var table = cont.table(Tex.button, (t -> {
				if (from.equals("#")) {
					t.add("#").padRight(6);
					var field = new TextField(to);
					t.add(field).fillX().get();
					var bundle = new Bundle() {
						public String toString() {
							return "# " + field.getText();
						}
					};
					bundle.name = "这个" + Core.bundle.get("annotation", "annotation");
					arr.add(bundle);
					return;
				}
				var field1 = new TextField(from);
				t.add(field1).width(200).get();
				t.add(" = ", Color.gray);
				var field2 = new TextField(to);
				t.add(field2).width(200).get();
				var bundle = new Bundle() {
					@Override
					public String toString() {
						return field1.getText().isBlank() && field2.getText().isBlank() ? field1.getText() + " = " + field2.getText() : " ";
					}
				};
				bundle.name = from;
				field1.changed(() -> bundle.name = field1.getText());
				arr.add(bundle);
			})).get();
			var index = arr.size - 1;
			IntUI.doubleClick(table, () -> {
				Vars.ui.showConfirm("$confirm", Core.bundle.format("confirm.remove",
								arr.get(index).name), () -> {
							table.remove();
							arr.remove(index);
						}
				);
			}, () -> {});
			cont.row();
		};
		for (String s : str) {
			if (s.isBlank()) continue;
			// 解析注释
			if (Pattern.compile("^\\s*#").matcher(s).find()) {
				var res = s.replaceAll("^\\s*#\\s*", "");
				fun.get("#", res);
				continue;
			}
			var array = s.split("\\s*=\\s*");
			if (array.length == 2) {
				fun.get(array[0], array[1]);
			}
		}

		pane.row();
		pane.table(btn -> {
			btn.button("$add", Icon.add, () -> fun.get("", " ")).size(210, 64);
			btn.button(Core.bundle.get("content.add", "add") + Core.bundle.get("annotation", "annotation"),
					() -> fun.get("#", "")).size(210, 64);
		});
		result.value = () -> {
			var join = new StringJoiner("\n");
			for (var bundle1 : arr) {
				join.add(bundle1 + "");
			}

			return join + "";
		};
	}

	// 编辑代码
	public void build() {
		pane.clearChildren();
		result = new Result();
		var file = this.file;
		var ext = file.extension();
		if (ext.equalsIgnoreCase("hjson") || ext.equalsIgnoreCase("json")) buildJson(file);

		else if (ext.equalsIgnoreCase("properties")) buildPro(file);

		else {
			var area = pane.area(file.readString(), (t -> {

			})).size(Math.min(Core.graphics.getWidth(), Core.graphics.getHeight()) - 200).get();
			result.value = () -> area.getText().replaceAll("\\r", "\n");
		}

		// 为了不阻挡最底下的部分
		pane.image().color(Color.clear).height(74);
	}


	public void parseJson() {
		var obj = result.obj;
		var typeName = result.typeName();
		var type = result.type();
		if (type != null) {
			if (UnitType.class.isAssignableFrom(type) ||
					Item.class.isAssignableFrom(type) ||
					Liquid.class.isAssignableFrom(type) ||
					StatusEffect.class.isAssignableFrom(type) ||
					SectorPreset.class.isAssignableFrom(type)) {} else if (!obj.has("type"))
				obj.put("type", typeName);
		}

		file.writeString(formatPrint(result.value.get()));
		/*var dir = mod.root.child("content").child(type == null ? "blocks" :
				(UnitType.class.isAssignableFrom(type) ? "unit"
						: Item.class.isAssignableFrom(type) ? "item"
						: Liquid.class.isAssignableFrom(type) ? "liquid"
						: StatusEffect.class.isAssignableFrom(type) ? "statu"
						: SectorPreset.class.isAssignableFrom(type) ? "sector"
						: Weather.class.isAssignableFrom(type) ? "weather"
						: "block") + "s");
		var toFile = dir.child(file.name());
		file.moveTo(toFile);
		file = toFile;*/
	}


	// 编译代码
	public void parse() {
		if (file.extEquals("hjson") || file.extEquals("json")) parseJson();

		else file.writeString(result.value.get());
	}
}
