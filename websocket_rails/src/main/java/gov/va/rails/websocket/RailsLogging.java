package gov.va.rails.websocket;

public interface RailsLogging {

	boolean trace(String msg, Throwable t);
	boolean debug(String msg, Throwable t);
	boolean info(String msg, Throwable t);
	boolean warn(String msg, Throwable t);
	boolean error(String msg, Throwable t);
	boolean fatal(String msg, Throwable t);
	boolean always(String msg, Throwable t);
	
}
