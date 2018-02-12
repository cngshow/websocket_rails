package gov.va.rails.websocket;

import java.io.IOException;
import java.util.Observable;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;

@ServerEndpoint(value = WebSocketSupport.END_POINT)
public class WebSocketSupport {
	
	static{
		System.out.println("Rails has WebSocketSupport at:: " + WebSocketSupport.END_POINT);
	}

	public static final String END_POINT = "/websocket/rails"; 
	
    private static final Log log = LogFactory.getLog(WebSocketSupport.class);

    private static final Set<WebSocketSupport> connections =
            new CopyOnWriteArraySet<>();
    
    private static final IncomingMessageObserver messageNotifier = new IncomingMessageObserver();
    private static final WebSocketRemovedObserver websocketRemovedNotifier = new WebSocketRemovedObserver();

	private Session session;
	private String channel;

    public WebSocketSupport() {}

    @OnOpen
    public void start(Session session) {
        this.session = session;
        connections.add(this);
        log.debug("WebSocketSupport session started!");
        System.out.println("WebSocketSupport session started!");
    }


    @OnClose
    public void end() {
        connections.remove(this);
        websocketRemovedNotifier.notifyObservers(this);
        log.debug("WebSocketSupport session ended! ");
        System.out.println("WebSocketSupport session ended! ");
    }


    @OnMessage
    public void incoming(String message) {
    	messageNotifier.notifyObservers(new MessageHolder(this, message));
    }


    @OnError
    public void onError(Throwable t) throws Throwable {
        log.error("WebSocketSupport Error: " + t.toString(), t);
        System.out.println("WebSocketSupport Error: " + t.toString());
        t.printStackTrace();
    }
    
    public Session getSession() {
		return session;
	}
    
    public boolean isPresent() {
    	return connections.contains(this);
    }
    
    public static boolean chat(WebSocketSupport client, String msg) {
    	System.out.println("Chat request made, " + msg);
    	boolean success = true;
    	try {
            synchronized (client) {
                client.getSession().getBasicRemote().sendText(msg);
            }
    	} catch (IOException e) {
            log.error("WebSocketSupport Error: Failed to send message to client", e);
            System.out.println("WebSocketSupport Error: Failed to send message to client");
            e.printStackTrace();
            connections.remove(client);
            websocketRemovedNotifier.notifyObservers();
            success = false;
            try {
                client.session.close();
            } catch (IOException e1) {
                // Ignore
            }
    	}
        	System.out.println("Chat request success? " + success);
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
                log.error("WebSocketSupport Error: Failed to send message to client", e);
                System.out.println("WebSocketSupport Error: Failed to send message to client");
                e.printStackTrace();
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
    }

	public String getChannel() {
		return channel;
	}

	public void setChannel(String channel) {
		System.out.println("My channel has been set to " + channel);
		this.channel = channel;
	}

	public static class IncomingMessageObserver extends Observable {
    	
		@Override
		public void notifyObservers(Object o) {
    		setChanged();
    		super.notifyObservers(o);
    		System.out.println("IncomingMessageObserver notified!");
    	}
    }
	
	public static class WebSocketRemovedObserver extends Observable {
    	
		@Override
		public void notifyObservers(Object o) {
    		setChanged();
    		super.notifyObservers(o);
    		System.out.println("WebSocketRemovedObserver notified!");
    	}
    }
	
	public static class MessageHolder {
		private WebSocketSupport session;
		private String message;

		public MessageHolder(WebSocketSupport s, String msg) {
			this.session = s;
			this.message = msg;
			System.out.println("Message holder built with " + msg);
		}

		public WebSocketSupport getWebSocketSupport() {
			return session;
		}

		public String getMessage() {
			return message;
		}
	
		public boolean chat(String msg) {
			System.out.println("I am responding with this chat back:");
			System.out.println(msg);
			System.out.println("--------------------------------------");
			return WebSocketSupport.chat(getWebSocketSupport(), msg);
		}

	}
}
