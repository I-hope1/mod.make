const IntFunc = require('func/index');
const findClass = require('func/findClass')
const IntStyles = findClass('ui.styles');
const { types: typesIni } = findClass("components.dataHandle");

module.exports = function (type, typeName, types, other) {
	this.table = new Table(Tex.clear, cons(t => {
		t.defaults().fillX()
		t.add('$type').padRight(4);
		let button = new Button(IntStyles.clearb);
		t.add(button).height(40);
		button.table(cons(l => {
			l.label(() => typesIni.get(typeName) || typeName)
		})).padLeft(4).padRight(4).grow().row();
		button.image().color(Color.gray).fillX();
		button.clicked(() => IntFunc.showSelectTable(button, (p, hide, v) => {
			p.clearChildren()
			let reg = RegExp('' + v, 'i')

			types.forEach(t => {
				if (v != '' && !reg.test(t.getSimpleName()) && !reg.test(typesIni.get(t.getSimpleName()) || '')) return;
				p.button(typesIni.get(t.getSimpleName()) || t.getSimpleName(), Styles.cleart, run(() => {
					type = t
					typeName = t.getSimpleName();
					hide.run();
				})).pad(5).size(200, 65).disabled(type == t).row();
			})

			if (!other) return
			p.add('other', Pal.accent).growX().left().row()
			p.image().color(Pal.accent).fillX().row()
			p.button(Core.bundle.get('none', 'none'), Styles.cleart, run(() => {
				type = null
				typeName = 'none';
				hide.run();
			})).pad(5).size(200, 65).disabled(typeName == 'none').row()
		}, true));
	}))
	Object.defineProperty(this, 'type', { get: () => type })
	Object.defineProperty(this, 'typeName', { get: () => typeName })
}
