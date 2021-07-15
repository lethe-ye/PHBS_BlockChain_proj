package com.dce.blockchain.web.util;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.java_websocket.WebSocket;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import com.dce.blockchain.web.model.Block;
import com.dce.blockchain.web.model.Transaction;

@ConfigurationProperties(prefix = "block")
@Component
public class BlockCache {

	/**
	 * 当前节点的区块链结构
	 */
	private List<Block> blockChain = new CopyOnWriteArrayList<Block>();

	/**
	 * 已打包保存的业务数据集合
	 */
	private List<Transaction> packedTransactions = new CopyOnWriteArrayList<Transaction>();
	
	/**
	 * 当前节点的socket对象
	 */
	private List<WebSocket> socketsList = new CopyOnWriteArrayList<WebSocket>();

	private List<WebSocket> selfishSocketsList = new CopyOnWriteArrayList<WebSocket>();
	
	/**
	 * 挖矿的难度系数
	 */
	@Value("${block.difficulty}")
	private int difficulty;
	
	/**
	 * 当前节点p2pserver端口号
	 */
	@Value("${block.p2pport}")
	private int p2pport;
	
	/**
	 * 要连接的节点地址
	 */
	@Value("#{'${block.address}'.split(',')}")
	private List<String> addressList;

	/**
	 * 要连接的自私节点地址
	 */
	@Value("#{'${block.selfishAddress}'.split(',')}")
	private List<String> selfishAddressList;
	
	/**
	 * 获取最新的区块，即当前链上最后一个区块
	 * 
	 * @return
	 */
	public Block getLatestBlock() {
		return blockChain.size() > 0 ? blockChain.get(blockChain.size() - 1) : null;
	}

	public List<Block> getBlockChain() {
		return blockChain;
	}

	public List<Block> getPartialBlockChain(int id){
		List<Block> partialBlockChain = new CopyOnWriteArrayList<Block>();
		for (int i=0;i<id;i++){
			partialBlockChain.add(blockChain.get(i));
		}
		return partialBlockChain;
	}

	public void setBlockChain(List<Block> blockChain) {
		this.blockChain = blockChain;
	}

	public List<Transaction> getPackedTransactions() {
		return packedTransactions;
	}

	public void setPackedTransactions(List<Transaction> packedTransactions) {
		this.packedTransactions = packedTransactions;
	}

	public int getDifficulty() {
		return difficulty;
	}

	public void setDifficulty(int difficulty) {
		this.difficulty = difficulty;
	}

	public List<WebSocket> getSocketsList() {
		return socketsList;
	}

	public List<WebSocket> getSelfishSocketsList() {
		return selfishSocketsList;
	}

	public void setSocketsList(List<WebSocket> socketsList) {
		this.socketsList = socketsList;
	}

	public void setSelfishSocketsList(List<WebSocket> socketsList) {
		this.selfishSocketsList = socketsList;
	}

	public int getP2pport() {
		return p2pport;
	}

	public void setP2pport(int p2pport) {
		this.p2pport = p2pport;
	}

	public List<String> getSelfishAddressList() {
		return selfishAddressList;
	}

	public List<String> getAddressList() {
		return addressList;
	}

	public void setAddressList(List<String> addressList) {
		this.addressList = addressList;
	}

	public void setSelfishAddressList(List<String> addressList) {
		this.selfishAddressList = addressList;
	}

}
