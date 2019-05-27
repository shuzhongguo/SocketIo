package com.sunsheen.socketio;

import io.netty.channel.ChannelHandlerContext;
import io.netty.util.concurrent.Future;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.BroadcastOperations;
import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.listener.ConnectListener;
import com.corundumstudio.socketio.listener.DataListener;
import com.corundumstudio.socketio.listener.ExceptionListenerAdapter;

public class SocketServer{
	final static Logger logger = LoggerFactory.getLogger(SocketServer.class);

	private final SocketIOServer server;
	private SocketServer(String hostname, String port, String eventName){
		Configuration config = new Configuration();
		config.setHostname(hostname);
		config.setPort(Integer.parseInt(port));
		
		DisConnectionException exception = new DisConnectionException();
		config.setExceptionListener(exception);
		
		this.server = new SocketIOServer(config);
		addConnectListener(eventName);
		addEventListener(eventName);
	}

	private void addConnectListener(final String eventName) {
		server.addConnectListener(new ConnectListener() {
			
			public void onConnect(SocketIOClient client) {
				logger.info(client.getRemoteAddress() + " web客户端接入");
				System.out.println(client.getRemoteAddress() + " web客户端接入");
				client.sendEvent(eventName, "Welcome connection");
			}
		});
	}

	private void addEventListener(final String eventName) {
		server.addEventListener(eventName, Map.class, new DataListener<Map>() {

			public void onData(SocketIOClient client, Map data, AckRequest ackSender) throws Exception {
				BroadcastOperations broadcastOperations = server
						.getBroadcastOperations();
				for (Object object : data.keySet()) {
					System.out.println(object.toString() +  " " + data.get(object));
				}
				broadcastOperations.sendEvent(eventName, data);
			}
		});
	}
	private void start() {
		server.start();
	}	
	class DisConnectionException extends ExceptionListenerAdapter{

		@Override
		public boolean exceptionCaught(ChannelHandlerContext ctx, Throwable e)
				throws Exception {
			logger.error(e.getMessage());
			ctx.close();
			return true;
		}
		
	}
	
	public static void main(String[] args) {
		
		SocketServer socketIOServer = new SocketServer(args[0],args[1],args[2]);
//		SocketServer socketIOServer = new SocketServer("192.168.137.43","10050","szgtest111");
		socketIOServer.start();
		try {
			Thread.sleep(Integer.MAX_VALUE);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	
	}
}
