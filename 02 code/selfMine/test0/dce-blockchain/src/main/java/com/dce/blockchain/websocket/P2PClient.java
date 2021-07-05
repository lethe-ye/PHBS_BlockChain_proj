package com.dce.blockchain.websocket;

import java.net.URI;
import java.net.URISyntaxException;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.dce.blockchain.web.service.P2PService;

/**
 * p2p客户端
 * 
 * @author Jared Jia
 *
 */
@Component
public class P2PClient {
	
	@Autowired
	P2PService p2pService;

	public void connectToPeer(String addr) {
		try {
			final WebSocketClient socketClient = new WebSocketClient(new URI(addr)) {
				@Override
				public void onOpen(ServerHandshake serverHandshake) {
					System.out.println("p2pclient onOpen");

					//客户端发送请求，查询最新区块
					p2pService.write(this, p2pService.queryLatestBlockMsg());
					p2pService.getSockets().add(this);
				}

				/**
				 * 接收到消息时触发
				 * @param msg
				 */
				@Override
				public void onMessage(String msg) {
					System.out.println("p2pclient onMessage");
					p2pService.handleMessage(this, msg, p2pService.getSockets());
					// TODO: 添加判断blockCache里的height是否比这个收到msg里面的高
				}

				@Override
				public void onClose(int i, String msg, boolean b) {
					p2pService.getSockets().remove(this);
					System.out.println("connection closed");
				}

				@Override
				public void onError(Exception e) {
					p2pService.getSockets().remove(this);
					System.out.println("connection failed");
				}
			};
			socketClient.connect();
		} catch (URISyntaxException e) {
			System.out.println("p2p connect is error:" + e.getMessage());
		}
	}

}
