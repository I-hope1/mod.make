const IntFunc = require('func/index');
const IntStyles = require('scene/styles');

exports.constructor = function (type, typeName, types, other) {
	this.table = new Table(Tex.clear, cons(t => {
		t.defaults().fillX()
		t.add('$type').padRight(2);
		let button = new Button(IntStyles.clearb);
		t.add(button).size(190, 40);
		button.label(() => Core.bundle.get("type." + typeName.toLowerCase(), typeName)).center().grow().row();
		button.image().color(Color.gray).fillX();
		button.clicked(run(() => IntFunc.showSelectTable(button, (p, hide, v) => {
			p.clearChildren()
			let reg = RegExp('' + v, 'i')
			for (let k in types) {
				p.add(k, Pal.accent).growX().left().row()
				p.image().color(Pal.accent).fillX().row()

				types[k].forEach(t => {
					if (v != '' && !reg.test(t.getSimpleName())) return;
					p.button(Core.bundle.get(t.getSimpleName().toLowerCase(), t.getSimpleName()), Styles.cleart, run(() => {
						type = t
						typeName = t.getSimpleName();
						hide.run();
					})).pad(5).size(200, 65).disabled(type == t).row();
				})
			}

			if (!other) return
			p.add('other', Pal.accent).growX().left().row()
			p.image().color(Pal.accent).fillX().row()
			p.button(Core.bundle.get('none', 'none'), Styles.cleart, run(() => {
				type = null
				typeName = 'none';
				hide.run();
			})).pad(5).size(200, 65).disabled(typeName == 'none').row()
		}, true)));
	}))
	Object.defineProperty(this, 'type', { get: () => type })
	Object.defineProperty(this, 'typeName', { get: () => typeName })
}
