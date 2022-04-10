
const IntFunc = require('func/index');
const IntSettings = require("content/settings");
const IntStyles = require('ui/styles');
const { otherTypes } = require('ui/dialogs/Editor');
const add = require('ui/components/addFieldBtn');
const typeSelection = require('ui/components/typeSelection');
const Fields = require('ui/components/Fields');
const { MyObject, MyArray } = require('func/constructor');
const Classes = exports.classes = Packages.mindustry.mod.ClassMap.classes;
const { caches: { content: contentIni, types: typesIni } } = require('func/IniHandle')
const mod_Name = modName

const lang = java.lang

const defaultClass = exports.defaultClass = ObjectMap.of(
	Effect, 'none',
	UnitType, 'mono',
	Item, 'copper',
	Liquid, 'water',
	ItemStack, 'copper/0',
	LiquidStack, 'water/0',
	Attribute, Attribute.all[0],
	BulletType, new MyObject()
)

function genericSeqByClass(clazz, _func) {
	let seq = new Seq()
	let f = IntFunc.toClass(clazz).getFields()
	for (let i = 0; i < f.length; i++) {
		seq.add(_func.get(f[i]))
	}
	seq.add("自定义")
	return seq;
}
const effects = genericSeqByClass(Fx, func(field => field.name))
const bullets = genericSeqByClass(Bullets, func(field => field.name))
const attributes = Seq(Attribute.all)

const UnitPlan = UnitFactory.UnitPlan

exports.filterClass = ObjectMap.of(
	Attribute, (table, value) => {
		return tableWithListSelection(table, value, attributes, defaultClass.get(Attribute), false)
	},
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

		return prov(() => '"' + color + '"')
	},
	BulletType, (table, value) => {
		return listWithType(table, value, BulletType, "BasicBulletType", bullets);
	},
	StatusEffect, (table, value) => {
		return listWithType(table, value, StatusEffect, "StatusEffect", Vars.content.statusEffects(), Seq([StatusEffect]));
	},
	// AmmoType, (table, value) => {},
	DrawBlock, (table, value, vType) => {
		return tableWithTypeSelection(table, value, vType, "DrawBlock")
	},
	Ability, (table, value, vType) => {
		return tableWithTypeSelection(table, value, vType, "Ability")
	},
	Weapon, (table, value) => {
		table = table.table().get()
		value = value || new MyObject()
		let cont = table.table().name('cont').get()
		let map = fObject(cont, prov(() => Weapon), value, Seq([Weapon]))
		return map
	},

	ItemStack, (table, value) => {
		let [item, amount] =
			typeof value == 'string' ? value.split('/') :
				value instanceof MyObject ? [value.get('item'), value.get('amount')] : [Items.copper, 0]

		if (isNaN(amount)) throw TypeError('\'' + amount + '\' isn\'t a number')
		return buildOneStack(table, 'item', Vars.content.items(), item, amount)
	},
	// like ItemStack
	LiquidStack, (table, value) => {
		let [item, amount] = typeof value == 'string' ?
			value.split('/') : [value.get('liquid'), value.get('amount')]

		if (isNaN(amount)) amount = 0
		return buildOneStack(table, 'liquid', Vars.content.liquids(), item, amount)
	},
	Effect, (table, value) => {
		return listWithType(table, value, Effect, "ParticleEffect", effects);
	},
	UnitType, (table, value, vType) => {
		return tableWithFieldImage(table, value, vType, Vars.content.units())
	},
	Item, (table, value, vType) => {
		return tableWithFieldImage(table, value, vType, Vars.content.items())
	},
	Liquid, (table, value, vType) => {
		return tableWithFieldImage(table, value, vType, Vars.content.liquids())
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
				let key = exports.filterClass.get(classes[0])(t, k);
				let foldt = foldTable()
				t.add(foldt[0])
				map.put(
					key, exports.filterClass.get(classes[1])(foldt[1], v)
				)
				t.table(cons(right => {
					right.button('', Icon.trash, IntStyles.cleart, () => {
						map.remove(key)
						if (t != null) t.remove()
					});
				})).padLeft(4).growX().right();
			}))).growX().row()
		}
		value = value || new MyObject()
		value.each(add)

		cont.button('$add', Icon.add, () => add(defaultClass.get(classes[0]), new MyObject())).fillX()

		return prov(() => map)
	},

	UnitPlan, (table, value) => {
		let map = value || new MyObject()
		let cont = new Table(Tex.button)
		table.add(cont).fillX()
		cont.add(Core.bundle.get('unit', 'unit'));
		map.put("unit", exports.filterClass.get(UnitType)(cont, map.get("unit")));
		cont.row()
		cont.add(Core.bundle.get('time', 'time'));
		map.put("time", fail(cont, map.getDefault("time", 0), java.lang.Number));
		cont.row()
		cont.add(Core.bundle.get("requirements"))
		let foldt = foldTable()
		cont.add(foldt[0]).row()
		map.put(
			"requirements", fArray(foldt[1], IntFunc.toClass(ItemStack), map.getDefault("requirements", new MyArray()))
		);

		return prov(() => map)
	}
)

const unitType = Seq.with('none', 'flying', 'mech', 'legs', 'naval', 'payload');
const AISeq = new Seq();
const AIBlackList = Seq.with(FormationAI);
const categories = new Seq();
const categoriesString = new ObjectMap();
const categoriesIcon = [];

exports.load = function () {
	Category.all.forEach(c => {
		categories.add(c)
		categoriesString.put(c.name(), c)
		categoriesIcon.push(Vars.ui.getIcon(c.name()));
	});
	otherTypes.get(AIController).forEach(ai => {
		if (!AIBlackList.contains(ai)) {
			AISeq.add(ai.getSimpleName())
		}
	})
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
		if (type == UnitType) {
			return tableWithListSelection(table, value, unitType, "none", false)
		}
		return null;
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
					t.check(Core.bundle.get(mod_Name + '.consumes-optional', 'optional'), obj.getDefault('optional', false), boolc(b => obj.put('optional', b)))
					t.check(Core.bundle.get(mod_Name + '.consumes-booster', 'booster'), obj.getDefault('booster', false), boolc(b => obj.put('booster', b)))
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
					t.check(Core.bundle.get(mod_Name + '.consumes-optional', 'optional'), obj.getDefault('optional', false), boolc(b => v.put('optional', b)))
					t.check(Core.bundle.get(mod_Name + '.consumes-booster', 'booster'), obj.getDefault('booster', false), boolc(b => v.put('booster', b)))
				})).fillX().row()
				return v
			}
		}
		function consumer(name, key, obj) {
			this.enable = obj != null
			this.name = name
			let table = new Table()
			let t = this.table = new Table()
			t.left().defaults().growX().left()

			cont.check(Core.bundle.get("consumes." + name, name), this.enable, boolc(b => this.setup(b))).row()
			cont.add(table).growX().left().padLeft(10).row()
			value.put(key, content[name](t, obj));
			cont.row()
			this.setup = function (b) {
				if (this.enable = b) {
					table.add(this.table).growX()
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
				table.button('', Icon.trash, IntStyles.cleart, () => {
					value.removeValue(item);
					table.remove()
				}).marginLeft(4);
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
	},

	'controller', (table, value, type) => {
		if (type == UnitType) {
			return tableWithListSelection(table, value, AISeq, "FlyingAI", false)
		}
		return null;
	}
)

exports.make = function (type) {
	try {
		let cons = IntFunc.toClass(type).getDeclaredConstructor();
		cons.setAccessible(true);
		return cons.newInstance();
	} catch (e) {
		Vars.ui.showErrorMessage(e);
	}
}

function fObject(t, type, value, typeBlackList, all) {
	let table = new Table(Tex.pane), children = new Table,
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
		addItem(vType, fields, v.length, add.defaultValue(vType))
	}).growX()
	return prov(() => v)
}

function foldTable() {
	let folded = false;
	let table = new Table();
	let content = new Table();
	let btn = table.button(Icon.rightOpen, Styles.clearTogglei, () => {
		folded = !folded
		folded ? content.remove() : table.add(content)
		btn.getStyle().imageUp = folded ? Icon.rightOpen : Icon.downOpen
	}).padTop(1).padBottom(1).padRight(4).growY().width(32).get()
	table.add(content).growX().left()
	if (IntSettings.getValue("editor", "auto_fold_code")) {
		btn.fireClick()
	}
	return [table, content]
}

function tableWithFieldImage(table, value, vType, seq) {
	value = '' + (value || defaultClass.get(vType));
	let prov = IntFunc.selectionWithField(table, seq, value, 42, 32, 6, true)

	return prov
}

function tableWithListSelection(table, value, seq, defaultValue, searchable) {
	let val = '' + (value || defaultValue);
	let btn = table.button(typesIni.get(val) || val, IntStyles.cleart, () => {
		IntFunc.showSelectListTable(btn, seq, val, 130, 50, cons(type => btn.setText(val = type)), searchable);
	}).minWidth(100).height(45).get();
	return prov(() => val)
}

function tableWithTypeSelection(table, value, vType, defaultValue) {
	table = table.table().get()
	value = value || new MyObject();
	let typeName = value.remove('type') || defaultValue
	let selection = new typeSelection.constructor(Classes.get(typeName), typeName, otherTypes.get(vType));
	table.add(selection.table).padBottom(4).row()
	let cont = table.table().name('cont').get()
	let map = fObject(cont, prov(() => selection.type), value)
	return map
}

function listWithType(table, value, vType, defaultValue, list, blackList) {
	let isObject = value instanceof MyObject;
	let val1 = isObject ? value : new MyObject();
	let table1 = new Table()
	let typeName = val1.remove('type') || defaultValue;
	let selection = new typeSelection.constructor(Classes.get(typeName), typeName, otherTypes.get(vType) || [IntFunc.toClass(vType)]);
	table1.add(selection.table).padBottom(4).row()
	let cont = table1.table().name('cont').get()
	let map = fObject(cont, prov(() => selection.type), val1, blackList || Seq())

	let val2 = isObject ? "自定义" : value || defaultClass.get(vType);
	let btn = table.button(val2, IntStyles.cleart, () => {
		Log.info(list)
		IntFunc.showSelectListTable(btn, list, val2, 130, 50, cons(fx => {
			btn.setText(fx);
			if (fx != "自定义") {
				val2 = fx;
				table1.remove()
			} else {
				table.add(table1);
				val2 = map;
			}
		}), true);
	}).width(200).height(45).get();
	table.row()

	if (isObject) {
		table.add(table1);
		val2 = map
	}

	return prov(() => val2 instanceof Prov ? val2.get() : val2)
}


let cont = Packages.mindustry.ctype.UnlockableContent
function buildOneStack(t, type, stack, content, amount) {
	let output = new MyObject();

	t.table(Tex.pane, cons(_t => t = _t)).grow()

	t.add('$' + type);

	content = content || stack.get(0)
	output.put(type, IntFunc.selectionWithField(t, stack, content instanceof cont ? content.name : '' + content, 42, 32, 6, true))

	t.add('$amount');
	let atf = t.field('' + amount, cons(t => { })).get();
	output.put('amount', prov(() => +atf.getText()));

	return prov(() => output);
}

exports.getGenericType = function (field) {
	let method = IntFunc.toClass(Json).getDeclaredMethod("getElementType", lang.reflect.Field, lang.Integer.TYPE);
	method.setAccessible(true);
	let arr = [], val, i = 0;
	do {
		val = method.invoke(null, field, new lang.Integer(i++))
		if (val != null) arr.push(val)
	} while (val != null);

	return arr;
}

function getType(type, k, isArray) {
	try {
		let field = isArray ? null : type.getField(k)
		let vType = isArray ? type : field.type
		return [field, vType];
	} catch (e) {
		// IntFunc.showException(e)
		return []
	}

}

const lstr = lang.String
// 折叠黑名单
const foldBlackList = Seq.with(lstr, Color, Category, ItemStack, LiquidStack, UnitType, Item, Liquid)
// 单位额外字段
const UnitTypeExFields = Seq.with("requirements", "waves", "controller", "type")
const json = add.json;

function fail(t, v, vType) {
	vType = vType = IntFunc.toClass(lstr)
	let field = new TextField(('' + v).replace(/\n|\r/g, '\\n'))
	if (IntFunc.toClass(lstr).isAssignableFrom(vType)) IntFunc.longPress(field, 600, longPress => longPress && IntFunc.showTextArea(field))
	if (Vars.mobile) field.removeInputDialog()
	t.add(field).growX();
	return prov(() => {
		let txt = field.getText().replace(/\s*/, '') != '' ? field.getText() : ''
		return vType.isPrimitive() ? txt : '"' + txt + '"';
	})
}
/* 构建table */
exports.build = function (type, fields, t, k, v, isArray) {
	if (type == null) return;
	let unknown = false;
	if (!isArray && (type != UnitType || !UnitTypeExFields.contains(k)) && IntSettings.getValue("editor", "point_out_unknown_field") && !json.getFields(type).containsKey(k)) {
		t.table(Tex.pane, cons(t => t.add('unknown', Color.yellow))).padRight(5)
		unknown = true
	}

	let map = fields.map

	let [field, vType] = getType(type, k, isArray);

	// 折叠代码
	let foldt, tmp
	if (vType != null && !vType.isPrimitive() && !foldBlackList.contains(vType)) {
		tmp = t
		foldt = foldTable()
	}
	if (!isArray) t.add((unknown ? k : (contentIni.get(k) || k)) + ':').fillX().left().padLeft(2).padRight(6);

	if (foldt != null) {
		t.add(foldt[0])
		t = foldt[1]
	}
	let output = (() => {

		if (type == null) return

		try {
			if (vType == null || vType == lstr) {
				return
			}
			if (vType.isPrimitive()) {
				if (vType + '' == 'boolean') {
					let obj = {
						"true": "是",
						"false": "否"
					}
					let btn = t.button(obj['' + v], IntStyles.cleart, () => btn.setText(obj['' + (v = !v)])).minWidth(100).height(45).get()
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
			Log.err("[red][" + e.type + "][]" + e.message + "(#" + e.lineNumber + ")")
			Log.err(e.stack)
		}
		finally {

			if (this.filterKey.containsKey(k)) {
				return this.filterKey.get(k)(t, v, type)
			}
		}
		return
	})();
	map.put(k, output || fail(t, v, vType))
	if (tmp != null) t = tmp;

	t.table(cons(right => {
		right.right().defaults().right()
		if (!isArray && !unknown && contentIni.has(k + '.help')) {
			let btn = right.button('?', Styles.clearPartialt, () => IntFunc.showSelectTable(btn, (p, hide) => {
				p.pane(p => p.add(contentIni.get(k + '.help'), 1.3)).pad(4, 8, 4, 8)
			}, false)).size(8 * 5).padLeft(5).padRight(5).right().grow().get();
		}
		right.button('', Icon.trash, IntStyles.cleart, () => fields.remove(t, k));
	})).padLeft(4).growX().right();

}

