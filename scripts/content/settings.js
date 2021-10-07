
exports.cont = {
	name: 'settings',
	load() {
		let ui = this.ui = new BaseDialog('$settings')
		let arr = ['tester', 'makeMod', 'other'], arr2 = ['lastlog', 'unit_spawn', 'showcrashes', 'select']
		let cont = ui.cont.table().width(400).get()
		cont.add('load').growX().left().row();
		cont.table(cons(t => {
			t.left().defaults().left()
			arr.forEach(str => {
				t.check(str, Core.settings.get(modName + '-load-' + str, true), boolc(b => Core.settings.put(modName + '-load-' + str, b)
				)).row()
			})
			t.table(cons(t => {
				t.left().defaults().left()
				arr2.forEach(str => {
					t.check(str, Core.settings.get(modName + '-load-' + str, true), boolc(b => Core.settings.put(modName + '-load-' + str, b)
					))
					.disabled(boolf(() => !Core.settings.get(modName + '-load-other', true))).row()
				})
			})).growX().left().padLeft(10)
		})).growX().left().padLeft(16)
		this.ui.addCloseButton()
	},
	buildConfiguration(){
		this.ui.show()
	}
}