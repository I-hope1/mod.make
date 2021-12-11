
exports.cont = {
	name: 'settings', tables: {},
	load() {
		let ui = this.ui = new BaseDialog('$settings')
		if (!Core.settings.has(modName + '-load-dummy')) Core.settings.put(modName + '-load-dummy', false)
		let arr = ['tester', 'makeMod', 'dummy', 'other'], arr2 = ['lastlog', 'unit_spawn', 'showcrashes', 'select', 'showicon']
		let cont = ui.cont.table().width(400).get()
		cont.add('load').color(Pal.accent).growX().left().row();
		cont.table(cons(t => {
			t.left().defaults().left()
			arr.forEach(str => {
				t.check(str, Core.settings.get(modName + '-load-' + str, true), boolc(b => Core.settings.put(modName + '-load-' + str, b)
				)).row()
			})
			t.table(cons(t => {
				t.left().defaults().left()
				arr2.forEach(str => {
					t.check(str, Core.settings.get(modName + '-load-' + str, false), boolc(b => Core.settings.put(modName + '-load-' + str, b)
					))
					.disabled(boolf(() => !Core.settings.get(modName + '-load-other', true))).row()
				})
			})).growX().left().padLeft(10)
		})).growX().left().padLeft(16).row()

		this.cont = cont;
		this.ui.addCloseButton()
	},
	add(name, table) {
		this.cont.add(name).color(Pal.accent).growX().left().row();
		this.cont.add(table).growX().left().padLeft(16).row()
	},
	buildConfiguration(){
		this.ui.show()
	}
}