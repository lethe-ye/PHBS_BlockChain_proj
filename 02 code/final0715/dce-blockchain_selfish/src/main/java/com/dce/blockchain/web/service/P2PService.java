package com.dce.blockchain.web.service;

import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.java_websocket.WebSocket;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import com.alibaba.fastjson.JSON;
import com.dce.blockchain.web.model.Block;
import com.dce.blockchain.web.model.Message;
import com.dce.blockchain.web.util.BlockCache;
import com.dce.blockchain.web.util.BlockConstant;
import com.dce.blockchain.websocket.P2PClient;
import com.dce.blockchain.websocket.P2PServer;

/**
 * p2p网络服务类
 * 
 * @author Jared Jia
 *
 */
@Service
public class P2PService implements ApplicationRunner {
	
	@Autowired
	BlockService blockService;
	
	@Autowired
	BlockCache blockCache;
	
	@Autowired
	P2PServer p2PServer;
	
	@Autowired
	P2PClient p2PClient;

	@Value("${block.selfish}")
	private boolean selfish;

	@Value("#{'${address.whitelist}'.split(',')}")
	private List<String> whiteList;

	private boolean fork=false;

	public boolean getFork() {
		return fork;
	}

	public void setFork(boolean tf) {
		this.fork = tf;
	}

	/**
	 * 客户端和服务端共用的消息处理方法
	 * @param webSocket
	 * @param msg
	 * @param sockets
	 */
	public void handleMessage(WebSocket webSocket, String msg, List<WebSocket> sockets) {
		try {
			Message message = JSON.parseObject(msg, Message.class);
			System.out.println("接收到IP地址为：" +webSocket.getRemoteSocketAddress().getAddress().toString()
					+"，端口号为："+ webSocket.getRemoteSocketAddress().getPort() + "的p2p消息："
			        + JSON.toJSONString(message));
			switch (message.getType()) {
			//客户端请求查询最新的区块:1
			case BlockConstant.QUERY_LATEST_BLOCK:
				write(webSocket, responseLatestBlockMsg());//服务端调用方法返回最新区块:2
				break;
			//接收到服务端返回的最新区块:2
			case BlockConstant.RESPONSE_LATEST_BLOCK:
				handleBlockResponse(message.getData());
				break;
			//客户端请求查询整个区块链:3
			case BlockConstant.QUERY_BLOCKCHAIN:
				write(webSocket, responseBlockChainMsg());//服务端调用方法返回最新区块:4
				break;
			//直接接收到其他节点发送的整条区块链信息:4
			case BlockConstant.RESPONSE_BLOCKCHAIN:
				handleBlockChainResponse(message.getData());
				break;
			}
		} catch (Exception e) {
			System.out.println("处理IP地址为：" +webSocket.getRemoteSocketAddress().getAddress().toString()
				+"，端口号为："+ webSocket.getRemoteSocketAddress().getPort() + "的p2p消息错误:" 
				+ e.getMessage());
		}
	}

	public void handleSelfMessage(String msg) {
		try {
			Message message = JSON.parseObject(msg, Message.class);
			System.out.println("解决本地mine到的msg for selfish mining"+JSON.toJSONString(message));
			switch (message.getType()) {
				//客户端请求查询最新的区块:1
				case BlockConstant.QUERY_LATEST_BLOCK:
					System.out.println(responseLatestBlockMsg());//服务端调用方法返回最新区块:2
					break;
				//接收到服务端返回的最新区块:2
				case BlockConstant.RESPONSE_LATEST_BLOCK:
					handleBlockResponse(message.getData());
					break;
				//客户端请求查询整个区块链:3
				case BlockConstant.QUERY_BLOCKCHAIN:
					System.out.println(responseBlockChainMsg());//服务端调用方法返回最新区块:4
					break;
				//直接接收到其他节点发送的整条区块链信息:4
				case BlockConstant.RESPONSE_BLOCKCHAIN:
					handleBlockChainResponse(message.getData());
					break;
			}
		} catch (Exception e) {
			System.out.println("处理消息错误:" + e.getMessage());
		}
	}

	/**
	 * 处理其它节点发送过来的区块信息
	 * @param blockData
	 */
	public synchronized void handleBlockResponse(String blockData) {
		//反序列化得到其它节点的最新区块信息
		Block latestBlockReceived = JSON.parseObject(blockData, Block.class);
		//当前节点的最新区块
		Block latestBlock = blockCache.getLatestBlock();
		if (latestBlockReceived != null) {
			if(latestBlock != null) {
				if (selfish) {
					//自私矿工
					if (whiteList.contains(latestBlockReceived.minerAddress())) {
						//如果接收到的区块高度比本地区块高度大的多
						if(latestBlockReceived.getHight() > latestBlock.getHight() + 1) {
							selfishBroadcast(queryBlockChainMsg());
							System.out.println("重新查询所有节点上的整条区块链");
						} else if (latestBlockReceived.getHight() > latestBlock.getHight() &&
								latestBlock.getHash().equals(latestBlockReceived.getPreviousHash())) {
							if (blockService.addBlock(latestBlockReceived)) {
								selfishBroadcast(responseLatestBlockMsg());
							}
							System.out.println("将新接收到的区块加入到本地的区块链");
						}
					}
					else {
						if (latestBlockReceived.getHight() > latestBlock.getHight()) {
							System.out.println("重新查询所有节点上的整条区块链");
							System.out.println("接受新区块链，设置fork=false");
							fork = false;
							broadcast(queryBlockChainMsg());
						} else if (latestBlockReceived.getHight()+1 == latestBlock.getHight()) {
							System.out.println("broadcast 最近的两个block的blockChain");
							broadcast(responsePartialBlockChainMsg(latestBlockReceived.getHight()+1));
							System.out.println("其他节点会更新blockChain");
						} else if (latestBlockReceived.getHight() == latestBlock.getHight() &&
								whiteList.contains(latestBlock.minerAddress())) {
							System.out.println("挖到下一个block要直接broadcast,设置fork=true");
							fork = true;
						}
					}
				} else {
					//正常矿工
					//如果接收到的区块高度比本地区块高度大的多
					if (latestBlockReceived.getHight() > latestBlock.getHight() + 1) {
						broadcast(queryBlockChainMsg());
						System.out.println("重新查询所有节点上的整条区块链");
					} else if (latestBlockReceived.getHight() > latestBlock.getHight() &&
							latestBlock.getHash().equals(latestBlockReceived.getPreviousHash())) {
						if (blockService.addBlock(latestBlockReceived)) {
							broadcast(responseLatestBlockMsg());
						}
						System.out.println("将新接收到的区块加入到本地的区块链");
					}
				}
			}else if(latestBlock == null) {
				broadcast(queryBlockChainMsg());
				System.out.println("重新查询所有节点上的整条区块链");
			}
		}
	}
	
	/**
	 * 处理其它节点发送过来的区块链信息
	 * @param blockData
	 */
	public synchronized void handleBlockChainResponse(String blockData) {
		//反序列化得到其它节点的整条区块链信息
		List<Block> receiveBlockchain = JSON.parseArray(blockData, Block.class);
		if(!CollectionUtils.isEmpty(receiveBlockchain) && blockService.isValidChain(receiveBlockchain)) {
			//根据区块索引先对区块进行排序
			Collections.sort(receiveBlockchain, new Comparator<Block>() {
				public int compare(Block block1, Block block2) {
					return block1.getHight() - block2.getHight();
				}
			});
			
			//其它节点的最新区块
			Block latestBlockReceived = receiveBlockchain.get(receiveBlockchain.size() - 1);
			//当前节点的最新区块
			Block latestBlock = blockCache.getLatestBlock();
			
			if(latestBlock == null) {
				//替换本地的区块链
				blockService.replaceChain(receiveBlockchain);
				System.out.println("latestBlock == null, 替换本地的区块链");
			} else {
				//其它节点区块链如果比当前节点的长，则处理当前节点的区块链
				if (latestBlockReceived.getHight() > latestBlock.getHight()) {
					if (latestBlock.getHash().equals(latestBlockReceived.getPreviousHash())) {
						if (blockService.addBlock(latestBlockReceived)) {
							if (selfish && whiteList.contains(latestBlockReceived.minerAddress())) {
								selfishBroadcast(responseLatestBlockMsg());
							} else {
								broadcast(responseLatestBlockMsg());
							}
						}
						System.out.println("将新接收到的区块加入到本地的区块链");
					} else {
						// 用长链替换本地的短链
						Boolean isValid = true;
						for (int i=1;i<receiveBlockchain.size()-1;i++) {
							Block currentBlock = receiveBlockchain.get(i);
							Block nextBlock = receiveBlockchain.get(i+1);
							if (!currentBlock.getHash().equals(nextBlock.getPreviousHash())) {
								isValid = false;
								break;
							}
						}
						if (isValid) {
							blockService.replaceChain(receiveBlockchain);
						}
					}
				}
			}
		}
	}
	
	/**
	 * 全网广播消息
	 * @param message
	 */
	public void broadcast(String message) {
		List<WebSocket> socketsList = this.getSockets();
		if (CollectionUtils.isEmpty(socketsList)) {
			return;
		}
		System.out.println("======全网广播消息开始：");
		for (WebSocket socket : socketsList) {
			this.write(socket, message);
		}
		System.out.println("======全网广播消息结束");
	}

	public void selfishBroadcast(String message) {
		List<WebSocket> socketsList = this.getSelfishSockets();
		if (CollectionUtils.isEmpty(socketsList)) {
			return;
		}
		System.out.println("======Selfish广播消息开始：");
		for (WebSocket socket : socketsList) {
			this.write(socket, message);
		}
		System.out.println("======Selfish广播消息结束");
	}
	
	/**
	 * 向其它节点发送消息
	 * @param ws
	 * @param message
	 */
	public void write(WebSocket ws, String message) {
		System.out.println("发送给IP地址为：" +ws.getRemoteSocketAddress().getAddress().toString() 
			+ "，端口号为："+ws.getRemoteSocketAddress().getPort() + " 的p2p消息:" + message);
		ws.send(message);
	}

	/**
	 * 查询整条区块链
	 * @return
	 */
	public String queryBlockChainMsg() {
		return JSON.toJSONString(new Message(BlockConstant.QUERY_BLOCKCHAIN));
	}
	
	/**
	 * 返回整条区块链数据
	 * @return
	 */
	public String responseBlockChainMsg() {
		Message msg = new Message();
		msg.setType(BlockConstant.RESPONSE_BLOCKCHAIN);
		msg.setData(JSON.toJSONString(blockCache.getBlockChain()));
		return JSON.toJSONString(msg);
	}

	/**
	 * 返回部分区块链数据
	 * @return
	 */
	public String responsePartialBlockChainMsg(int i) {
		Message msg = new Message();
		msg.setType(BlockConstant.RESPONSE_BLOCKCHAIN);
		msg.setData(JSON.toJSONString(blockCache.getPartialBlockChain(i)));
		return JSON.toJSONString(msg);
	}

	/**
	 * 查询最新的区块
	 * @return
	 */
	public String queryLatestBlockMsg() {
		return JSON.toJSONString(new Message(BlockConstant.QUERY_LATEST_BLOCK));
	}
	
	/**
	 * 返回最新的区块
	 * @return
	 */
	public String responseLatestBlockMsg() {
		Message msg = new Message();
		msg.setType(BlockConstant.RESPONSE_LATEST_BLOCK);
		Block b = blockCache.getLatestBlock();
		msg.setData(JSON.toJSONString(b));
		return JSON.toJSONString(msg);
	}
	
	public List<WebSocket> getSockets(){
		return blockCache.getSocketsList();
	}

	public List<WebSocket> getSelfishSockets(){
		return blockCache.getSelfishSocketsList();
	}

	@Override
	public void run(ApplicationArguments args) throws Exception {
		p2PServer.initP2PServer(blockCache.getP2pport());
		System.out.println("*****难度系数******"+blockCache.getDifficulty());
		System.out.println("*****端口号******"+blockCache.getP2pport());
		for (String Address : blockCache.getAddressList()) {
				p2PClient.connectToPeer(Address);
				System.out.println("*****节点地址******"+Address);

		}
		if (selfish) {
			for (String Address : blockCache.getSelfishAddressList()) {
				p2PClient.connectToSelfishPeer(Address);
				System.out.println("*****自私节点地址******" + Address);
			}
		}

	}
	
}