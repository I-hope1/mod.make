
const buildContent = require('func/buildContent')
const findClass = require('func/findClass')
const { settings } = findClass("components.dataHandle");
const { MyObject, MyArray } = require("func/constructor")


let backgrounds;
function Fields(value, type, group) {
	if (value == null) throw Error("'value' can't be null");

	if (value instanceof MyObject) {
		this.map = value
	} else throw new TypeError("'" + value + "' is not MyArray or MyObject")

	this.group = group
	Object.defineProperty(this, 'type', { get: () => type.get() })
	let i = 0;
	let data = new Map();
	this.add = function (table, key, value) {
		if (value != null && !this.map.has(key)) {
			this.map.put(key, value)
		}
		let t = table || Fields.json(this, i++, key)
		t.name = key
		t.defaults().fillX()
		data.set(key, this.group.add(t).fillX());

		this.group.row()
	}
	this.remove = function (key) {
		this.map.remove(key)
		let cell = data.get(key)
		data.delete(key)
		if (cell != null && cell.get() != null) cell.get().remove()
		else Log.err("can't remove key: " + key)
	}
	this.setTable = function(key, table){
		let cell = data.get(key)
		if (cell != null) {
			cell.setElement(table)
		}
	}
}
Fields.load = function () {
	backgrounds = [
		Tex.whiteui.tint(.2, .6, .2, 1),
		Tex.whiteui.tint(.6, .2, .6, 1),
		Tex.whiteui.tint(.6, .6, .2, 1),
		Tex.whiteui.tint(.2, .6, .6, 1),
	]
}
Fields.build = (i, _cons) => {
	let bg;
	if (settings.getBool("colorful_table")) {
		bg = backgrounds[i % 4]
	} else {
		bg = Tex.pane;
	}
	return new Table(bg, _cons)
}

Fields.json = function (fields, i, key) {
	return this.build(i, table => {
		table.left().defaults().left();
		buildContent.build(fields.type, fields, table, key, fields.map.get(key))
	})
}

module.exports = Fields;