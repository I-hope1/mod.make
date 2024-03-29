package modmake.components;

import arc.Core;
import arc.func.Cons;
import arc.graphics.Color;
import arc.scene.ui.Button;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import mindustry.gen.Tex;
import mindustry.graphics.Pal;
import mindustry.ui.Styles;
import modmake.IntUI;
import modmake.ui.MyStyles;
import modmake.util.tools.Tools;

import java.util.*;

/** type选择 */
public class TypeSelection {
	public Table table;
	Class<?> type;
	String   typeName;
	public Cons<Class<?>> switchType;

	public TypeSelection(Class<?> type, String typeName, Seq<Class<?>> types) {
		this(type, typeName, types, false);
	}

	public TypeSelection(Class<?> _type, String _typeName, Seq<Class<?>> types, boolean other) {
		var typesIni = DataHandle.types;
		this.type = _type;
		this.typeName = _typeName;
		this.table = new Table(Tex.clear, t -> {
			t.defaults().fillX();
			t.add("@type").padRight(4);
			var button = new Button(MyStyles.clearb);
			t.add(button).height(40);
			button.table(l -> {
				l.label(() -> typesIni.get(typeName, () -> typeName));
			}).padLeft(4).padRight(4).grow().row();
			button.image().color(Color.gray).fillX();
			button.clicked(() -> IntUI.showSelectTable(button, (p, hide, v) -> {
				p.clearChildren();
				var reg = Tools.compileRegExp("" + v);

				types.each(t1 -> {
					if (!v.isEmpty() && !Tools.testP(reg, t1.getSimpleName())
							&& !Tools.testP(reg, typesIni.get(t1.getSimpleName())))
						return;
					p.button(typesIni.get(t1.getSimpleName(), t1::getSimpleName),
					 Styles.cleart, () -> {
						 type = t1;
						 typeName = t1.getSimpleName();
						 hide.run();
						 if (switchType != null) switchType.get(t1);
					 }).pad(5).size(200, 65).disabled(type == t1).row();
				});

				if (!other) return;
				p.add("other", Pal.accent).growX().left().row();
				p.image().color(Pal.accent).fillX().row();
				p.button(Core.bundle.get("none", "none"), Styles.cleart, () -> {
					type = null;
					typeName = "none";
					hide.run();
				}).pad(5).size(200, 65).disabled(Objects.equals(typeName, "none")).row();
			}, true));
		});
	}

	public Class<?> type() {
		return type;
	}

	public String typeName() {
		return typeName;
	}

}
