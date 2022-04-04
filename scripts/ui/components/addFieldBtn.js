
const buildContent = require('func/buildContent');
const IntFunc = require('func/index');
const IntSettings = require("content/settings");
const { MyObject, MyArray } = require('func/constructor');
const { caches: { content: contentIni } } = require('func/IniHandle')

const json = exports.json = new Json();

exports.filter = function (field) {
	if (!IntSettings.getValue("base", "display_deprecated") && field.isAnnotationPresent(java.lang.Deprecated)) return false;

	let type = field.type, name = field.name
	while (type.isArray() || type == Seq) {
		type = type == Seq ? buildContent.getGenericType(field)[0] : type.getComponentType()
	}
	if (/^(id|minfo|iconId|uiIcon|fullIcon|unlocked|stats|bars|timers|singleTarget|mapColor|buildCost|flags|timerDump|dumpTime)$/.test(name) | /region/i.test(name)) return false;
	if (type.isPrimitive() || type == java.lang.String) return true;
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


exports.constructor = function (obj, Fields, prov) {
	let btn = new TextButton('$add');
	btn.add(new Image(Icon.add))
	btn.getCells().reverse()
	btn.clicked(() => {
		let cont = prov.get(), fields = json.getFields(cont);

		let table = new Table();
		let reg, hide;
		function eachFields() {
			table.clearChildren()
			if (cont == UnitType) table.button('type', Styles.cleart, run(() => {
				Fields.add(null, 'type', 'none');

				hide.run();
			})).size(Core.graphics.getWidth() * .2, 45)
				.disabled(obj.has('type')).row();

			fields.each(new Cons2({
				get: (key, meta) => {
					let field = meta.field
					if (!exports.filter(field)) return
					let name = key
					let displayName = contentIni.get(name) || name
					if (reg != null && !reg.test(name) && !reg.test(displayName)) return
					table.table(cons(t => {
						t.button(displayName, Styles.cleart, run(() => {
							let type = field.type
							Fields.add(null, name,
								type.isArray() || type == Seq ? new MyArray() :
									type.getSimpleName() == 'boolean' ? false :
										type.isPrimitive() ? 0 :
											type.getSimpleName() == 'String' ? '' : /* buildContent.make(type) */
												buildContent.defaultClass.containsKey(type) ? buildContent.defaultClass.get(type) : new MyObject()
							);

							hide.run();
						})).size(Core.graphics.getWidth() * .2, 45)
							.disabled(obj.has(name))
						let help = contentIni.get(name + '.help')
						if (help != null) {
							let btn = t.button('?', Styles.clearPartialt, () => IntFunc.showSelectTable(btn, (p, hide) => {
								p.pane(p => p.add(help, 1.3)).pad(4, 8, 4, 8).row()
							}, false)).size(8 * 5).padLeft(5).padRight(5).right().grow().get();
						}
					})).row();
				}
			}));
			if (table.children.size == 0) {
				table.table(cons(t => t.add('$none'))).size(Core.graphics.getWidth() * .2, 45)

			}
		}

		IntFunc.showSelectTable(btn, (p, _hide, v) => {
			p.clearChildren()
			hide = _hide
			if (cont != null) {
				try {
					reg = RegExp(v, 'i');
				} catch (e) { reg = null };
				eachFields()
				p.add(table)
			} else {
				p.left().top().defaults().left().top();
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
			}

		}, cont != null)
	});
	return btn
}