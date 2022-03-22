
const IntFunc = require('func/index');
const { otherTypes } = require('ui/dialogs/Editor');
const add = require('ui/components/addFieldBtn');
const typeSelection = require('ui/components/typeSelection');
const Fields = require('ui/components/Fields');
const { MyObject, MyArray } = require('func/constructor');
const Classes = exports.classes = Packages.mindustry.mod.ClassMap.classes;
const { caches: { content: contentIni } } = require('func/IniHandle')

const lang = Packages.java.lang

const defaultClass = exports.defaultClass = ObjectMap.of(
	Effect, 'none',
	UnitType, 'mono',
	Item, 'copper',
	Liquid, 'water',
	ItemStack, 'copper/0',
	LiquidStack, 'water/0'
)

const effects = new Seq()
let fs = IntFunc.toClass(Fx).getFields()
for (let i = 0; i < fs.length; i++) {
	effects.add(fs[i].name)
}
const UnitPlan = UnitFactory.UnitPlan

exports.filterClass = ObjectMap.of(
	// Attribute, (table, value) => {},
	Color, (table, value) => {
		let color
		try {
			color = Color.valueOf(value);
		} catch (e) { color = new Color; }
		let button = new Button();

		let image = button.image().size(30).color(color);
		let field = button.add('' + color).get();
		/* 使用原本自带的采色器 */
		button.clicked(() => Vars.ui.picker.show(color, cons(c => {
			image.color(color = c.cpy());
			field.setText(c + '');
		})));

		table.add(button);

		return prov(() => color)
	},
	BulletType, (table, value) => {
		table = table.table().get()
		value = value || new MyObject();
		let typeName = value.remove('type') || 'BulletType'
		let selection = new typeSelection.constructor(Classes.get(typeName), typeName, otherTypes.get(BulletType));
		table.add(selection.table).padBottom(4).row()
		let cont = table.table().name('cont').get()
		let map = fObject(cont, prov(() => selection.type), value, Seq([BulletType]))
		return map
	},
	StatusEffect, (table, value) => {
		table = table.table().get()
		value = value || new MyObject()
		let cont = table.table().name('cont').get()
		let map = fObject(cont, prov(() => StatusEffect), value, Seq([StatusEffect]))
		return map
	},
	// AmmoType, (table, value) => {},
	DrawBlock, (table, value) => {
		table = table.table().get()
		value = value || new MyObject();
		let typeName = value.remove('type') || 'DrawBlock'
		let selection = new typeSelection.constructor(Classes.get(typeName), typeName, otherTypes.get(DrawBlock));
		table.add(selection.table).padBottom(4).row()
		let cont = table.table().name('cont').get()
		let map = fObject(cont, prov(() => selection.type), value)
		return map
	},
	// Ability, (table, value) => {},
	Weapon, (table, value) => {
		table = table.table().get()
		value = value || new MyObject()
		let cont = table.table().name('cont').get()
		let map = fObject(cont, prov(() => Weapon), value, Seq([Weapon]))
		return map
	},
	/* 	UnitPlan, (table, value) => {
			table = table.table().get()
			value = value || new MyObject();
			let cont = table.table().name('cont').get()
	
		}, */

	ItemStack, (table, value) => {
		let [item, amount] =
			typeof value == 'string' ? value.split('/') :
				value instanceof MyObject ? [value.get('item'), value.get('amount')] : [Items.copper, 0]

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
		let val = '' + value || defaultClass.get(Effect);
		let btn = table.button(val, Styles.cleart, () => {
			IntFunc.showSelectListTable(btn, effects, val, 130, 50, cons(fx => btn.setText(val = fx)), true);
		}).width(200).height(45).get();
		return prov(() => val)
	},
	UnitType, (table, value) => {
		value = '' + value || defaultClass.get(UnitType);
		let prov = IntFunc.selectionWithField(table, Vars.content.units(), value, 42, 32, 6, true)

		return prov
	},
	Item, (table, value) => {
		value = '' + value || defaultClass.get(Item);
		let prov = IntFunc.selectionWithField(table, Vars.content.items(), value, 42, 32, 6, true)

		return prov
	},
	Liquid, (table, value) => {
		value = '' + value || defaultClass.get(Liquid);
		let prov = IntFunc.selectionWithField(table, Vars.content.liquids(), value, 42, 32, 6, true)

		return prov
	},
	ObjectMap, (table, value, vType, classes) => {
		let map = new MyObject()
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
		value = value || new MyObject()
		value.each(add)

		cont.button('$add', Icon.add, () => add(defaultClass.get(classes[0]), new MyObject())).fillX()

		return prov(() => map)
	}
)

const unitType = Seq.withArrays('none', 'flying', 'mech', 'legs', 'naval', 'payload');
const categories = new Seq();
const categoriesString = new ObjectMap();
const categoriesIcon = [];

exports.load = function () {
	Category.all.forEach(c => {
		categories.add(c)
		categoriesString.put(c.name(), c)
		categoriesIcon.push(Vars.ui.getIcon(c.name()));
	});
}

exports.filterKey = ObjectMap.of(
	'category', (table, value) => {
		let val = categoriesString.get(value, Category.distribution);

		let btn = new ImageButton(Styles.none, new ImageButton.ImageButtonStyle(Styles.clearPartial2i));
		let style = btn.getStyle();
		style.imageUp = Vars.ui.getIcon(val);
		btn.clicked(() => {
			IntFunc.showSelectImageTableWithIcons(btn, categories, categoriesIcon, val, 42, 32, cons(cat => style.imageUp = Vars.ui.getIcon(val = cat)), 2, false);
		});
		table.add(btn).size(45, 45);
		return prov(() => val)
	},
	'type', (table, value, type) => {
		let val;
		if (type == UnitType) {
			val = value || 'none';
			let btn = table.button(val, Styles.cleart, () => {
				IntFunc.showSelectListTable(btn, unitType, val, 130, 50, cons(type => btn.setText(val = type)), false);
			}).minWidth(100).height(45).get();
		}

		return prov(() => val)
	},
	'consumes', (table, value) => {
		value = value || new MyObject()
		let cont = table.table(Tex.button).get()
		let content = {
			power: (t, v) => {
				let field = new TextField(isNaN(v) ? '0' : '' + +v)
				t.add(field).row();
				t.image().fillX().color(Pal.accent)
				return prov(() => isNaN(field.getText()) ? 0 : +field.getText())
			}, powerBuffered: (t, v) => {
				let field = new TextField(isNaN(v) ? '0' : '' + +v)
				t.add(field).row();
				t.image().fillX().color(Pal.accent)
				return prov(() => isNaN(field.getText()) ? 0 : +field.getText())
			}, item: (t, obj) => {
				obj = obj || new MyObject()
				obj.put('items', fArray(t, Classes.get('ItemStack'), obj.getDefault('items', new MyArray())))
				t.row()
				t.table(cons(t => {
					t.check(Core.bundle.get('ModMake.consumes-optional', 'optional'), obj.getDefault('optional', false), boolc(b => obj.put('optional', b)))
					t.check(Core.bundle.get('ModMake.consumes-booster', 'booster'), obj.getDefault('booster', false), boolc(b => obj.put('booster', b)))
				})).fillX().row()
				t.image().fillX().color(Pal.accent)

				return obj
			}, liquid: (t, obj) => {
				obj = obj || new MyObject()
				let table = new Table()
				t.add(table).fillX()
				let p = exports.filterClass.get(LiquidStack)(table, obj)
				let v = p.get()
				t.row()
				t.table(cons(t => {
					t.check(Core.bundle.get(modName + '.consumes-optional', 'optional'), obj.getDefault('optional', false), boolc(b => v.put('optional', b)))
					t.check(Core.bundle.get(modName + '.consumes-booster', 'booster'), obj.getDefault('booster', false), boolc(b => v.put('booster', b)))
				})).fillX().row()
				return v
			}
		}
		function consumer(name, key, obj) {
			this.enable = obj != null
			this.name = name
			let table = new Table()
			let t = this.table = new Table()
			t.left().defaults().fillX().left()

			cont.check(Core.bundle.get("consumes." + name, name), this.enable, boolc(b => this.setup(b))).row()
			cont.add(table).fillX().left().padLeft(10).row()
			value.put(key, content[name](t, obj));
			cont.row()
			this.setup = function (b) {
				if (this.enable = b) {
					table.add(this.table).fillX()
				} else this.table.remove()
			}
			this.setup(this.enable)
		}
		let power = new consumer('power', 'power', value.get('power'));
		let powerBuffered = new consumer('powerBuffered', 'powerBuffered', value.get('powerBuffered'));
		let item = new consumer('item', 'items', value.get('items'));
		let liquid = new consumer('liquid', 'liquid', value.get('liquid'));

		return prov(() => {
			if (!power.enable) value.remove('power')
			if (!powerBuffered.enable) value.remove('powerBuffered')
			if (!item.enable) value.remove('items')
			if (!liquid.enable) value.remove('liquid')
			return value
		})
	},

	'upgrades', (table, value) => {
		if (!(value instanceof MyArray)) return null;
		table = table.table().fillX().get()
		let cont = new Table();
		//.name('upgrades-cont');
		table.add(cont).row();
		function build(item) {
			if (item instanceof MyArray) {
				let table = cont.table().get()
				cont.row();
				let unitType1 = exports.filterClass.get(UnitType)(table, item.get(0) || defaultClass.get(UnitType))
				item.put(0, unitType1)
				table.add("-->");
				let unitType2 = exports.filterClass.get(UnitType)(table, item.get(1) || defaultClass.get(UnitType))
				item.put(1, unitType2)
				table.button('', Icon.trash, Styles.cleart, () => {
					value.removeValue(item);
					table.remove()
				});
			} else {
				Vars.ui.showErrorMessage("upgrades解析错误");
				return null;
			}
		}
		value.each(build)
		table.button("$add", () => {
			let array = new MyArray()
			value.append(array)
			build(array)
		}).fillX()

		return value
	}

)

exports.make = function (type) {
	try {
		let cons = Seq([type]).get(0).getDeclaredConstructor();
		cons.setAccessible(true);
		return cons.newInstance();
	} catch (e) {
		Vars.ui.showErrorMessage(e);
	}
}

function fObject(t, type, value, typeBlackList, all) {
	let table = new Table(Tex.button), children = new Table,
		fields = new Fields.constructor(value, type, children);
	value = fields.map
	children.center().defaults().center().minWidth(100)
	table.add(children).row()
	t.add(table)

	if (all) {
		type.getDeclaredFields().forEach(f => {
			fields.add(null, f.name())
		})
	} else {
		value.each((k, v) => {
			if (!(v instanceof Function))
				fields.add(null, k)
		})
		table.add(add.constructor(value, fields, type)).fillX().growX()
	}
	return prov(() => {
		if (typeBlackList == null || !typeBlackList.contains(type.get())) value.put('type', type.get().getSimpleName())
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
		fields = new Fields.constructor(v || new MyArray(), prov(() => vType), children)
	children.center().defaults().center().minWidth(100)
	table.add(children).name('cont').row()
	t.add(table)
	v = fields.map
	v.each((v, j) => {
		addItem(vType, fields, j, v)
	})
	table.button('$add', () => {
		addItem(vType, fields, v.length, defaultClass.get(vType) || new MyObject())
	}).fillX()
	return prov(() => v)
}

let cont = Packages.mindustry.ctype.UnlockableContent
function buildOneStack(t, type, stack, content, amount) {
	let output = new MyObject();

	t.add('$' + type);

	content = content || stack.get(0)
	output.put(type, IntFunc.selectionWithField(t, stack, content instanceof cont ? content.name : '' + content, 42, 32, 6, true))

	t.add('$amount');
	let atf = t.field('' + amount, cons(t => { })).get();
	output.put('amount', prov(() => +atf.getText()));

	return prov(() => output);
}

let _class = Seq([Seq]).get(0);
exports.getGenericType = function (field) {
	let method = IntFunc.toClass(Json).getDeclaredMethod("getElementType", java.lang.reflect.Field, java.lang.Integer.TYPE);
	method.setAccessible(true);
	let arr = [], val, i = 0;
	do {
		val = method.invoke(null, field, new java.lang.Integer(i++))
		if (val != null) arr.push(val)
	} while (val != null);

	return arr;
}

const lstr = lang.String
/* 构建table */
exports.build = function (type, fields, t, k, v, isArray) {
	if (type == null) return;
	function fail() {
		let field = new TextField(('' + v).replace(/\n|\r/g, '\\n'))
		IntFunc.longPress(field, 600, longPress => longPress && IntFunc.showTextArea(field))
		t.add(field);
		return prov(() => field.getText().replace(/\s*/, '') != '' ? field.getText() : '')
	}
	let map = fields.map

	if (!isArray) t.add((contentIni.get(k) || k) + ':').fillX().left().padLeft(2).padRight(6);

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
					let obj = {
						"true": "是",
						"false": "否"
					}
					let btn = t.button(obj['' + v], Styles.cleart, () => btn.setText(obj['' + (v = !v)])).minWidth(100).height(45).get()
					return prov(() => v)
				}
				return
			}

			if ((vType.isArray() || vType == Seq) && v instanceof MyArray && k != "upgrades") {
				return fArray(t, vType == Seq ? this.getGenericType(field)[0] : vType.getComponentType(), v)
			}
			if (IntFunc.toClass(ObjectMap).isAssignableFrom(vType)) {
				let classes = this.getGenericType(field)
				return this.filterClass.get(vType)(t, v, vType, classes)
			}
			if (this.filterClass.containsKey(vType)) {
				return this.filterClass.get(vType)(t, v, vType)
			}
		} catch (e) {
			Log.info(type)
			Log.err(e)
		}
		finally {

			if (this.filterKey.containsKey(k)) {
				return this.filterKey.get(k)(t, v, type)
			}
		}
		return
	})();
	map.put(k, output || fail())


	t.table(cons(right => {
		right.right().defaults().right()
		if (!isArray && contentIni.has(k + '.help')) {
			let btn = right.button('?', Styles.clearPartialt, () => IntFunc.showSelectTable(btn, (p, hide) => {
				p.pane(p => p.add(contentIni.get(k + '.help'), 1.3)).pad(4, 8, 4, 8)
			}, false)).size(8 * 5).padLeft(5).padRight(5).right().grow().get();
		}
		right.button('', Icon.trash, Styles.cleart, () => fields.remove(t, k));
	})).right().growX().right();

}

