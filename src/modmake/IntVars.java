package modmake;

import arc.files.Fi;
import mindustry.Vars;
import mindustry.mod.Mods.LoadedMod;

public class IntVars {
	public static String modName = "mod-make";
	public static Fi data = Vars.mods.locateMod(modName).root.child("data");
	public static LoadedMod mod = Vars.mods.locateMod(modName);
}
