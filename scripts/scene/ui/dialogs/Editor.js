const IntFunc = require('func/index');
const IntStyles = require('scene/styles');
const Fields = require('scene/ui/components/Fields');
const scripts = require('scene/ui/scripts');
const addBtn = require('scene/ui/components/addFieldBtn');

const Classes = Packages.mindustry.mod.ClassMap.classes;

let fileName, fileNameTable,
	cont, pane, result = {};

exports.contentTypes = [];
const types = exports.types = {};

exports.load = function () {
	for (let type of ContentType.all) {
		let arr = Vars.content.getBy(type);
		if (!arr.isEmpty()) {
			let c = arr.first().getClass();
			//get base content class, skipping intermediates
			while (!(c.getSuperclass() == Content || c.getSuperclass() == UnlockableContent || Packages.java.lang.reflect.Modifier.isAbstract(c.getSuperclass().getModifiers()))) {
				c = c.getSuperclass();
			}
			this.contentTypes.push(c, type);
		}
	}

	for (let i = 0; i < this.contentTypes.length; i += 2) {
		this.types[this.contentTypes[i + 1]] = []
	}
	Classes.each(new Cons2({get:(k, type) => {
		for (let i = 0; i < this.contentTypes.length; i += 2) {
			if (this.contentTypes[i].isAssignableFrom(type)) {
				this.types[this.contentTypes[i + 1]].push(type);
				break;
			}
		}
	}}));


	const Editor = exports.ui = new BaseDialog(Core.bundle.get('code-editor', 'code editor'))

	cont = Editor.cont;
	cont.top().defaults().padTop(0).top();

	fileName = new TextField, fileNameTable = cont.table().get()

	cont.row();
	cont.pane(cons(p => pane = p)).fillX().fillY().grow().row();

	Editor.buttons.button('$back', Icon.left, run(() => Editor.hide())).size(220, 70);

	Editor.buttons.button('$ok', Icon.ok, run(() => {
		// try {
		this.parse()
		/* } catch (e) {
			Vars.ui.showErrorMessage(e);
		} */
		if (fileNameTable.size) {
			let toFile = this.file.parent().child(fileName.getText() + '.' + this.file.extension());
			this.file.moveTo(toFile);
		}
		Editor.hide();
	})).size(220, 70);

	Editor.addCloseListener();
}


exports.edit = function (file, mod) {
	Vars.ui.loadfrag.show()

	file.exists() || file.writeString('');

	this.file = file, this.mod = mod;
	let ext = file.extension();

	if (ext != 'properties') {
		if (fileNameTable.children.size == 0) {
			fileNameTable.add('$fileName')
			fileNameTable.add(fileName)
		}
		fileName.setText(file.nameWithoutExtension())
	} else fileNameTable.clearChildren()

	this.build()

	this.ui.show()
	Vars.ui.loadfrag.hide()
}

// 编辑代码
exports.build = function () {
	pane.clearChildren()
	result = {}
	let file = this.file
	let ext = file.extension();
	if (/^h?json$/.test(ext)) {
		let obj = result.value = IntFunc.HjsonParse(file.readString())

		let parentname = file.parent().name();
		let type = parentname.slice(0, -1);
		if (obj.get('type') != null && parentname == 'blocks') {
			result.type = obj.get('type')
			obj.remove('type')
		}
		else if (types[type] != null && types[type][0] != null) {
			result.type = types[type][0].getSimpleName()
		} else result.type = 'none'
		result.Class = Classes.get(result.type)

		// type接口
		pane.table(Tex.clear, cons(t => {
			t.add('$type').padRight(2);
			let button = new Button(IntStyles.clearb);
			t.add(button).size(190, 40);
			button.label(() => Core.bundle.get(result.type.toLowerCase(), result.type)).center().grow().row();
			button.image().color(Color.gray).fillX();
			button.clicked(run(() => IntFunc.showSelectTable(button, (p, hide, v) => {
				p.clearChildren()
				let reg = RegExp('' + v, 'i')
				for (let k in types) {
					p.add(k, Pal.accent).growX().left().row()
					p.image().color(Pal.accent).fillX().row()

					types[k].forEach(t => {
						if (v != '' && !reg.test(t.getSimpleName())) return;
						p.button(Core.bundle.get(t.getSimpleName().toLowerCase(), t.getSimpleName()), Styles.cleart, run(() => {
							fields.type = result.Class = t
							result.type = t.getSimpleName();
							hide.run();
						})).pad(5).size(200, 65).disabled(result.Class == t).row();
					})
				}

				p.add('other', Pal.accent).growX().left().row()
				p.image().color(Pal.accent).fillX().row()
				p.button(Core.bundle.get('none', 'none'), Styles.cleart, run(() => {
					result.type = 'none';
					hide.run();
				})).pad(5).size(200, 65).disabled(result.type == 'none').row()
			}, true)));
		})).fillX().row();

		let table = new Table(Tex.whiteui.tint(.4, .4, .4, .9))
		let fields = new Fields.constructor(result.value, result.Class, table)
		pane.table(Tex.button, cons(t => {
			t.add(table).fillX().pad(4).row();

			// 添加接口
			t.add(addBtn.constructor(result.value, fields, prov(() => Classes.get(result.type)))).fillX().growX()
			t.row();
			t.table(cons(t => {
				/* if (result.type instanceof UnitType) {
					var k = 'constructor';
					t.add(Core.bundle.get(k, k));
					t.add(':');

					let btn = t.button(obj[k] != '' && obj[k] != null ? obj[k] : Core.bundle.get('none', 'none'), Styles.cleart, () => IntFunc.showSelectTable(btn, (p, hide, v) => {
						p.clearChildren();
						
						IntVars.unitConstructor.forEach(val =>
							p.button(Core.bundle.get(val, val), Styles.cleart, () => obj[k] = val)
						})
					})
				} */
				// 研究
				var k = 'research';
				let value = obj.get(k)
				t.add(Core.bundle.get(k, k));
				t.add(':');

				let techs = TechTree.all.toArray();

				let btn = t.button(value != '' && value != null ? value : '$none', Styles.cleart,
					run(() => IntFunc.showSelectTable(btn, (p, hide, v) => {
						p.clearChildren();
						p.button('$none', Styles.cleart, run(() => {
							obj.put(k, '');
							btn.setText('$none');
						}));

						let cont = p.table().get();
						let tableArr = [new Table, new Table, new Table, new Table, new Table, new Table];

						let reg = RegExp(v, 'i'), i = 0;

						for (let tech of techs) {
							let t = tech.content;
							if (!reg.test(t.name) && !reg.test(t.localizedName)) continue;

							let table = tableArr[
								t instanceof Item ? 0 :
								t instanceof Liquid ? 1 :
								t instanceof Block ? 2 :
								t instanceof UnitType ? 3 :
								t instanceof SectorPreset ? 4 :
								5
							];
							let button = table.button(Tex.whiteui, Styles.clearToggleTransi, 32, () => {
								btn.setText(obj.put(k, t.name));
								hide.run()
							}).size(40).get();
							button.getStyle().imageUp = new TextureRegionDrawable(t.uiIcon);
							button.update(() => button.setChecked(obj.get(k) == t.name));

							if (table.children.size % (Vars.mobile ? 6 : 10) == 0) {
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
					)).size(150, 60).get();
			})).fillX();
		})).fillX().row();
		obj.each((k, v) => {
			if (k == 'research') return;
			try {
				fields.add(null, k);
			} catch(e) { return }
		})
	}


	else if (ext == 'properties') {
		let str = file.readString().split('\n');
		let arr = result.value = [];
		if (str.join('') == '') str.length = 0;
		Log.info(str.join('\n'))
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


	else if (ext == 'js') {
		pane.top().defaults().top().grow();

		let cont = pane.table().get();
		pane.row();
		let btn = pane.button('$add', Icon.add, run(() => IntFunc.showSelectTable(btn, (p, hide) => {
			let cols = 2, c = 0;
			p.left();
			Object.keys(scripts).forEach(k => {
				if (k == 'build') return;

				p.button('$scripts.' + k, Styles.cleart, run(() => {
					cont.add((new scripts[k]).build()).row();
				})).size(440 / 2, 60);
				if (++c % cols == 0) p.row();
			});
		}))).size(220, 75).bottom().get();
		pane.row();
	}


	else {
		let area = result.value = pane.area(file.readString(), cons(t => {

		})).size(Math.min(Core.graphics.getWidth(), Core.graphics.getHeight()) - 200)
			.get();
		/*area.changed(run(() => {
			nums
		}));*/
		//area.clicked(run(() => IntFunc.showTextArea(result.value)));
	}
}
// 编译代码
exports.parse = function () {
	let file = this.file
	let ext = file.extension()
	if (ext == 'json' || ext == 'hjson') {
		let arr = [], obj = result.value;
		let toClass = IntFunc.toClass, type = result.type, Class = result.Class
		if (result.type != null){
			let str = type
			if (toClass(Item).isAssignableFrom(Class)) str = 'Item'
			if (toClass(Liquid).isAssignableFrom(Class)) str = 'Liquid'

			arr.push('type: ' + str)
		}

		file.writeString(arr.join('\n') + '\n' + obj);
		let dir = this.mod.file.child('content').child(
			type != 'none' ?
				(IntFunc.toClass(Block).isAssignableFrom(Class)  ? 'block' :
				IntFunc.toClass(UnitType).isAssignableFrom(Class) ? 'unit' :
				IntFunc.toClass(Liquid).isAssignableFrom(Class) ? 'liquid' :Class.getContentType()) + 's'
				: 'blocks'
		);
		this.file = dir.child(file.name());
		file.moveTo(this.file)

		/* let strs = [], obj = result.value;
		for(let k in obj){
			strs.push('\t"' + k + '": "' + obj[k] + '"');
		}
		file.writeString('{\n' + strs.join(',\n') + '\n}'); */
	}
	else if (ext == 'properties') {
		let arr = result.value;
		Log.info(file)
		file.writeString(arr.join('\n'));
	}
	else {
		file.writeString(result.value.getText().replace(/\r/g, '\n'));
	}
}
