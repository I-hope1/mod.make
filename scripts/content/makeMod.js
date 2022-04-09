
const buildContent = require('func/buildContent');
const Editor = require('ui/dialogs/Editor');
const IntModsDialog = require('ui/dialogs/ModsDialog')
const Fields = require('ui/components/Fields');

// const Contents = Vars.content.blocks().toArray().concat(Vars.content.liquids().toArray()).concat(Vars.content.items());

contArr.push({
	name: 'makeMod', needFi: true,

	load() {
		Fields.load();
		IntModsDialog.load(this.name)
		Editor.load()
		buildContent.load()
	},
	buildConfiguration(table) {
		IntModsDialog.show()
	}
});