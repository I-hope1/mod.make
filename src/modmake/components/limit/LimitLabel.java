package modmake.components.limit;

import arc.func.Prov;
import arc.scene.ui.Label;

import static modmake.components.limit.Limit.isVisible;

public class LimitLabel extends Label {
	public LimitLabel(Prov<CharSequence> sup) {
		super(sup);
	}

	public LimitLabel(CharSequence text) {
		super(text);
	}

	public LimitLabel(CharSequence text, LabelStyle style) {
		super(text, style);
	}

	@Override
	public void updateVisibility() {
		visible = isVisible(this);
	}
}
