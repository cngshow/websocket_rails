package gov.va.rails.websocket;

import java.io.IOException;
import java.util.Observable;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

@ServerEndpoint(value = WebSocketSupport.END_POINT)
public class WebSocketSupport {

	static{
		System.out.println("Rails has WebSocketSupport at: " + WebSocketSupport.END_POINT);
	}

	public static final String END_POINT = "/websocket/rails";

    private static Optional<RailsLogging> log = Optional.empty();

    private static final Set<WebSocketSupport> connections = new CopyOnWriteArraySet<>();

    private static final IncomingMessageObserver messageNotifier = new IncomingMessageObserver();
    private static final WebSocketRemovedObserver websocketRemovedNotifier = new WebSocketRemovedObserver();

	private Session session;
	private String  initialData;

    public WebSocketSupport() {}

    @OnOpen
    public void start(Session session) {
        this.session = session;
        connections.add(this);
        log.filter(l -> l.debug("Java: WebSocketSupport session started!", null));
    }


    @OnClose
    public void end() {
        connections.remove(this);
        websocketRemovedNotifier.notifyObservers(this);
        log.filter(l -> l.debug("Java: WebSocketSupport session ended! ", null));
    }


    @OnMessage
    public void incoming(String message) {
    	messageNotifier.notifyObservers(new MessageHolder(this, message));
    }


    @OnError
    public void onError(Throwable t) throws Throwable {
        log.filter(l -> l.error("Java: WebSocketSupport Error: ", t));
    }

    public Session getSession() {
		return session;
	}

    public boolean isPresent() {
    	return connections.contains(this);
    }

    public static boolean chat(WebSocketSupport client, String msg) {
        log.filter(l -> l.debug("Java: Chat request made, " + msg, null));

    	boolean success = true;
    	try {
            synchronized (client) {
                client.getSession().getBasicRemote().sendText(msg);
            }
    	} catch (IOException e) {
            log.filter(l -> l.error("Java: WebSocketSupport Error: Failed to send message to client", e));
            connections.remove(client);
            websocketRemovedNotifier.notifyObservers();
            success = false;
            try {
                client.session.close();
            } catch (IOException e1) {
                // Ignore
            }
    	}
            return success;
    }

    public static void remove(WebSocketSupport ws) {
        connections.remove(ws);
        try {
            ws.session.close();
        } catch (IOException e1) {
            // Ignore
        }
        websocketRemovedNotifier.notifyObservers();
    }

    /**
     * Broadcasts to all websockets on the endpoint.
     * @param msg the msg to broadcast, usually in JSON
     * @return the number of websockets removed from tracking due to a lack of reachability.
     */
    public static int broadcast(String msg) {
    	int removed = 0;
        for (WebSocketSupport client : connections) {
            try {
                synchronized (client) {
                    client.session.getBasicRemote().sendText(msg);
                }
            } catch (IOException e) {
                log.filter(l -> l.error("Java: WebSocketSupport Error: Failed to send message to client", e));
                connections.remove(client);
                removed++;
                websocketRemovedNotifier.notifyObservers();
                try {
                    client.session.close();
                } catch (IOException e1) {
                    // Ignore
                }
                //If the day comes here is where we notify the world of a client leaving
            }
        }
        return removed;
    }

    public static IncomingMessageObserver getMessageNotifier() {
		return messageNotifier;
	}

    public static WebSocketRemovedObserver getWebSocketRemovedNotifier() {
    	return websocketRemovedNotifier;
    }//

    public static void setLogger(RailsLogging r) {
    	//r is a plain old ruby object
    	log = Optional.ofNullable(r);
    }

	public String getInitialData() {
		return initialData;
	}

	public void setInitialData(String channel) {
        log.filter(l -> l.debug("Java: My channel (" + this + ") has been set to " + channel, null));
		this.initialData = channel;
	}

	public static class IncomingMessageObserver extends Observable {

		@Override
		public void notifyObservers(Object o) {
    		setChanged();
    		super.notifyObservers(o);
            log.filter(l -> l.debug("Java: IncomingMessageObserver notified!", null));
    	}
    }

	public static class WebSocketRemovedObserver extends Observable {

		@Override
		public void notifyObservers(Object o) {
    		setChanged();
    		super.notifyObservers(o);
            log.filter(l -> l.debug("Java: WebSocketRemovedObserver notified!", null));
    	}
    }

	public static class MessageHolder {
		private WebSocketSupport session;
		private String message;

		public MessageHolder(WebSocketSupport s, String msg) {
			this.session = s;
			this.message = msg;
            log.filter(l -> l.debug("Java: Message holder built with " + msg, null));
		}

		public WebSocketSupport getWebSocketSupport() {
			return session;
		}

		public String getMessage() {
			return message;
		}

		public boolean chat(String msg) {
            log.filter(l -> l.debug("Java: Chatting " + msg, null));
			return WebSocketSupport.chat(getWebSocketSupport(), msg);
		}

	}
}
