package modmake.ui.dialog;

import arc.Core;
import arc.files.Fi;
import arc.func.*;
import arc.graphics.Color;
import arc.scene.ui.*;
import arc.scene.ui.layout.Table;
import arc.struct.*;
import arc.util.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.ui.Styles;
import modmake.IntUI;
import modmake.components.*;
import modmake.components.constructor.MyObject;
import modmake.components.input.area.TextAreaTable;
import modmake.components.input.area.TextAreaTable.MyTextArea;
import modmake.components.input.highlight.*;
import modmake.components.limit.LimitTable;
import modmake.util.*;
import modmake.util.load.ContentVars;
import modmake.util.tools.Search;

import java.util.*;
import java.util.regex.Pattern;

import static arc.Core.bundle;
import static mindustry.Vars.ui;
import static modmake.components.DataHandle.formatPrint;
import static modmake.util.Tools.*;
import static modmake.util.load.ContentVars.*;
import static modmake.util.tools.Tools.compileRegExp;

//@SuppressWarnings("ALL")
public class Editor extends Window {
	TextField  fileName;
	Table      topTable;
	Table      pane;
	TextButton checkBtn;
	Result     result = new Result();

	static class Result {
		public Prov<String> value, typeNameProv;
		Prov<Class<?>> typeProv;
		// for json
		MyObject       obj;

		public Class<?> type() {
			return typeProv == null ? null : typeProv.get();
		}

		public String typeName() {
			return typeNameProv == null ? null : typeNameProv.get();
		}

		public void check() {}
	}

	Fi     file;
	String extension;
	MyMod  mod;

	public Editor() {
		super(bundle.get("code-editor", "code editor")
		 , 120, 80, true, false);
	}

	public void load() {
		// 为了更好看，仿写 CustomGameDialog
		// clearChildren();a'a'a
		// add(titleTable).growX().row();
		// stack(cont, buttons).grow();
		buttons.bottom();

		cont.top().defaults().padTop(0).top();

		fileName = new TextField();
		// fileName.setValidator(() -> {});
		topTable = cont.table().get();
		new Search((__, text) -> pattern = compileRegExp(text)).build(cont, null);

		cont.row();
		cont.pane(p -> pane = p).grow().colspan(2);
		cont.row();

		// to do
		/*checkBtn = new TextButton("检查", Styles.defaultt);
		checkBtn.add(new Image(icons.get("inspect-code"))).size(32);
        checkBtn.getCells().reverse();
        checkBtn.clicked(result::check);*/

		buttons.button("@back", Icon.left, this::hide).size(220, 70);

		buttons.button("@ok", Icon.ok, () -> {
			parse();
			if (moveFile) {
				Fi toFile = file.sibling(fileName.getText() + "." + file.extension());
				file.moveTo(toFile);
				file = toFile;
			}
			hide();
		}).size(220, 70);

		// addCloseListener();
		hidden(() -> Time.runTask(0, () -> file = null));
	}

	/** 是否移动文件 */
	public boolean moveFile, textEdit;

	public void edit(Fi file, MyMod mod, boolean textEdit) {
		if (!file.exists()) file.writeString("");

		this.file = file;
		this.extension = file.extension();
		this.mod = mod;
		this.textEdit = textEdit;

		topTable.clearChildren();
		if (!Objects.equals(extension, "properties")) {
			moveFile = true;
			topTable.add("@fileName");
			topTable.add(fileName);
			topTable.add(checkBtn).padLeft(6);
			fileName.setText(file.nameWithoutExtension());
		} else {
			moveFile = false;
		}

		build();

		show();
	}
	public void edit(Fi file, MyMod mod) {
		edit(file, mod, false);
	}


	public static ObjectSet<String> set = ObjectSet.with(
	 "block", "weather", "unit", "liquid"
	);

	public Pattern pattern;
	public void buildJson(Fi file) {
		MyObject<String, ?> obj = as(DataHandle.parse(file.readString()));
		result.value = () -> String.valueOf(obj);
		result.obj = obj;

		String parentName = getParentName(file);
		// 不同的类会有不同的type
		String typeKey = getTypeKey(parentName);
		// 获取type名称
		String typeName = resolveType(obj, parentName, typeKey);
		// 转换为首字母大小
		typeName = typeName.substring(0, 1).toUpperCase() + typeName.substring(1);
		// Log.info(obj);

		String finalTypeName = typeName;
		var selection = new TypeSelection(or(Classes.get(typeName), () -> {
			Time.runTask(1, () -> ui.showException(new ClassNotFoundException("无法找到类: " + finalTypeName)));
			return types.get(parentName).first();
		}), typeName, types.get(parentName), true);
		pane.add(selection.table).padBottom(4);
		pane.row();
		result.typeProv = selection::type;
		result.typeNameProv = selection::typeName;

		var table  = new LimitTable();
		var fields = new Fields(obj, selection::type, table);

		pane.table(Tex.button, t -> {
			t.add(table).fillX().pad(4).row();

			// 添加接口按钮
			try {
				t.add(new AddFieldBtn(obj, fields, () -> Classes.get(selection.typeName()))).fillX().growX();
			} catch (Exception e) {
				ui.showException(e);
			}
			/*t.row();
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
						"@none", Styles.cleart);
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
								styles.clearToggleTransi, 32, () -> {
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
			}).fillX();*/
		}).fillX().colspan(2).row();

		fields.map.each((k, v) -> {
			fields.add(null, k);
		});
	}
	private static String resolveType(MyObject<String, ?> obj, String parentName, String typeKey) {
		String typeName;
		if (obj.has(typeKey) && set.contains(parentName)) {
			typeName = "" + obj.remove(typeKey);
		} else if (types.containsKey(parentName) && types.get(parentName).get(0) != null) {
			typeName = types.get(parentName).get(0).getSimpleName();
		} else typeName = "none";
		return typeName;
	}
	private static String getTypeKey(String parentName) {
		return parentName.equals("unit") ? "template" : "type";
	}
	/** 根据文件所在文件夹，判断parentName（单数type）
	 * @see ContentVars#contentTypes */
	private String getParentName(Fi f) {
		Fi contentRoot = mod.root.child("content");

		String res = f.path().replace(contentRoot.path(), "");
		if (res.equals(f.path())) return "block";
		while (!f.parent().equals(contentRoot)) {
			f = f.parent();
		}
		return cTypeMap.get(f.name());
	}
	public void buildPro(Fi file) {
		var content = file.readString();
		var str     = content.split("\n");
		var cont    = pane.table(Styles.none).padLeft(3).get();
		class Bundle {
			public String name;
		}
		Seq<Bundle> arr = new Seq<>();
		Cons2<String, String> fun = (from, to) -> {
			var table = cont.table(Tex.button, t -> {
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
			}).get();
			var index = arr.size - 1;
			IntUI.doubleClick(table, () -> {
				ui.showConfirm("@confirm", Core.bundle.format("confirm.remove",
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
			btn.button("@add", Icon.add, () -> fun.get("", " ")).size(210, 64);
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
		Fi file = this.file;
		Cons<Func<TextAreaTable, Syntax>> textEditCons = func -> {
			MyTextArea area = pane.add(new TextAreaTable(file.readString()) {{
				syntax = func.get(this);
			}}).grow().get().getArea();
			result.value = () -> area.getText().replaceAll("\\r", "\n");
		};
		if (extension.equalsIgnoreCase("hjson")
				|| extension.equalsIgnoreCase("json")) {
			if (textEdit) textEditCons.get(JSONSyntax::new);
			else buildJson(file);
		} else if (extension.equalsIgnoreCase("properties")) {
			if (textEdit) textEditCons.get(Syntax::new);
			else buildPro(file);
		} else if (extension.equalsIgnoreCase("js")) {
			textEditCons.get(JSSyntax::new);
		} else {
			textEditCons.get(Syntax::new);
		}

		// 为了不阻挡最底下的部分
		pane.image().color(Color.clear).height(74);
	}


	public void parseJson() {
		var obj      = result.obj;
		var typeName = result.typeName();
		var type     = result.type();
		if (type != null) {
			if (UnitType.class.isAssignableFrom(type) && !obj.has("template")) {
				obj.put("template", typeName);
			} else if (Item.class.isAssignableFrom(type) ||
								 // Liquid.class.isAssignableFrom(type) ||
								 StatusEffect.class.isAssignableFrom(type) ||
								 SectorPreset.class.isAssignableFrom(type) ||
								 Planet.class.isAssignableFrom(type)) {
			} else if (!obj.has("type")) {
				obj.put("type", typeName);
			}
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
		if (extension.equalsIgnoreCase("hjson")
				|| extension.equalsIgnoreCase("json")) {
			parseJson();
		} else {
			file.writeString(result.value.get());
		}
	}
}
