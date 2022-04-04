
const buildContent = require('func/buildContent')
const { MyObject, MyArray } = require('func/constructor')

exports.colorfulTable = (i, cons) => {
	return new Table(i % 3 == 0 ? Tex.whiteui.tint(0, .8, .8, .6) :
		i % 3 == 1 ? Tex.whiteui.tint(.8, .8, 0, .6) :
			Tex.whiteui.tint(.8, 0, .8, .6), cons)
}

exports.json = function (fields, i, key) {
	return this.colorfulTable(i, table => {
		table.left().defaults().left();
		buildContent.build(fields.type, fields, table, key, fields.map.get(key))
	})
}

exports.constructor = function (value, type, table) {
	if (value == null) throw Error("'value' can't be null");

	if (value instanceof MyArray || value instanceof MyObject) {
		this.map = value
	} else throw new TypeError("'" + value + "' is not MyArray or MyObject")

	this.table = table
	Object.defineProperty(this, 'type', { get: () => type.get() })
	let i = 0;
	this.add = function (table, key, value) {
		if (value != null && !this.map.has(key)) {
			this.map.put(key, value)
		}
		let t = table || exports.json(this, i++, key)
		this.table.add(t).fillX().row()
	}
	this.remove = function (item, key) {
		this.map.remove(key)
		if (item != null) item.remove()
		else Log.err("can't remove key: " + key)
	}
}