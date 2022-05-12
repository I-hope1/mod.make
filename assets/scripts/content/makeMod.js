
const buildContent = require('func/buildContent');
const Editor = require('ui/dialogs/Editor');
const IntModsDialog = require('ui/dialogs/ModsDialog')
const Fields = require('ui/components/Fields');
const findClass = require('func/findClass')
const ModMakeContent = findClass("ui.content.ModMakeContent")

// const Contents = Vars.content.blocks().toArray().concat(Vars.content.liquids().toArray()).concat(Vars.content.items());

new ModMakeContent(() => {
	Fields.load();
	IntModsDialog.load(this.name)
	Editor.load()
	buildContent.load()
}, () => {
	IntModsDialog.show()
});