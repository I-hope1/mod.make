package modmake.components.build.inspect;

import arc.struct.ObjectMap;
import mindustry.Vars;

import java.awt.*;

import static rhino.ScriptRuntime.toNumber;

public class IKeys extends ObjectMap<String, Inspect> {
	{
		put("size", o -> {
			Double d = toNumber(o);
			int i = d.intValue();
			if (i > 0 && i <= Vars.maxBlockSize) return IExceptionType.NONE;
			return IExceptionType.ERROR;
		});
	}

	public static class IKeyException extends Exception {
		public Color color;
		public IKeyException(String message) {
			super(message);
		}
	}
}
