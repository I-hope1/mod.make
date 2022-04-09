const IntFunc = require('func/index');
const Fields = require('ui/components/Fields');
// const scripts = require('ui/scripts');
const addBtn = require('ui/components/addFieldBtn');
const typeSelection = require('ui/components/typeSelection');
const IntSettings = require("content/settings");
const IniHandle = require('func/IniHandle')

const Classes = Packages.mindustry.mod.ClassMap.classes;

let fileName, fileNameTable,
	cont, pane, result = {};

exports.contentTypes = new Seq();
const ContentTypes = exports.ContentTypes = new ObjectMap();
exports.otherTypes = ObjectMap.of(
	BulletType, [],
	DrawBlock, [],
	Ability, [],
	Effect, [],
	AIController, []
)
const types = exports.types = {};

exports.load = function () {
	let field = Vars.mods.getClass().getDeclaredField('parser')
	field.setAccessible(true)
	let parser = this.parser = field.get(Vars.mods)
	field = parser.getClass().getDeclaredField("parsers")
	field.setAccessible(true)
	let parsers = field.get(parser)
	for (let type of ContentType.all) {
		let arr = Vars.content.getBy(type);
		if (!arr.isEmpty()) {
			let c = arr.first().getClass();
			//get base content class, skipping intermediates
			while (!(c.getSuperclass() == Content || c.getSuperclass() == UnlockableContent || java.lang.reflect.Modifier.isAbstract(c.getSuperclass().getModifiers()))) {
				c = c.getSuperclass();
			}
			if (parsers.containsKey(type)) {
				this.contentTypes.add(c, type);
				type = type + ""
				let type_s = type.endsWith("s") ? type : type + "s"
				this.ContentTypes.put(type_s, type)
			}
		}
	}

	for (let i = 0; i < this.contentTypes.size; i += 2) {
		let key = this.contentTypes.get(i + 1)
		if (parsers.containsKey(key)) {
			types[key] = []
		}
	}

	Classes.each(new Cons2({
		get: (k, type) => {
			if (!IntSettings.getValue("editor", "display_deprecated") && type.isAnnotationPresent(java.lang.Deprecated)) return;
			for (let i = 0; i < this.contentTypes.size; i += 2) {
				if (this.contentTypes.get(i).isAssignableFrom(type)) {
					let key = this.contentTypes.get(i + 1)
					types[key].push(type)
					break;
				}
			}
			this.otherTypes.each(new Cons2({
				get: (k, arr) => {
					if (k.isAssignableFrom(type) && !java.lang.reflect.Modifier.isAbstract(type.getModifiers())) {
						arr.push(type)
					}
				}
			}))
		}
	}));

	const Editor = exports.ui = new BaseDialog(Core.bundle.get('code-editor', 'code editor'))

	// 为了更好看，仿写 CustomGameDialog
	Editor.clearChildren();
	Editor.add(Editor.titleTable).growX().row();
	Editor.stack(Editor.cont, Editor.buttons).grow();
	Editor.buttons.bottom();

	cont = Editor.cont;
	cont.top().defaults().padTop(0).top();

	fileName = new TextField, fileNameTable = cont.table().get()

	cont.row();
	cont.pane(cons(p => pane = p)).fillX().fillY().grow().row();

	Editor.buttons.button('$back', Icon.left, run(() => Editor.hide())).size(220, 70);

	Editor.buttons.button('$ok', Icon.ok, run(() => {
		this.parse()
		if (fileNameTable.children.size > 0) {
			let toFile = this.file.parent().child(fileName.getText() + '.' + this.file.extension());
			this.file.moveTo(toFile);
			this.file = toFile;
		}
		Editor.hide();
	})).size(220, 70);

	Editor.addCloseListener();
}

exports.edit = function (file, mod) {

	file.exists() || file.writeString('');

	this.file = file, this.mod = mod;

	if (file.extension() != 'properties') {
		if (fileNameTable.children.size == 0) {
			fileNameTable.add('$fileName')
			fileNameTable.add(fileName)
		}
		fileName.setText(file.nameWithoutExtension())
	} else fileNameTable.clearChildren()

	this.build();

	this.ui.show()
}

// 编辑代码
exports.build = function () {
	pane.clearChildren()
	result = {}
	let file = this.file
	let ext = file.extension();
	if (/^h?json$/.test(ext)) {
		let obj = result.value = IniHandle.toIntObject(IniHandle.hjsonParse(file.readString()))

		let parentName = (() => {
			let content = this.mod.file.child("content")

			let f = file
			while (!f.parent().equals(content)) {
				f = f.parent();
			}
			return ContentTypes.get(f.name())
		})()
		let typeName
		if (obj.has('type') && parentName == 'block') {
			typeName = obj.remove('type')
		} else if (types[parentName] != null && types[parentName][0] != null) {
			typeName = types[parentName][0].getSimpleName()
		} else typeName = 'none'
		if (typeName[0].toLowerCase() == typeName[0]) typeName = typeName[0].toUpperCase() + typeName.slice(1);

		let selection = new typeSelection.constructor(Classes.get(typeName), typeName, types[parentName], true);
		pane.add(selection.table).padBottom(4).row()
		Object.defineProperty(result, 'type', { get: () => selection.type })
		Object.defineProperty(result, 'typeName', { get: () => selection.typeName })

		let table = new Table(Tex.whiteui.tint(.4, .4, .4, .9))
		let fields = new Fields.constructor(obj, prov(() => selection.type), table)

		pane.table(Tex.button, cons(t => {
			t.add(table).fillX().pad(4).row();

			// 添加接口
			t.add(addBtn.constructor(result.value, fields, prov(() => Classes.get(selection.typeName)))).fillX().growX()
			t.row();
			t.table(cons(t => {
				// 研究
				var k = 'research';
				let value = obj.getDefault(k, "core-shard")
				t.add(Core.bundle.get(k, k));
				t.add(':');

				let techs = TechTree.all.toArray();

				let btn = t.button(value != '' && value != null ? value : '$none', Styles.cleart,
					() => IntFunc.showSelectTable(btn, (p, hide, v) => {
						p.clearChildren();
						p.button('$none', Styles.cleart, run(() => {
							obj.put(k, '');
							btn.setText('$none');
						}));

						let cont = p.table().get();
						let tableArr = [new Table, new Table, new Table, new Table, new Table, new Table];

						let reg = RegExp(v, 'i'), i = 0;
						let cols = Vars.mobile ? 6 : 10;

						for (let tech of techs) {
							let t = tech.content;
							if (!reg.test(t.name) && !reg.test(t.localizedName)) continue;

							let index = (t instanceof Item && "0")
								|| (t instanceof Liquid && 1)
								|| (t instanceof Block && 2)
								|| (t instanceof UnitType && 3)
								|| (t instanceof SectorPreset && 4)
								|| 5;
							let table = tableArr[index];
							let button = table.button(Tex.whiteui, Styles.clearToggleTransi, 32, () => {
								obj.put(k, t.name)
								btn.setText(t.localizedName);
								hide.run()
							}).size(42).get();
							button.getStyle().imageUp = new TextureRegionDrawable(t.uiIcon);
							button.update(() => button.setChecked(obj.getDefault(k, '') == t.name));

							if (table.children.size % cols == 0) {
								table.row();
							}
						}
						for (let i = 0; i < tableArr.length; i++) {
							let table = tableArr[i];
							cont.add(table).growX().left().row();
							if (table.children.size != 0 && i < tableArr.length - 2) {
								cont.image(Tex.whiteui, Pal.accent)
									.growX().height(3).pad(4).row();
							}
						}
					}, true)
				).size(150, 60).get();
			})).fillX();
		})).fillX().row();
		fields.map.each((k, v) => {
			if (k == 'research') return;

			fields.add(null, k);
		})
	}


	else if (ext == 'properties') {
		let str = file.readString().split('\n');
		let arr = result.value = [];
		if (str.join('') == '') str.length = 0;
		let cont = pane.table(Styles.none).padLeft(3).get();
		let fun = (from, to) => {
			let table = cont.table(Tex.button, cons(t => {
				if (from == '#') {
					t.add('#').padRight(6);
					let field = new TextField(to);
					t.add(field).fillX().get();
					arr.push({
						getName: () => '这个' + Core.bundle.get('annotation', 'annotation'),
						toString: () => '# ' + field.getText()
					})
					return;
				}
				let field1 = new TextField(from);
				t.add(field1).width(200).get();
				t.add(' = ', Color.gray);
				let field2 = new TextField(to);
				t.add(field2).width(200).get();
				arr.push({
					getName: () => field1.getText(),
					toString: () => field1.getText() + field2.getText() != '' ? field1.getText() + ' = ' + field2.getText() : ''
				})
			})).get();
			let index = arr.length - 1;
			IntFunc.doubleClick(table, [run(() => {
				Vars.ui.showConfirm('$confirm',
					Core.bundle.format('confirm.remove', arr[index].getName()),
					run(() => {
						table.remove();
						arr.splice(index, 1);
					})
				);
			}), run(() => { })]);
			cont.row();
		}
		str.forEach(e => {
			if (e == '') return;
			if (/^\s*#/.test(e)) {
				let str = e.replace(/^\s*\#\s*/, '');
				return fun('#', str);
			}
			let arr = e.split(/\s*=\s*/);
			fun(arr[0], arr[1]);
		});
		pane.row();
		pane.table(cons(btn => {
			btn.button('$add', Icon.add, () => fun('', '')).size(210, 64);
			btn.button(Core.bundle.get('content.add', 'add') + Core.bundle.get('annotation', 'annotation'), () => fun('#', '')).size(210, 64)
		}))
	}


	else {
		let area = result.value = pane.area(file.readString(), cons(t => {

		})).size(Math.min(Core.graphics.getWidth(), Core.graphics.getHeight()) - 200).get();
	}

	// 为了不阻挡最底下的部分
	pane.image().color(Color.clear).height(74)
}
// 编译代码
exports.parse = function () {
	let file = this.file
	let ext = file.extension()
	if (/^h?json$/.test(ext)) {
		let obj = result.value;
		let toClass = IntFunc.toClass, typeName = result.typeName, type = result.type
		if (type != null) {
			if (toClass(UnitType).isAssignableFrom(type) ||
				toClass(Item).isAssignableFrom(type) ||
				toClass(Liquid).isAssignableFrom(type) ||
				toClass(StatusEffect).isAssignableFrom(type)) { }

			else if (!obj.has('type')) obj.put('type', typeName)
		}

		file.writeString(obj + "");
		let dir = this.mod.file.child('content').child(
			type != null ? ((toClass(Block).isAssignableFrom(type) && 'block')
				|| (toClass(UnitType).isAssignableFrom(type) && 'unit')
				|| (toClass(Item).isAssignableFrom(type) && 'item')
				|| (toClass(Liquid).isAssignableFrom(type) && 'liquid')
				|| (toClass(StatusEffect).isAssignableFrom(type) && 'statu')
				|| type.getContentType()) + 's'
				: 'blocks'
		);
		this.file = dir.child(file.name());
		file.moveTo(this.file)
	}
	else if (ext == 'properties') {
		let arr = result.value;
		file.writeString(arr.join('\n'));
	}
	else {
		file.writeString(result.value.getText().replace(/\r/g, '\n'));
	}
}
