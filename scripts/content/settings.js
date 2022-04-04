
const { caches: { settings } } = require('func/IniHandle')
const mod_Name = modName

function newObj(objName, key, name, defaultValue, info) {
	return object['obj_' + objName][key] = {
		"name": name,
		value: settings.get(mod_Name + key) || defaultValue,
		get() { return !!this.value },
		checkbox: null, "info": info,
		set(value) {
			this.value = value;
			settings.set(mod_Name + key, value);
			this.checkbox.setChecked(value);
		}
	}
}

function getValue(objName, key) {
	return !object['obj_' + objName].disabled && object['obj_' + objName][key].get();
}
function setValue(objName, key, value) {
	object['obj_' + objName][key].set(value);
}

let object = {
	name: 'settings', tables: {},
	obj_base: {},
	obj_loadMod: {},
	load() {
		let dialog = this.ui = new BaseDialog("设置");
		let cont
		dialog.cont.pane(cons(p => cont = p.table().width(400).get())).fillX()
		cont.left().defaults().left()
		cont.add("基础").color(Pal.accent).row()
		cont.table(cons(t => {
			t.left().defaults().left()
			let obj1 = this.obj_base
			for (let k in obj1) {
				let setting = obj1[k];
				if (typeof setting.info == "string") t.add(setting.info).padTop(4).row();
				setting.checkbox = t.check(setting.name, setting.get(),
					boolc(b => setting.set(b))
				).disabled(() => !!obj1.disabled).get();
				t.row()
			}
		})).fillX().padLeft(16).row();
		cont.add("加载mod").color(Pal.accent).row()
		cont.table(cons(t => {
			t.left().defaults().left()
			let obj1 = this.obj_loadMod
			for (let k in obj1) {
				let setting = obj1[k];
				if (typeof setting.info == "string") t.add(setting.info).padTop(4).row();
				setting.checkbox = t.check(setting.name, setting.get(),
					boolc(b => setting.set(b))
				).disabled(() => !!obj1.disabled).get();
				t.row()
			}
		})).fillX().padLeft(16);
		dialog.addCloseButton();
	},
	buildConfiguration() {
		this.ui.show();
	}
}

newObj("base", "display-content-sprite", "显示content图片", true)
newObj("base", "display_mod_logo", "显示mod的logo", false)
newObj("base", "auto_fold_code", "自动折叠代码", false)
newObj("base", "display_deprecated", "显示禁用类(字段)", false, "需要重启")
newObj("base", "point_out_unknown_field", "指出未知字段", false)
newObj("base", "not_show_again", "不再显示更新日志", false)
newObj("base", "auto_load_mod", "自动加载mod", false, "清除所有content，加载此mod\n[red](不会导入到游戏的mods文件夹，实验性)")
newObj("loadMod", "load_sprites", "加载图集", false, "未完成(存在bug)")
Object.defineProperty(object.obj_loadMod, 'disabled', {
	get: () => !getValue("base", "auto_load_mod")
})

contArr.push(object)

exports.setValue = (name, key, value) => setValue(name, key, value);
exports.getValue = (name, key) => getValue(name, key);