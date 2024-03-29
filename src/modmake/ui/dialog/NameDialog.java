package modmake.ui.dialog;

import arc.func.Cons;
import arc.input.KeyCode;
import arc.scene.event.*;
import arc.scene.ui.TextField;
import modmake.components.Window;

public class NameDialog extends Window {
	TextField    namef = new TextField();
	Cons<String> okCons;

	{
		cont.table(t -> {
			t.add("@name");
			t.add(namef).growX();
		}).growX().row();

		buttons.button("@back", this::hide).size(150, 64);
		buttons.button("@ok", () -> {
			okCons.get(namef.getText());
			hide();
		}).size(150, 64).disabled(__ -> !namef.isValid());
		// closeOnBack();
	}

	public NameDialog() {
		super("", 120, 80, true, false);
	}

	public void show(Cons<String> okCons, TextField.TextFieldValidator valid, String text) {
		this.okCons = okCons;
		if (valid != null) namef.setValidator(valid);
		namef.setText(text);
		namef.setMessageText("请输入");
		namef.addListener(new InputListener() {
			@Override
			public boolean keyUp(InputEvent event, KeyCode keycode) {
				if (keycode == KeyCode.enter) {
					okCons.get(namef.getText());
					hide();
				}
				return false;
			}
		});
		show();
	}

	public void show(Cons<String> okCons, TextField.TextFieldValidator valid) {
		show(okCons, valid, "");
	}
}
