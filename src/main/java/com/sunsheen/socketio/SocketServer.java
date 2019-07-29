package com.sunsheen.socketio;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.concurrent.Future;
import io.netty.util.internal.StringUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.BroadcastOperations;
import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIONamespace;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.listener.ConnectListener;
import com.corundumstudio.socketio.listener.DataListener;
import com.corundumstudio.socketio.listener.ExceptionListenerAdapter;

public class SocketServer {
	final static Logger logger = LoggerFactory.getLogger(SocketServer.class);

	private SocketIOServer server;
	private static String socketPort = "10020";
	private static String socketHost = "localhost";
	private final static String EVENTMANAGER = "eventManager";
	// 私有化
	private static SocketServer socketServer;

	private SocketServer() {

	}

	private SocketServer(String hostname, String port) {
		Configuration config = new Configuration();
		config.setHostname(hostname);
		config.setPort(Integer.parseInt(port));
		DisConnectionException exception = new DisConnectionException();
		config.setExceptionListener(exception);
		this.server = new SocketIOServer(config);
	}

	// 实现单例中的 懒汉模式
	public static SocketServer getSocketServer(String hostname, String port) {
		if (socketServer == null) {
			synchronized (SocketServer.class) {
				if (socketServer == null) {
					socketServer = new SocketServer(hostname, port);
				}
			}
		}
		return socketServer;
	}

	private void addConnectListener(final String eventName) {
		server.addConnectListener(new ConnectListener() {

			public void onConnect(SocketIOClient client) {
//				logger.info(client.getRemoteAddress() + " web客户端接入");
				System.out.println(client.getRemoteAddress() + " web客户端接入");
				System.out.println("======================>" + eventName);
				client.sendEvent(eventName, "Welcome connection");
			}
		});
	}

	private void addEventListener(final String eventName) {
		server.addEventListener(eventName, Map.class, new DataListener<Map>() {
			public void onData(SocketIOClient client, Map data, AckRequest ackSender) throws Exception {
				if (eventName.equalsIgnoreCase("eventManager")) {
					Map<String, Object> result = new HashMap<String, Object>();
					try {
						Set keySet = data.keySet();
						for (Object object : keySet) {
							if (object.toString().equals("add")) {
								Object object2 = data.get(object);
								addConnectListener(object2.toString());
								addEventListener(object2.toString());
								result.put("messge", "添加事件监听成功");
								result.put("add", "true");
								client.sendEvent(eventName, result);
							} else if (object.toString().equals("remove")) {
								Object object2 = data.get(object);
								server.removeAllListeners(object2.toString());
								result.put("messge", "移除事件" + object2.toString() + "所有监听成功");
								result.put("remove", "true");
								client.sendEvent(eventName, result);
							} else {
								result.put("message", "该事" + eventName + "件只提供添加对其他添加事件的监听及移除监听");
								client.sendEvent(eventName, result);
							}
						}
					} catch (Exception e) {
						result.put("message", "操作异常:" + e.getMessage());
						client.sendEvent(eventName, result);
						e.printStackTrace();
					}
				} else {
					BroadcastOperations broadcastOperations = server.getBroadcastOperations();
					broadcastOperations.sendEvent(eventName, data);
				}
			}
		});
	}

	public void start() {
		server.start();
	}

	class DisConnectionException extends ExceptionListenerAdapter {

		@Override
		public boolean exceptionCaught(ChannelHandlerContext ctx, Throwable e) throws Exception {
			logger.error(e.getMessage());
			ctx.close();
			return true;
		}

	}

	public static SocketServer initSocketServer(InputStream in) {
		Properties pros = new Properties();
		try {
			pros.load(in);
			if (!StringUtil.isNullOrEmpty(pros.getProperty("socketPort"))) {
				socketPort = pros.getProperty("socketPort");
			}
			if (!StringUtil.isNullOrEmpty(pros.getProperty("socketHost"))) {
				socketHost = pros.getProperty("socketHost");
			}
			socketServer = SocketServer.getSocketServer(socketHost, socketPort);
			String eventManager = pros.getProperty(EVENTMANAGER);
			if (!StringUtil.isNullOrEmpty(eventManager)) {
				socketServer.addConnectListener(eventManager);
				socketServer.addEventListener(eventManager);
			}else {
				socketServer.addConnectListener(EVENTMANAGER);
				socketServer.addEventListener(EVENTMANAGER);
			}
			String event_str = pros.getProperty("eventName");
			if (!StringUtil.isNullOrEmpty(event_str)) {
				String[] events = event_str.split(",");
				for (String event : events) {
					socketServer.addConnectListener(event);
					socketServer.addEventListener(event);
				}
			}
			in.close();
			return socketServer;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static void main(String[] args) {
		String path = "socketServer.properties";
		InputStream in = SocketServer.class.getClassLoader().getResourceAsStream(path);
		if (args.length == 0) {
			System.out.println("请输入文件路径");
		} else if(args.length > 0){
			path = args[0];
			try {
				in = new FileInputStream(path);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}
		SocketServer socketServer = SocketServer.initSocketServer(in);
		socketServer.start();
		try {
			Thread.sleep(Integer.MAX_VALUE);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

	}

}
