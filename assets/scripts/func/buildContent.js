
const IntFunc = require('func/index');
const findClass = require('func/findClass')
const IntStyles = findClass('ui.styles');
const Classes = findClass("util.Classes")
const ContentSeq = findClass("util.ContentSeq");
const { otherTypes } = ContentSeq;
const addFieldBtn = require('ui/components/addFieldBtn');
const typeSelection = require('ui/components/typeSelection');
const Fields = require('ui/components/Fields');
const { MyObject, MyArray } = require("func/constructor");
const { settings, content: contentIni, types: typesIni, parse } = findClass("components.dataHandle");
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
	BulletType, new MyObject(),
	Sector, 0,
	Planet, "serpulo",
)

function genericSeqByClass(clazz, _func) {
	let seq = new Seq()
	let f = IntFunc.toClass(clazz).getFields()
	for (let i = 0; i < f.length; i++) {
		seq.add(_func.get(f[i]))
	}
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
	Attributes, (table, value) => {
		let map = new MyObject()
		let cont = new Table(Tex.button)
		let children = new Table()
		cont.add(children).fillX().row()
		table.add(cont).fillX()
		let i = 0
		function add(k, v) {
			children.add(Fields.build(i++, cons(t => {
				let key = exports.filterClass.get(Attribute)(t, k);
				map.put(
					key, fail(t, v, IntFunc.toClass(lang.Double.TYPE))
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

		cont.button('$add', Icon.add, () => add(null, 0)).growX().minWidth(100)

		return prov(() => map)
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
	Sector, (table, value) => fail(table, value || 0, IntFunc.toClass(lang.Double.TYPE)),
	BulletType, (table, value) => {
		return listWithType(table, value, BulletType, "BasicBulletType", bullets, func(b => b));
	},
	StatusEffect, (table, value) => {
		return listWithType(table, value, StatusEffect, "StatusEffect", Vars.content.statusEffects(), func(s => s.name), Seq([StatusEffect]));
	},
	Weather, (table, value) => {
		return listWithType(table, value, Weather, "ParticleWeather", Vars.content.getBy(ContentType.weather), func(w => w.name));
	},
	// AmmoType, (table, value) => {},
	DrawBlock, (table, value, vType) => {
		if (typeof value == "string") value = MyObject.of("type", value)
		return tableWithTypeSelection(table, value, vType, "DrawBlock")
	},
	Ability, (table, value, vType) => {
		return tableWithTypeSelection(table, value, vType, "Ability")
	},
	Weapon, (table, value, vType) => {
		return tableWithTypeSelection(table, value, vType, "Weapon")
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

		if (amount == null || isNaN(amount)) amount = 0
		return buildOneStack(table, 'liquid', Vars.content.liquids(), item, amount)
	},
	Effect, (table, value) => {
		return listWithType(table, value, Effect, "ParticleEffect", effects, func(e => e));
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
	Planet, (table, value, vType) => {
		return tableWithFieldImage(table, value, vType, Vars.content.planets())
	},
	ObjectMap, (table, value, vType, classes) => {
		let map = new MyObject()
		let cont = new Table(Tex.button)
		let group = new Table()
		cont.add(group).fillX().row()
		table.add(cont).fillX()
		let i = 0
		function add(k, v, index) {
			let tab = Fields.build(i++, cons(t => {
				let key = exports.filterClass.get(classes[0])(t, k || defaultClass.get(classes[0]));
				let foldt = foldTable()
				t.add(foldt[0])
				map.put(
					key, exports.filterClass.get(classes[1])(foldt[1], v)
				)
				function remove(){
					map.remove(key)
					if (t != null) t.remove()
				};
				t.table(cons(right => {
					copyAndPaste(right, k, v, newV => {
						remove()
						add(k, newV);
					}, () => {})
					right.button('', Icon.trash, IntStyles.cleart, remove);
				})).padLeft(4).growX().right();
			}))
			tab.defaults().growX()
			group.add(tab).growX().row()
		}
		value = value || new MyObject()
		value.each(add)

		cont.button('$add', Icon.add, () => add()).growX().minWidth(100)

		return prov(() => map)
	},

	UnitPlan, (table, value) => {
		let map = value || new MyObject()
		let cont = new Table(Tex.button)
		table.add(cont).fillX()
		cont.add(Core.bundle.get('unit', 'unit'));
		map.put("unit", exports.filterClass.get(UnitType)(cont, map.get("unit", exports.defaultClass.get(UnitType))));
		cont.row()
		cont.add(Core.bundle.get('time', 'time'));
		map.put("time", fail(cont, map.get("time", 0), IntFunc.toClass(java.lang.Double.TYPE)));
		cont.row()
		cont.add(Core.bundle.get("requirements"))
		let foldt = foldTable()
		cont.add(foldt[0]).row()
		map.put(
			"requirements", fArray(foldt[1], IntFunc.toClass(ItemStack), map.get("requirements", new MyArray()))
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
	otherTypes.get(AIController).each(cons(ai => {
		if (!AIBlackList.contains(ai)) {
			AISeq.add(ai.getSimpleName())
		}
	}))
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
	'consumes', (table, value, type) => {
		value = value || new MyObject()
		let cont = table.table(Tex.button).get()
		let all = new Map()
		function consumer(name, key, obj, _func, disabledProv) {
			this.enable = obj != null
			this.name = name
			let table = new Table()
			table.defaults().growX().left()

			let c = cont.check(Core.bundle.get("consumes." + name, name), this.enable, boolc(b => this.setup(b)));
			if (disabledProv) c.disabled(disabledProv)
			c.row()
			c = null
			let cell = cont.add().growX().left().padLeft(10);
			cont.row()

			value.put(key, _func(table, obj));
			this.setup = function (b) {
				if ((this.enable = b)) {
					cell.setElement(table)
				} else {
					cell.clearElement()
				}
			}
			this.setup(this.enable)
			this.check = function () {
				if (!this.enable) value.remove(this.name)
			}
			all.set(name, this)
		}
		new consumer('power', 'power', value.get('power'), (t, v) => {
			let field = new TextField(isNaN(v) ? '0' : '' + +v)
			t.add(field).row();
			t.image().fillX().color(Pal.accent)
			return prov(() => isNaN(field.getText()) ? 0 : +field.getText())
		}, () => all.get('powerBuffered') != null && all.get('powerBuffered').enable);

		new consumer('powerBuffered', 'powerBuffered', value.get('powerBuffered'), (t, v) => {
			let field = new TextField(isNaN(v) ? '0' : '' + +v)
			t.add(field).row();
			t.image().fillX().color(Pal.accent)
			return prov(() => isNaN(field.getText()) ? 0 : +field.getText())
		}, () => all.get('power') != null && all.get('power').enable);

		let itemObj = (() => {
			let item = value.get('item')
			if (item == null) return item = null, value.get('items')
			let arr = new MyArray(new ItemStack(item))
			let obj = MyObject.of("items", arr);
			return obj
		})();
		new consumer('item', 'items', itemObj, (t, obj) => {
			obj = obj || new MyObject()
			obj.put('items', fArray(t, Classes.get('ItemStack'), obj.get('items', new MyArray())))
			t.row()
			t.table(cons(t => {
				t.check(Core.bundle.get(mod_Name + '.consumes-optional', 'optional'), obj.get('optional', false), boolc(b => obj.put('optional', b)))
				t.check(Core.bundle.get(mod_Name + '.consumes-booster', 'booster'), obj.get('booster', false), boolc(b => obj.put('booster', b)))
			})).fillX().row()
			t.image().fillX().color(Pal.accent)

			return obj
		}, () => type == NuclearReactor);
		if (type == NuclearReactor) all.get("item").setup(true)

		new consumer('liquid', 'liquid', value.get('liquid'), (t, obj) => {
			obj = obj || new MyObject()
			let table = new Table()
			t.add(table).fillX()
			let p = exports.filterClass.get(LiquidStack)(table, obj)
			let v = p.get()
			t.row()
			t.table(cons(t => {
				t.check(Core.bundle.get(mod_Name + '.consumes-optional', 'optional'), obj.get('optional', false), boolc(b => v.put('optional', b)))
				t.check(Core.bundle.get(mod_Name + '.consumes-booster', 'booster'), obj.get('booster', false), boolc(b => v.put('booster', b)))
			})).fillX().row()
			return v
		}, () => (all.get('coolant') != null && all.get('coolant').enable) || type == NuclearReactor);
		if (type == NuclearReactor) all.get("liquid").setup(true)

		new consumer('coolant', 'coolant', value.get('coolant'), (t, obj) => {
			obj = obj || new MyObject()
			let opt = obj.get('optional', false)
			let boost = obj.get('booster', false)
			let _prov = fObject(t, prov(() => IntFunc.toClass(ConsumeCoolant)), obj, Seq(), true)
			t.row()
			let v = new MyObject()
			v.put('update', false)
			t.table(cons(t => {
				t.check(Core.bundle.get(mod_Name + '.consumes-optional', 'optional'), opt, boolc(b => v.put('optional', b)))
				t.check(Core.bundle.get(mod_Name + '.consumes-booster', 'booster'), boost, boolc(b => v.put('booster', b)))
			})).fillX().row()
			return prov(() => {
				let out = _prov.get()
				v.each((k, v) => out.put(k, v))
				return out
			})
		}, () => (all.get('liquid') != null && all.get('liquid').enable) || type == NuclearReactor);
		if (type == NuclearReactor) all.get("coolant").setup(false)

		return prov(() => {
			for (let v of all.values()) {
				v.check()
			}
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
			if (!(item instanceof MyArray)) {
				IntFunc.showException("upgrades解析错误", new Error("item is't a MyArray"));
				return;
			}
			let list = item.toArray()
			item.clear()
			let table = cont.table().get()
			cont.row();
			let unitType1 = exports.filterClass.get(UnitType)(table, list.get(0) || defaultClass.get(UnitType))
			item.put(0, unitType1)
			table.add("-->");
			let unitType2 = exports.filterClass.get(UnitType)(table, list.get(1) || defaultClass.get(UnitType))
			item.put(1, unitType2)
			table.button('', Icon.trash, IntStyles.cleart, () => {
				value.removeValue(item);
				table.remove()
			}).marginLeft(4);
		}
		value.each(cons(build))
		table.button("$add", () => {
			let array = new MyArray()
			value.append(array)
			build(array)
		}).growX().minWidth(100)

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
	let table = new Table(Tex.pane), children = new Table(),
		fields = new Fields(value, type, children);
	children.center().defaults().center().minWidth(100)
	table.add(children).row()
	t.add(table)

	if (all) {
		let vType = type.get()
		vType.getFields().forEach(f => {
			f.setAccessible(true)
			if (addFieldBtn.filter(f, vType)) {
				fields.add(null, f.name, addFieldBtn.defaultValue(f.type))
			}
		})
	} else {
		value.each(cons2((k, v) => {
			if (!(v instanceof Function))
				fields.add(null, k)
		}))
		table.add(addFieldBtn(value, fields, type)).fillX().growX().minWidth(100)
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
		fields = new Fields(v || new MyArray(), prov(() => vType), children)
	children.center().defaults().center().minWidth(100)
	table.add(children).name('cont').row()
	t.add(table)
	v.cpy().each(cons2((i, v) => {
		addItem(vType, fields, i, v)
	}))
	table.button('$add', () => {
		addItem(vType, fields, -1, addFieldBtn.defaultValue(vType))
	}).growX().minWidth(100)

	return prov(() => v)
}

function foldTable() {
	let table = new Table();
	let content = new Table();
	let col = new Collapser(content, false);
	col.setDuration(0.3)

	let btn = new ImageButton(Icon.rightOpen, Styles.clearTogglei);
	let style = btn.style;
	btn.clicked(() => {
		col.toggle()
		style.imageUp = col.isCollapsed() ? Icon.rightOpen : Icon.downOpen
	});
	style.up = style.over = Tex.whiteui.tint(0.6, 0.8, 0.8, 1)
	table.add(btn).padTop(1).padBottom(1).padRight(4).growY().width(32);
	table.add(col).growX().left()
	if (settings.getBool("auto_fold_code")) {
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
		IntFunc.showSelectListTable(btn, seq, val, 130, 50, cons(
			type => btn.setText(typesIni.get(val = type) || val)
		), searchable);
	}).minWidth(100).height(45).get();
	return prov(() => val)
}

function tableWithTypeSelection(table, value, vType, defaultValue) {
	table = table.table().get()
	value = value || new MyObject();
	let typeName = value.remove('type') || defaultValue
	let selection = new typeSelection(Classes.get(typeName), typeName, otherTypes.get(vType));
	table.add(selection.table).padBottom(4).row()
	let cont = table.table().name('cont').get()
	let map = fObject(cont, prov(() => selection.type), value)
	return map
}

function listWithType(table, value, vType, defaultValue, list, _func, blackList) {
	list = new Seq(list)
	list.add("自定义")
	value = value || defaultClass.get(vType)
	let isObject = value instanceof MyObject;
	let val1 = isObject ? value : new MyObject();
	let table1 = new Table()
	let typeName = val1.remove('type') || defaultValue;
	let selection = new typeSelection(Classes.get(typeName), typeName, otherTypes.get(vType) || [IntFunc.toClass(vType)]);
	table1.add(selection.table).padBottom(4).row()
	let cont = table1.table().name('cont').get()
	let map = fObject(cont, prov(() => selection.type), val1, blackList || Seq())

	let val2 = isObject ? "自定义" : value;
	let btn = table.button(typesIni.get(val2 + "") || val2 + "", IntStyles.cleart, () => {
		IntFunc.showSelectListTable(btn, list, val2, 130, 50, cons(fx => {
			btn.setText(typesIni.get(fx + "") || fx + "");
			if (fx == "自定义") {
				cell.setElement(table1)
				val2 = map;
			} else {
				cell.clearElement()
				val2 = _func.get(fx);
			}
		}), true);
	}).width(200).height(45).get();
	table.row()
	let cell = table.add();

	if (isObject) {
		cell.setElement(table1);
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
	while (true) {
		val = method.invoke(null, field, new lang.Integer(i++))
		if (val == null) break;
		arr.push(val)
	}

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
const foldBlackList = Seq.with(lstr, Color, Category, ItemStack, LiquidStack, UnitType, Item, Liquid, Sector, Planet)
// 单位额外字段
const UnitTypeExFields = Seq.with("requirements", "waves", "controller", "type")
const json = addFieldBtn.json;
const reader = new JsonReader();

function copyAndPaste(table, k, v, paste, catchf){
	table.table(cons(t => {
		// 复制
		t.button('', Icon.copy, IntStyles.cleart, () => 
			Core.app.setClipboardText("" + v)
		).padRight(2)
		// 粘贴
		t.button('', Icon.paste, IntStyles.cleart, () => Vars.ui.showConfirm(
			'粘贴', '是否要粘贴', () => {
				let txt = Core.app.getClipboardText()
				try {
					paste(parse(txt));
				} catch(e) {
					catchf()
					IntFunc.showException(e)
				}
			}
		))
	})).padRight(6);
}


function fail(t, v, vType) {
	vType = vType || IntFunc.toClass(lstr)
	let field = new TextField(('' + v).replace(/\n|\r/g, '\\n').replace(/\t/g, '\\t'))
	if (IntFunc.toClass(lstr).isAssignableFrom(vType)) IntFunc.longPress(field, 600, longPress => longPress && IntFunc.showTextArea(field))
	if (Vars.mobile) field.removeInputDialog()
	t.add(field).growX();
	return prov(() => {
		let txt = field.getText().replace(/\s*/, '') != '' ? field.getText() : ''
		// 通过eval转义
		txt = eval('"' + txt.replace(/"/g, '\\"') + '"').replace(/\n|\r/g, "\n").replace(/"/g, '\\"')
		return vType.isPrimitive() ? txt : '"' + txt + '"';
	})
}
/* 构建table */
exports.build = function (type, fields, t, k, v, isArray) {
	if (type == null) return;
	let unknown = false;
	if (!isArray && (type != UnitType || !UnitTypeExFields.contains(k)) && settings.getBool("point_out_unknown_field") && !json.getFields(type).containsKey(k)) {
		t.table(Tex.pane, cons(t => t.add('未知', Color.yellow))).padRight(5)
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
	// 不是数组的话，添加key
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

			if ((vType.isArray() || addFieldBtn.arrayClass.contains(vType)) && v instanceof MyArray && k != "upgrades") {
				return fArray(t, addFieldBtn.arrayClass.contains(vType) ? this.getGenericType(field)[0] : vType.getComponentType(), v)
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
			Log.err("[red][" + (e.type || "异常") + "][]" + e.message + "(#" + e.lineNumber + ")")
			Log.err(e.stack)
		} finally {
			if (this.filterKey.containsKey(k)) {
				return this.filterKey.get(k)(t, v, type)
			}
		}
	})();
	map.put(k, output || fail(t, v, vType))
	if (tmp != null) t = tmp;

	// 右边
	t.table(cons(right => {
		right.right().defaults().right()
		if (foldt) copyAndPaste(right, k, v, v2 => {
			map.put(k, v2)
			fields.setTable(k, Fields.json(fields, 0, k))
		}, () => {
			map.put(k, v)
			fields.setTable(k, t)
		})
		// 帮助按钮
		if (!isArray && !unknown && contentIni.containsKey(k + '.help')) {
			let btn = right.button('?', Styles.clearPartialt, () => IntFunc.showSelectTable(btn, (p, _1) => {
				p.pane(p => p.add(contentIni.get(k + '.help'), 1.3)).pad(4, 8, 4, 8);
			}, false)).size(8 * 5).padLeft(5).padRight(5)
				.right().grow().get();
		}
		// 删除按钮
		right.button('', Icon.trash, IntStyles.cleart, () => fields.remove(k));
	})).padLeft(4).growX().right();

	t.row()
}

