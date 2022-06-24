package modmake.components.build.inspect.exceptions;

import modmake.components.build.inspect.IExceptionType;

public class BaseException  {
	public IExceptionType type;
	public String message;
	public BaseException(String message, IExceptionType type) {
		this.message = message;
		this.type = type;
	}
}
