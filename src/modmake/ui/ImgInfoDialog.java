package modmake.ui;

import arc.scene.ui.TextField;
import arc.struct.ObjectMap;
import arc.util.Log;
import mindustry.ui.dialogs.BaseDialog;
import mindustry.ui.dialogs.CustomRulesDialog;

import static modmake.IntUI.imgEditor;

public class ImgInfoDialog extends BaseDialog {
	private final CustomRulesDialog ruleInfo = new CustomRulesDialog();

	public ImgInfoDialog() {
		super("图像信息");

		addCloseButton();

		shown(this::setup);
	}

	private void setup() {
		cont.clear();

		ObjectMap<String, String> tags = imgEditor.tags;

		cont.pane(t -> {
			t.add("@filename").padRight(8).left();
			t.defaults().padTop(15);

			TextField name = t.field(tags.get("name", ""), text -> {
				tags.put("name", text);
			}).size(400, 55f).maxTextLength(50).get();
			name.setMessageText("@unknown");

			t.row();

			name.change();

			t.margin(16f);
		});
	}
}
