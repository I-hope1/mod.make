package modmake.ui.dialog;

import arc.Core;
import arc.files.Fi;
import arc.scene.ui.TextField;
import arc.scene.ui.layout.Table;
import arc.struct.*;
import arc.util.serialization.JsonValue;
import mindustry.Vars;
import mindustry.core.Version;
import mindustry.gen.Icon;
import modmake.IntUI;
import modmake.components.Window;
import modmake.util.Tools;

import java.util.StringJoiner;

import static modmake.IntUI.modsDialog;
import static modmake.components.DataHandle.*;
import static modmake.util.BuildContent.*;
import static rhino.ScriptRuntime.toNumber;

public class ModMetaDialog extends Window {
	int      __MinGameVersion__ = 136;
	Fi       modsDirectory      = dataDirectory.child("mods");
	String[] arr                = {"name", "displayName", "description", "author", "version", "main", "repo"};
	String   lastFiName;

	public boolean isNaN(String str) {
		try {
			Double.parseDouble(str);
			return false;
		} catch (Exception e) {
			return true;
		}
	}

	public ModMetaDialog() {
		super("");
		noButtons(false);
	}

	public void write(Fi modRoot) {
		if (!isNull) {
			file.parent().moveTo(modRoot);
		}
		StringJoiner str = new StringJoiner("\n");
		str.add("minGameVersion: " + Fields.get("minGameVersion").getText());

		for (String item : arr) {
			String text = Tools.trope(unpackString(Fields.get(item).getText()));
			if (!text.isEmpty()) str.add(item + ": "
																	 + (Tools.isNum(text) ? text : "\"" + text + "\""));
		}
		modRoot.child(isNull ? "mod.json" : "mod." + file.extension()).writeString("" + str);
		modsDialog.setup();

		hide();
	}

	ObjectMap<String, TextField> Fields = new ObjectMap<>();

	TextField MyTextField() {
		var field = new TextField("");
		if (Vars.mobile) field.removeInputDialog();
		return field;
	}

	boolean   isNull;
	JsonValue jsonValue;
	Fi        file;
	Table     container;

	String errorText = "";
	public void load() {
		cont.pane(p -> container = p).growY();
		Seq<TextField> FieldArray = new Seq<>();

		buttons.table(err -> {
			err.label(() -> "[red]" + errorText).get();
		}).row();
		buttons.table(b -> {
			b.button("@back", Icon.left, this::hide).size(210, 64);
			b.button("@ok", Icon.ok, () -> {
				Fi mod = modsDirectory.child(Fields.get("fileName").getText());
				if (!mod.name().equals(lastFiName) && mod.path().equals(file.parent().path()) && mod.exists()) {
					Vars.ui.showConfirm("@overwrite", "@confirm.rename", () -> {
						mod.deleteDirectory();
						write(mod);
					});
				} else {
					write(mod);
				}
			}).size(210, 64).disabled(__ -> {
				for (TextField f : FieldArray) {
					if (!f.isValid()) return true;
				}
				errorText = "";
				return false;
			});
		});

		container.add("@mod.fileName");
		TextField fileName = MyTextField();
		Fields.put("fileName", fileName);
		container.add(fileName).valid(text -> {
			boolean valid = true;
			if (text.replaceAll("\\s", "").equals("")) {
				errorText = "文件名不能为空";
			} else if (text.equals("tmp")) {
				errorText = "文件名不能为\"tmp\"";
			} else valid = false;
			return !valid;
		}).row();
		FieldArray.add(fileName);

		container.add("@minGameVersion");
		TextField minGameVersion = MyTextField();
		Fields.put("minGameVersion", minGameVersion);
		container.add(minGameVersion).valid(text -> {
			boolean valid = true;
			if (isNaN(text)) {
				errorText = Core.bundle.format("error.number.unvalid", "minGameVersion");
				return true;
			}
			double num = toNumber(text);
			if (num < 105) {
				errorText = "\"最小游戏版本\"不能小于105";
			} else if (num > Version.build) {
				errorText = "\"最小游戏版本\"不能大于 " + Version.build;
			} else {
				valid = false;
			}

			return !valid;
		}).row();
		FieldArray.add(minGameVersion);


		for (var str : arr) {
			container.add(Core.bundle.get(str, str));
			TextField field = MyTextField();
			IntUI.longPress(field, 600, longPress -> {
				if (longPress) IntUI.showTextArea(field);
			});
			Fields.put(str, field);
			container.add(field).row();
		}

		hidden(() -> {
			modsDialog.show();
			modsDirectory.child("tmp").deleteDirectory();
		});
		// closeOnBack();
	}


	public void show(Fi f) {
		modsDirectory.child("tmp").deleteDirectory();
		file = f;
		lastFiName = f.parent().name();
		isNull = !f.exists();
		if (isNull) file.writeString("");
		jsonValue = hjsonParse(file.readString());
		title.setText(isNull ? "@mod.create" : "@edit");

		Fields.get("fileName").setText(isNull ? "" : f.parent().name());
		int num = jsonValue.getInt("minGameVersion", __MinGameVersion__);
		Fields.get("minGameVersion").setText("" + num);

		for (String item : arr) {
			Fields.get(item).setText(packString(jsonValue.getString(item, "")));
		}

		show();
	}

}
