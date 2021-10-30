
const IntFunc = require('func/index');
const add = require('scene/ui/components/addFieldBtn');
const Fields = require('scene/ui/components/Fields');
const Classes = exports.classes = Packages.mindustry.mod.ClassMap.classes;

const lang = Packages.java.lang

exports.filterClass = ObjectMap.of(
	/* Effect, (table, value) => {},
	Attribute, (table, value) => {},
	StatusEffect, (table, value) => {}, */
	Color, (table, value) => {
		let color
		try {
			color = Color.valueOf(value);
		} catch (e) { return null; }
		let button = new Button;

		let image = button.image().size(30).color(color);
		let field = button.add('' + color).get();
		/* 使用原本自带的采色器 */
		button.clicked(() => Vars.ui.picker.show(color, cons(c => {
			image.color(color = c);
			field.setText(c + '');
		})));

		table.add(button);

		return prov(() => '"' + color + '"')
	},
	BulletType, (table, value) => {
		let map = fObject(table, Classes.get(value.type) || Classes.get('BulletType'), value)
		return map
	},
	/* AmmoType, (table, value) => {},
	DrawBlock, (table, value) => {},
	Ability, (table, value) => {},
	Weapon, (table, value) => {}, */
	ItemStack, (table, value) => {
		let [item, amount] = typeof value == 'string' ?
			value.split('/') : [value.item, value.amount],
			items = Vars.content.items().toArray()

		// if (!items.contains(item)) throw 'Unable to convert ' + item + ' to Item.'
		if (isNaN(amount)) throw 'Unable to convert \'' + amount + '\' to Number';
		let output = IntFunc.buildOneStack(table, 'item', items, item, amount)
		return prov(() => '{item:' + output[0].get() + ', amount:' + output[1].get() + '}');
	},
	// like ItemStack
	LiquidStack, (table, value) => {
		let [item, amount] = typeof value == 'string' ?
			value.split('/') : [value.liquid, value.amount],
			items = Vars.content.liquids().toArray()

		// if (!items.contains(item)) throw 'Unable to convert ' + item + ' to Liquid.'
		if (isNaN(amount)) throw 'Unable to convert ' + amount + ' to Number';
		let output = IntFunc.buildOneStack(table, 'liquid', items, item, amount)
		return prov(() => '{liquid:' + output[0].get() + ', amount:' + output[1].get() + '}');
	},
	UnitType, (table, value) => {
		value = '' + value || 'mono';
		let prov = IntFunc.selectionWidhField(table, Vars.content.units().toArray(), value, 40, 32, func(i => text = i.name), 6, true)

		return prov
	},
	Item, (table, value) => {
		value = '' + value || 'copper';
		let prov = IntFunc.selectionWidhField(table, Vars.content.items().toArray(), value, 40, 32, func(i => text = i.name), 6, true)

		return prov
	},
	Liquid, (table, value) => {
		value = '' + value || 'water';
		let prov = IntFunc.selectionWidhField(table, Vars.content.liquids().toArray(), value, 40, 32, func(i => text = i.name), 6, true)

		return prov
	}
)

const category = [], unitType = ['none', 'flying', 'mech', 'legs', 'naval', 'payload'];
exports.filterKey = ObjectMap.of(
	'category', (table, value) => {
		if (!category.includes('' + value)) return null;
		let val = value || 'distribution';
		let btn = table.button(val, Styles.cleart, run(() => {
			IntFunc.showSelectListTable(btn, category, val, 130, 50, cons(cat => btn.setText(val = cat)), false);
		})).size(130, 45).get();
		return prov(() => val)
	},
	'type', (table, value, type) => {
		let val;
		if (type == UnitType) {
			val = value || 'none';
			let btn = table.button(val, Styles.cleart, run(() => {
				IntFunc.showSelectListTable(btn, unitType, val, 130, 50, cons(type => btn.setText(val = type)), false);
			})).size(130, 45).get();
		}

		return prov(() => val)
	}
)

exports.load = function () {
	for (let cat of Category.all) category.push('' + cat)
}

exports.make = function (type) {
	try {
		let cons = type.getDeclaredConstructor();
		cons.setAccessible(true);
		return cons.newInstance();
	} catch (e) {
		Vars.ui.showErrorMessage(e);
	}
}
exports.parse = (new JsonReader).parse

function fObject(t, type, value) {
	let table = new Table, children = new Table,
		fields = new Fields.constructor(value, type, children);
	value = fields.map
	children.center().defaults().center().minWidth(100)
	table.add(children).row()
	t.add(table)
	value.each((k, v) => {
		if (!(value[k] instanceof Function))
		/* try {
			if (add.filter(type.getField(k)))  */fields.add(null, k)/* ;
		} catch(e) { continue } */
	})
	table.add(add.constructor(value, fields, prov(() => type))).fillX().growX()
	return prov(() => '{\n' + value + '\n}')
}

function addItem(type, fields, array, i, value) {
	let t = new Table;
	exports.build(type, array, [t, i, value || array[i], fields, i], true)
	fields.add(t, i)
}
function fArray(t, vType, v) {
	let table = new Table, children = new Table,
		cType = vType,
		fields = new Fields.constructor(v, vType, children)
	children.center().defaults().center().minWidth(100)
	table.add(children).row()
	t.add(table)
	for (let i = 0; i < v.length; i++) {
		addItem(cType, fields, v, i)
	}
	table.button('$add', () => {
		addItem(cType, fields, v, v.length, exports.make(cType))
	}).fillX()
	return prov(() => v)
}

exports.getGenericType = function (field) {
	return ('class ' + field.getGenericType())
		.replace(field.type, '').replace(/<(.+?)>/, '$1')
		.split(/\,\s+/).map(str => Classes.get(str))
}

const lstr = lang.String
/* 构建table */
exports.build = function (type, map, arr, isArray) {
	function fail() {
		let field = new TextField('' + v)
		t.add(field);
		return prov(() => field.getText().replace(/\s*/, '') != '' ? field.getText() : '""')
	}
	let [t, k, v, fields, i] = arr;

	if (!isArray) t.left().add(Core.bundle.get('content.' + k, k) + ':').fillX().left().padLeft(2).padRight(6);

	let output = (() => {

		if (type == null) return

		try {
			let field = isArray ? null : type.getField(k)
			let vType = isArray ? type : field.type
			if (vType == null || vType.isPrimitive() || vType == lstr) return;

			if ((vType.isArray() || vType == Seq) && v instanceof Array) {
				return fArray(t, vType == Seq ? this.getGenericType(vType)[0] : vType.getComponentType(), v)
			}
			else if (false/* vType instanceof ObjectMap */) {
				let classes = this.getGenericType(field)
				return
			}
			else if (this.filterClass.containsKey(vType)) {
				return this.filterClass.get(vType)(t, v, type)
			}
		} catch (e) {
			if (k != 'type') return
		}

		if (this.filterKey.containsKey(k)) {
			return this.filterKey.get(k)(t, v, type)
		}

		else if (v instanceof Boolean) {
			let label = new Label('' + v)
			t.button(cons(t => t.add(label)), IntStyles.clearb, () => label.setText(v = !v)).size(130, 45)
			return prov(() => v)
		}


		/* else if (k == 'ammoTypes') {
			v = v.toArray()
	
			let contents = Vars.content[type == 'LiquidTurret' ? 'liquids' : 'items']().toArray();
			let btn = t.button('$add', () => IntFunc.showSelectImageTable(
				btn, contents, null, 40, 32, cons(item => {
				v[item.name] = {}
			}), 6, true)).get();
	
			map.put(k, {
				toString() {
					t.clear();
					t.remove();
					return JSON.stringify(v)
				}
			})
		} */

		else if (k == 'consumes') {
			v = v == null || v instanceof Consumers ? {
				items: {
					items: []
				},
				liquid: {}
			} : v;

			let liquidEnab = true;
			if (v.liquid == null) {
				v.liquid = {}
				liquidEnab = false;
			}

			t.row();
			t.table(cons(t => {
				t.left().add(Core.bundle.get('item') + ': {').row();
			})).left().row();

			let stack = v.items.items.map(i => typeof i == 'string' ? i.split('/') : [i.item, i.amount]);
			let output = IntFunc.buildMultipleStack(Styles.black5, 'item', Vars.content.items().toArray(), stack, t);

			t.add('}').fillX().left().row();

			t.table(cons(t => {
				t.left().add(Core.bundle.get('liquid') + ': {').row();
			})).left().row();

			let liquidStack;
			t.table(cons(t => {
				t.left();
				t.check('$mod.enabled', liquidEnab, boolc(b => liquidEnab = b)).row();
				t.update(() => {
					if (!liquidEnab) return;
					if (stack[0] != '') {
						v.liquid.liquid = stack[0];
						v.liquid.amount = stack[1];
					} else v.liquid = {};
				});

				t.table(
					cons(t => liquidStack = IntFunc.buildOneStack(t, 'liquid',
						Vars.content.liquids().toArray(), v.liquid.liquid || 'water', v.liquid.amount || 0))
				).visible(boolp(() => liquidEnab));
			})).fillX().left().row();

			t.add('}').fillX().left().row();
			return prov(() => {
				t.clear();
				t.remove();
				v.items.items = output.map(e => e[0] + '/' + e[1]);
				if (!liquidEnab) {
					delete v.liquid;
				} else if (liquidStack[0].get() != null && liquidStack[1].get() != 0) {
					v.liquid = {
						liquid: liquidStack[0].get(),
						amount: liquidStack[1].get()
					}
				}
				return JSON.stringify(v)
			})
		}

	})();
	if (output == null) output = fail()
	map.put(k, output)


	t.table(cons(right => {
		right.right().defaults().right()
		if (!isArray && Core.bundle.has(type.getSimpleName() + '_' + k + '.help')) {
			let btn = right.button('?', () => IntFunc.showSelectTable(btn, (p, hide) => {
				p.pane(p => p.add(Core.bundle.get(type.getSimpleName() + '_' + k + '.help'))).width(400)
				p.button('ok', hide).fillX()
			}, false)).padLeft(2);
		}
		right.button('', Icon.trash, Styles.cleart, run(() => fields.remove(i, k)));
	})).right().growX().right();
}

