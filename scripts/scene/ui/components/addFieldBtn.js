
const buildContent = require('func/buildContent');
const IntFunc = require('func/index');
const IntCons = require('func/constructor');
const Modifier = Packages.java.lang.reflect.Modifier;

exports.filter = function (field) {
	if (!Modifier.isPublic(field.getModifiers()) || field.name == 'id' || /(i|I)con/.test(field.name)) return false;
	let type = field.type, name = field.name
	while (type.isArray() || type == Seq) {
		type = type == Seq ? buildContent.getGenericType(field)[0] : type.getComponentType()
	}
	if (type.isPrimitive() || type == lstr) return true;
	// 使用throw跳出循环
	try {
		buildContent.filterClass.each(new Cons2({
			get: (k, v) => {
				if (k.isAssignableFrom(type)) throw ''
			}
		}))
		buildContent.filterKey.each(new Cons2({
			get: (k, v) => {
				if (k == name) throw ''
			}
		}))
	} catch (e) { return true }
	return false
}

let lstr = Packages.java.lang.String
exports.constructor = function (obj, Fields, prov) {
	let btn = new TextButton('$add');
	btn.add(new Image(Icon.add))
	btn.getCells().reverse()
	btn.clicked(() => {
		let content = cont = prov.get(), fields;

		let table = new Table;
		let reg, hide;
		function eachFields() {
			table.clearChildren()
			if (cont == UnitType) table.button('type', Styles.cleart, run(() => {
				Fields.add(null, 'type', 'none');

				hide.run();
			})).size(Core.graphics.getWidth() * .2, 45)
				.disabled(obj.get('type') != null).row();
			for (let i = 0; i < fields.length; i++) {
				let field = fields[i]
				if (!exports.filter(field))
					continue
				let name = field.getName()
				if (reg != null && !reg.test(name)) continue
				table.button(name, Styles.cleart, run(() => {
					let type = field.type
					Fields.add(null, name,
						type.isArray() || type == Seq ? new IntCons.Array() :
							/^(int|double|float|long|short|byte|char)$/.test(type.getSimpleName()) ? 0 :
						type.getSimpleName() == 'boolean' ? false :
						type.getSimpleName() == 'String' ? '' : /* buildContent.make(type) */
						buildContent.defaultClass.containsKey(type) ? buildContent.defaultClass.get(type) : new IntCons.Object()
					);

					hide.run();
				})).size(Core.graphics.getWidth() * .2, 45)
					.disabled(obj.get(name) != null).row();
			}
			if (table.children.size == 0) {
				table.table(cons(t => t.add('$none'))).size(Core.graphics.getWidth() * .2, 45)
			}
		}

		let load = true, all = false, reload;
		IntFunc.showSelectTable(btn, reload = (p, _hide, v) => {
			hide = _hide
			if (!load) {
				try {
					reg = RegExp(v, 'i');
				} catch (e) { reg = null };
				eachFields(fields)
				return;
			}
			load = false, all = false
			if (cont == null) {
				p.left().top().defaults().left().top();
				p.clearChildren()
				let name = p.table(cons(t =>
					t.add('$name').growX().left().row()
				)).get().add(new TextField)
					.width(300).get();
				p.row();
				let value = p.table(cons(t => t.add(
					'$value').growX().left()
					.row())).get().add(
						new TextField)
					.width(300).get();
				p.row();
				p.button('$ok', Styles.cleart, run(() => {
					this.Fields.add(null, name
						.getText(), value
							.getText());
					_hide.run()
				})).fillX();
				return;
			}

			cont = content
			p.clearChildren()
			p.button('reload', Styles.cleart, () => reload(p, _hide, v, load = true)).size(Core.graphics.getWidth() * .2, 45).row()
			p.button('获取所有信息', Styles.cleart, () => {
				all = true
				eachFields(fields = prov.get().getFields())
			}).size(Core.graphics.getWidth() * .2, 45).disabled(boolf(() => all)).row()

			p.button('获取超类信息', Styles.cleart, () => {
				all = false;
				eachFields(fields = (cont = cont.getSuperclass()).getDeclaredFields())
			}).size(Core.graphics.getWidth() * .2, 45).disabled(boolf(() => cont.getSuperclass() == Packages.java.lang.Object || all)).row()
			p.image().color(Color.gray).pad(6).fillX().row()
			p.add(table).padBottom(4).fillX()
			eachFields(fields = cont.getDeclaredFields())
		}, cont != null)
	});
	return btn
}