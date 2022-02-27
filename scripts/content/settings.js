
function newObj(obj, key, name, defaultValue) {
	obj[key] = {
		"name": name,
		value: Core.settings.get(modName + key, defaultValue),
		checkbox: null,
		set(value){
			this.value = value;
			Core.settings.put(modName + key, value);
			this.checkbox.setChecked(value);
		}
	}
}

function getValue(objName, key) {
	return object['obj_' + objName][key].value;
}
function setValue(objName, key, value) {
	object['obj_' + objName][key].set(value);
}

let object = {
	name: 'settings', tables: {},
	obj_base: {},
	load() {
		newObj(this.obj_base, "display-content-sprite", "显示content图片", true)
		newObj(this.obj_base, "not_show_again", "不再显示更新日志", false)


		let dialog = this.ui = new BaseDialog("设置");
		let cont
		dialog.cont.pane(cons(p => cont = p.table().width(400).get())).fillX()
		cont.left().defaults().left()
		cont.add("基础").color(Pal.accent).row()
		cont.table(cons(t => {
			t.left().defaults().left()
			let obj1 = this.obj_base
			for (let k in obj1) {
				obj1[k].checkbox = t.check(obj1[k].name, obj1[k].value,
					boolc(b => obj1[k].set(b))
				).get();
				t.row()
			}
		})).fillX().padLeft(16);
		dialog.addCloseButton();
	},
	buildConfiguration() {
		this.ui.show();
	}
}
contArr.push(object)

exports.setValue = setValue;
exports.getValue = getValue;