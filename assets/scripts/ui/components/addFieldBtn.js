
const buildContent = require('func/buildContent');
const IntFunc = require('func/index');
const IntSettings = require("content/settings");
const findClass = require('func/findClass');
const { MyObject, MyArray } = require("func/constructor");
const { content: contentIni } = findClass("components.dataHandle");

const json = addFieldBtn.json = new Json();
const arrayClass = addFieldBtn.arrayClass = Seq.with(Seq, ObjectSet)

const caches = new Map();

function addFieldBtn(obj, Fields, prov) {
	let btn = new TextButton('$add');

	btn.add(new Image(Icon.add))
	btn.getCells().reverse()
	btn.clicked(() => {
		let cont = prov.get();
		let fields;
		if (caches.has(cont)) {
			fields = caches.get(cont)
		} else if (cont != null) {
			let m = OrderedMap(json.getFields(cont));
			if (cont == UnitType) {
				m.put('type', Label)
				m.put('controller', Label)
			}
			caches.set(cont, fields = m);
		}

		let table = new Table();
		let reg, hide;
		function eachFields() {
			table.clearChildren()

			fields.each(new Cons2({
				get: (key, meta) => {
					let field
					if (meta != Label) {
						field = meta.field
						if (!addFieldBtn.filter(field, cont)) return
					}
					let name = key
					let displayName = contentIni.get(name) || name
					if (reg != null && !reg.test(name) && !reg.test(displayName)) return

					table.table(cons(t => {
						t.button(displayName, Styles.cleart, () => {
							if (field == null) {
								Fields.add(null, name, null)
								hide.run();
								return
							}
							Fields.add(null, name, addFieldBtn.defaultValue(field.type));

							hide.run();
						}).size(Core.graphics.getWidth() * .2, 45)
							.disabled(() => obj.has(name))
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
				let name;
				p.table(cons(t => {
					t.add('$name').growX().left().row();
					t.add(name = new TextField).width(300);
				})).pad(6, 8, 6, 8).row();
				let value;
				p.table(cons(t => {
					t.add('$value').growX().left().row();
					t.add(value = new TextField).width(300);
				})).pad(6, 8, 6, 8).row();

				p.button('$ok', Styles.cleart, run(() => {
					Fields.add(null, name
						.getText(), value
							.getText());
					_hide.run()
				})).height(64).fillX();
			}

		}, cont != null)
	});
	return btn
}

addFieldBtn.filter = function (field, vType) {
	if (!IntSettings.getValue("editor", "display_deprecated") && field.isAnnotationPresent(java.lang.Deprecated)) return false;

	let type = field.type, name = field.name
	while (type.isArray() || arrayClass.contains(type)) {
		type = arrayClass.contains(type) ? buildContent.getGenericType(field)[0] : type.getComponentType()
	}
	if (/^(id|minfo|iconId|uiIcon|fullIcon|unlocked|stats|bars|timers|singleTarget|mapColor|buildCost|flags|timerDump|dumpTime|generator|capacities|region|legRegion|jointRegion|baseJointRegion|footRegion|legBaseRegion|baseRegion|cellRegion|softShadowRegion|outlineRegion|shadowRegion|heatRegion|outlineRegion|edgeRegion|overlayRegion|canHeal)$/.test(name)
		|| (type == TextureRegion && field.type.isArray())
		|| (IntFunc.toClass(Consume).isAssignableFrom(vType) && /^(update|optional|booster)$/.test(name))) return false;
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

addFieldBtn.defaultValue = function (type) {
	return type.isArray() || arrayClass.contains(type) ? new MyArray() :
		type.getSimpleName() == 'boolean' ? false :
			type.isPrimitive() ? 0 :
				type.getSimpleName() == 'String' ? '' : /* buildContent.make(type) */
					buildContent.defaultClass.containsKey(type) ? buildContent.defaultClass.get(type) : new MyObject()
}

module.exports = addFieldBtn