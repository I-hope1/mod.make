
const IntFunc = require('func/index');
const types = require('scene/ui/dialogs/Editor').types
const add = require('scene/ui/components/addFieldBtn');
const typeSelection = require('scene/ui/components/typeSelection');
const Fields = require('scene/ui/components/Fields');
const IntObject = require('func/constructor').Object;
const IntArray = require('func/constructor').Array;
const Classes = exports.classes = Packages.mindustry.mod.ClassMap.classes;

const lang = Packages.java.lang

exports.defaultClass = ObjectMap.of(
	UnitType, 'mono',
	Item, 'copper',
	Liquid, 'water'
)

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
			value instanceof IntObject ? [value.get('item'), value.get('amount')] : [Items.copper, 0];

		let items = Vars.content.items().toArray()

		// if (!items.contains(item)) throw 'Unable to convert ' + item + ' to Item.'
		if (isNaN(amount)) throw TypeError('\'' + amount + '\' isn\'t a number')
		let output = buildOneStack(table, 'item', items, item, amount)
		return prov(() => '{item:' + output[0].get() + ', amount:' + output[1].get() + '}');
	},
	// like ItemStack
	LiquidStack, (table, value) => {
		let [item, amount] = typeof value == 'string' ?
			value.split('/') : [value.get('liquid'), value.get('amount')],
			items = Vars.content.liquids().toArray()

		// if (!items.contains(item)) throw 'Unable to convert ' + item + ' to Liquid.'
		if (isNaN(amount)) amount = 0// throw TypeError('\'' + amount + '\' isn\'t a number')
		let output = buildOneStack(table, 'liquid', items, item, amount)
		return prov(() => '{liquid:' + output[0].get() + ', amount:' + output[1].get() + '}');
	},
	UnitType, (table, value) => {
		value = '' + value || exports.defaultClass.get(UnitType);
		let prov = IntFunc.selectionWithField(table, Vars.content.units().toArray(), value, 42, 32, 6, true)

		return prov
	},
	Item, (table, value) => {
		value = '' + value || exports.defaultClass.get(Item);
		let prov = IntFunc.selectionWithField(table, Vars.content.items().toArray(), value, 42, 32, 6, true)

		return prov
	},
	Liquid, (table, value) => {
		value = '' + value || exports.defaultClass.get(Liquid);
		let prov = IntFunc.selectionWithField(table, Vars.content.liquids().toArray(), value, 42, 32, 6, true)

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

const category = [], unitType = ['none', 'flying', 'mech', 'legs', 'naval', 'payload'];
exports.filterKey = ObjectMap.of(
	'category', (table, value) => {
		if (!category.includes('' + value)) return null;
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
		let tables = {item: new Table, liquid: new Table}
		let enable = {item:true, liquid:true}
		let itemsetup = b => {
			if (enable.item = b) {
				items.add(tables.item)
			} else tables.item.remove()
		}
		let items = new Table()
		itemsetup(value.get('items') != null)
		cont.check('items', enable.item, boolc(b => itemsetup(b))).row()
		cont.add(items).row()
		value.put('items', fArray(tables.item, IntFunc.toClass(ItemStack), value.getDefault('items', [])));
		cont.row()

		let liquidsetup = b => {
			if (enable.liquid = b) {
				liquids.add(tables.liquid)
			} else tables.liquid.remove()
		}
		let liquids = new Table()
		liquidsetup(value.get('liquid') != null)
		cont.check('liquid', enable.liquid, boolc(b => liquidsetup(b))).row()
		cont.add(liquids).row()
		value.put('liquid', exports.filterClass.get(LiquidStack)(tables.liquid, value.getDefault('liquid', 'water/0')))
		
		return prov(() => {
			if (!enable.item) value.remove('items')
			if (!enable.liquid) value.remove('liquid')
			return value + ''
		})
	}
)

exports.load = function () {
	for (let cat of Category.all) category.push('' + cat)
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
exports.parse = (new JsonReader).parse

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
	let output = [];

	t.add('$' + type);

	content = content || stack[0]
	output[0] = IntFunc.selectionWithField(t, stack, content instanceof cont ? content.name : '' + content, 42, 32, 6, true)

	t.add('$amount');
	let atf = t.field('' + (amount | 0), cons(t => { })).get();
	output[1] = prov(() => atf.getText() | 0);

	return output;
}

exports.getGenericType = function (field) {
	return ('class ' + field.getGenericType())
		.replace(field.type, '').replace(/\<(.+?)\>/, '$1')
		.split(/\,\s*/).map(str => Seq([Seq]).get(0).forName(str))
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
				if (vType + '' == 'boolean'){
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

