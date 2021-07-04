package com.dce.blockchain.web.model;

import java.io.Serializable;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;

/**
 * 区块结构
 * 
 * @author Jared Jia
 *
 */
public class Block implements Serializable {

	public static final double COINBASE = 25;

	private String hash;
	private String prevBlockHash;
	private Transaction coinbase;
	private List<Transaction> txs;

	/**
	 * {@code address} is the address to which the coinbase transaction would go
	 */
	public Block(String prevHash, List<Transaction> blockTxs, String thisHash) {
		prevBlockHash = prevHash;
		coinbase = blockTxs.get(0);
		txs = blockTxs;
		hash = thisHash;
	}

	public Transaction getCoinbase() {
		return coinbase;
	}

	public String getMinerAddress() {
		return coinbase.getOutput(0).getPublicKey();
	}

	public String getHash() {
		return hash;
	}

	public String getPrevBlockHash() {
		return prevBlockHash;
	}

	public List<Transaction> getTransactions() {
		return txs;
	}

	public Transaction getTransaction(int index) {
		return txs.get(index);
	}


	public String getPreviousHash() {
		return this.prevBlockHash;
	}


	private static final long serialVersionUID = 1L;

	/**
	 * 区块索引号(区块高度)
	 */
	private int index;
	/**
	 * 生成区块的时间戳
	 */
	private long timestamp;
	/**
	 * 工作量证明，计算正确hash值的次数
	 */
	private int nonce;

	public int getIndex() {
		return index;
	}

	public void setIndex(int index) {
		this.index = index;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	public int getNonce() {
		return nonce;
	}

	public void setNonce(int nonce) {
		this.nonce = nonce;
	}
}


