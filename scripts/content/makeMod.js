
const buildContent = require('func/buildContent');
const Editor = require('ui/dialogs/Editor');
const IntModsDialog = require('ui/dialogs/ModsDialog')

// const Contents = Vars.content.blocks().toArray().concat(Vars.content.liquids().toArray()).concat(Vars.content.items());

contArr.push({
	name: 'makeMod', needFi: true,

	load() {
		buildContent.load()
		IntModsDialog.load(this.name)
		Editor.load()
	},
	buildConfiguration(table) {
		IntModsDialog.show()
	}
});