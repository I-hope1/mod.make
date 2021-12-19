
const IntFunc = require('func/index');
const types = require('scene/ui/dialogs/Editor').types
const add = require('scene/ui/components/addFieldBtn');
const typeSelection = require('scene/ui/components/typeSelection');
const Fields = require('scene/ui/components/Fields');
const { Object: IntObject, Array: IntArray } = require('func/constructor');
const Classes = exports.classes = Packages.mindustry.mod.ClassMap.classes;

const lang = Packages.java.lang

exports.defaultClass = ObjectMap.of(
	Effect, 'none',
	UnitType, 'mono',
	Item, 'copper',
	Liquid, 'water'
)

const effects = new Seq()
let fs = IntFunc.toClass(Fx).getFields()
for (let i = 0; i < fs.length; i++) {
	effects.add(fs[i].name)
}
exports.filterClass = ObjectMap.of(
	/* Effect, (table, value) => {},
	Attribute, (table, value) => {},
	StatusEffect, (table, value) => {}, */
	Color, (table, value) => {
		let color
		try {
			color = Color.valueOf(value);
		} catch (e) { color = new Color; }
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
		table = table.table().get()
		value = value || new IntObject();
		let typeName = value.remove('type') || 'BulletType'
		let selection = new typeSelection.constructor(Classes.get(typeName), typeName, { bullet: types.bullet });
		table.add(selection.table).padBottom(4).row()
		let cont = table.table().name('cont').get()
		let map = fObject(cont, prov(() => selection.type), value, Seq([BulletType]))
		return map
	},
	StatusEffect, (table, value) => {
		table = table.table().get()
		value = value || new IntObject()
		let cont = table.table().name('cont').get()
		let map = fObject(cont, prov(() => StatusEffect), value, Seq([StatusEffect]))
		return map
	},
	/* AmmoType, (table, value) => {},
	DrawBlock, (table, value) => {},
	Ability, (table, value) => {}, */
	Weapon, (table, value) => {
		table = table.table().get()
		value = value || new IntObject()
		let cont = table.table().name('cont').get()
		let map = fObject(cont, prov(() => Weapon), value, Seq([Weapon]))
		return map
	},
	ItemStack, (table, value) => {
		let [item, amount] =
			typeof value == 'string' ? value.split('/') :
				value instanceof IntObject ? [value.get('item'), value.get('amount')] : [Items.copper, 0]

		let items = Vars.content.items()

		// if (!items.contains(item)) throw 'Unable to convert ' + item + ' to Item.'
		if (isNaN(amount)) throw TypeError('\'' + amount + '\' isn\'t a number')
		return buildOneStack(table, 'item', items, item, amount)
	},
	// like ItemStack
	LiquidStack, (table, value) => {
		let [item, amount] = typeof value == 'string' ?
			value.split('/') : [value.get('liquid'), value.get('amount')]

		let items = Vars.content.liquids()

		// if (!items.contains(item)) throw 'Unable to convert ' + item + ' to Liquid.'
		if (isNaN(amount)) amount = 0// throw TypeError('\'' + amount + '\' isn\'t a number')
		return buildOneStack(table, 'liquid', items, item, amount)
	},
	Effect, (table, value) => {
		let val = '' + value || exports.defaultClass.get(Effect);
		let btn = table.button(val, Styles.cleart, () => {
			IntFunc.showSelectListTable(btn, effects, val, 130, 50, cons(fx => btn.setText(val = fx)), true);
		}).size(130, 45).get();
		return prov(() => val)
	},
	UnitType, (table, value) => {
		value = '' + value || exports.defaultClass.get(UnitType);
		let prov = IntFunc.selectionWithField(table, Vars.content.units(), value, 42, 32, 6, true)

		return prov
	},
	Item, (table, value) => {
		value = '' + value || exports.defaultClass.get(Item);
		let prov = IntFunc.selectionWithField(table, Vars.content.items(), value, 42, 32, 6, true)

		return prov
	},
	Liquid, (table, value) => {
		value = '' + value || exports.defaultClass.get(Liquid);
		let prov = IntFunc.selectionWithField(table, Vars.content.liquids(), value, 42, 32, 6, true)

		return prov
	},
	ObjectMap, (table, value, classes) => {
		let map = new IntObject()
		let cont = new Table(Tex.button)
		let children = new Table()
		cont.add(children).fillX().row()
		table.add(cont).fillX()
		let i = 0
		function add(k, v) {
			children.add(Fields.colorfulTable(i++, cons(t => {
				map.put(
					exports.filterClass.get(classes[0])(t, k),
					exports.filterClass.get(BulletType)(t, v)
				)
				t.table(cons(right => {
					right.button('', Icon.trash, Styles.cleart, () => {
						map.remove(k)
						if (t != null) t.remove()
					});
				})).right().growX().right();
			}))).row()
		}
		value = value || new IntObject()
		value.each(add)

		cont.button('$add', Icon.add, () => add(exports.defaultClass.get(classes[0]), new IntObject())).fillX()

		return prov(() => map)
	}
)

const category = new Seq(), unitType = Seq.withArrays('none', 'flying', 'mech', 'legs', 'naval', 'payload');
exports.filterKey = ObjectMap.of(
	'category', (table, value) => {
		if (!category.contains('' + value)) return null;
		let val = value || 'distribution';
		let btn = table.button(val, Styles.cleart, () => {
			IntFunc.showSelectListTable(btn, category, val, 130, 50, cons(cat => btn.setText(val = cat)), false);
		}).size(130, 45).get();
		return prov(() => val)
	},
	'type', (table, value, type) => {
		let val;
		if (type == UnitType) {
			val = value || 'none';
			let btn = table.button(val, Styles.cleart, () => {
				IntFunc.showSelectListTable(btn, unitType, val, 130, 50, cons(type => btn.setText(val = type)), false);
			}).size(130, 45).get();
		}

		return prov(() => val)
	},
	'consumes', (table, value) => {
		value = value || new IntObject()
		let cont = table.table(Tex.button).get()
		let content = {
			power: (t, v) => {
				let field = new TextField(v instanceof IntObject ? 0 : '' + v)
				t.add(field);
				return prov(() => field.getText())
			}, item: (t, obj) => {
				obj.put('items', fArray(t, Classes.get('ItemStack'), obj.getDefault('items', [])))
				t.row()
				t.table(cons(t => {
					t.check(Core.bundle.get('ModMake.consumes-optional', 'optional'), obj.getDefault('optional', false), boolc(b => obj.put('optional', b)))
					t.check(Core.bundle.get('ModMake.consumes-booster', 'booster'), obj.getDefault('booster', false), boolc(b => obj.put('booster', b)))
				})).row()
				return obj
			}, liquid: (t, obj) => {
				let p = exports.filterClass.get(LiquidStack)(t, obj)
				let v = p.get()
				t.row()
				t.table(cons(t => {
					t.check(Core.bundle.get('ModMake.consumes-optional', 'optional'), obj.getDefault('optional', false), boolc(b => v.put('optional', b)))
					t.check(Core.bundle.get('ModMake.consumes-booster', 'booster'), obj.getDefault('booster', false), boolc(b => v.put('booster', b)))
				})).row()
				return v
			}
		}
		function consumer(name, displayName, key, obj) {
			this.enable = obj != null
			obj = obj || new IntObject()
			this.name = name
			let table = new Table()
			let t = this.table = new Table()

			cont.check(displayName, this.enable, boolc(b => this.setup(b))).row()
			cont.add(table).row()
			value.put(key, content[name](t, obj));
			cont.row()
			this.setup = function (b) {
				if (this.enable = b) {
					table.add(this.table)
				} else this.table.remove()
			}
			this.setup(this.enable)
		}
		let power = new consumer('power', 'power', 'power', value.get('power'));
		let item = new consumer('item', 'items', 'items', value.get('items'));
		let liquid = new consumer('liquid', 'liquids', 'liquid', value.get('liquid'));

		return prov(() => {
			if (!power.enable) value.remove('power')
			if (!item.enable) value.remove('items')
			if (!liquid.enable) value.remove('liquid')
			return value + ''
		})
	}
)

exports.load = function () {
	for (let cat of Category.all) category.add('' + cat)
}

exports.make = function (type) {
	try {
		let cons = Seq([type]).get(0).getDeclaredConstructor();
		cons.setAccessible(true);
		return cons.newInstance();
	} catch (e) {
		Vars.ui.showErrorMessage(e);
	}
}

function fObject(t, type, value, typeBlackList) {
	let table = new Table(Tex.button), children = new Table,
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
	table.add(add.constructor(value, fields, type)).fillX().growX()
	return prov(() => {
		if (!typeBlackList.contains(type.get())) value.put('type', type.get().getSimpleName())
		return value
	})
}

function addItem(type, fields, i, value) {
	let t = new Table;
	exports.build(type, fields, t, i, value, true)
	fields.add(t, i)
}
function fArray(t, vType, v) {
	let table = new Table, children = new Table,
		fields = new Fields.constructor(v || new IntArray(), prov(() => vType), children)
	children.center().defaults().center().minWidth(100)
	table.add(children).name('cont').row()
	t.add(table)
	v = fields.map
	let len = v.length
	for (var j = 0; j < len; j++) {
		addItem(vType, fields, j, v[j])
	}
	table.button('$add', () => {
		addItem(vType, fields, v.length, exports.make(vType))
	}).fillX()
	return prov(() => v)
}

let cont = Packages.mindustry.ctype.UnlockableContent
function buildOneStack(t, type, stack, content, amount) {
	let output = new IntObject();

	t.add('$' + type);

	content = content || stack.get(0)
	output.put(type, IntFunc.selectionWithField(t, stack, content instanceof cont ? content.name : '' + content, 42, 32, 6, true))

	t.add('$amount');
	let atf = t.field('' + (amount | 0), cons(t => { })).get();
	output.put('amount', prov(() => atf.getText() | 0));

	return prov(() => output);
}

let _class = Seq([Seq]).get(0);
exports.getGenericType = function (field) {
	return ('class ' + field.getGenericType())
		.replace(field.type, '').replace(/\<(.+?)\>/, '$1')
		.split(/\,\s*/).map(str => _class.forName(str, false, _class.getClassLoader()))
}

const lstr = lang.String
/* 构建table */
exports.build = function (type, fields, t, k, v, isArray) {
	function fail() {
		let field = new TextField('' + v)
		t.add(field);
		return prov(() => field.getText().replace(/\s*/, '') != '' ? field.getText() : '""')
	}
	let map = fields.map

	if (!isArray) t.add(Core.bundle.get('content.' + k, k) + ':').fillX().left().padLeft(2).padRight(6);

	let output = (() => {

		if (type == null) return

		try {
			let field = isArray ? null : type.getField(k)
			let vType = isArray ? type : field.type
			if (vType == null || vType == lstr) {
				return
			}
			if (vType.isPrimitive()) {
				if (vType + '' == 'boolean') {
					let btn = t.button('' + v, Styles.cleart, () => btn.setText('' + (v = !v))).size(130, 45).get()
					return prov(() => v)
				}
				return
			}

			if ((vType.isArray() || vType == Seq) && v instanceof IntArray) {
				return fArray(t, vType == Seq ? this.getGenericType(field)[0] : vType.getComponentType(), v)
			}
			if (IntFunc.toClass(ObjectMap).isAssignableFrom(vType)) {
				let classes = this.getGenericType(field)
				return this.filterClass.get(vType)(t, v, classes)
			}
			if (this.filterClass.containsKey(vType)) {
				return this.filterClass.get(vType)(t, v)
			}
		} catch (e) {
			Log.info(type)
			Log.err(e)
		}
		finally {

			if (this.filterKey.containsKey(k)) {
				return this.filterKey.get(k)(t, v)
			}
		}
		return

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
		right.button('', Icon.trash, Styles.cleart, () => fields.remove(t, k));
	})).right().growX().right();
}

