package modmake.components.build.inspect;

import modmake.components.build.inspect.exceptions.BaseException;

public interface Inspect {
	BaseException get(Object o);
}
