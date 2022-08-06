package modmake.components.constructor;

import arc.util.serialization.JsonValue;

public class MyJsonValue extends JsonValue {
	public MyJsonValue(ValueType type) {
		super(type);
	}

	public MyJsonValue(String value) {
		super(value);
	}

	public MyJsonValue(double value) {
		super(value);
	}

	public MyJsonValue(long value) {
		super(value);
	}

	public MyJsonValue(double value, String stringValue) {
		super(value, stringValue);
	}

	public MyJsonValue(long value, String stringValue) {
		super(value, stringValue);
	}

	public MyJsonValue(boolean value) {
		super(value);
	}

	@Override
	public String toString() {
		return super.toString();
	}
}
