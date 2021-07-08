package com.dce.blockchain.web.model;

import com.alibaba.fastjson.JSON;
import com.dce.blockchain.web.util.CryptoUtil;

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

	public static final double COINBASE = 50;

	private static final long serialVersionUID = 1L;

	/**
	 * 区块索引号(区块高度)
	 */
	private int hight;

	private int difficulty;
	/**
	 * 生成区块的时间戳
	 */
	private long timestamp;
	/**
	 * 工作量证明，计算正确hash值的次数
	 */
	private int nonce;

	private String hash;

	private String previousHash;

	private String merkleRoot;

	private String transactionsStr;

	public void setTransactions(List<Transaction> transactions) {
		this.transactionsStr = JSON.toJSONString(transactions);
	}

	public void setPreviousHash(String previousHash) {
		this.previousHash = previousHash;
	}

	public void setHash(String hash) {
		this.hash = hash;
	}

	public String minerAddress() {
		return getTransactions().get(0).getOutput(0).getPublicKey();
	}

	public String getHash() {
		return hash;
	}

	public List<Transaction> getTransactions() {
		List<Transaction> transactions = JSON.parseArray(transactionsStr,Transaction.class);
		return transactions;
	}

	public Transaction getTransaction(int index) {
		List<Transaction> transactions = JSON.parseArray(transactionsStr,Transaction.class);
		Transaction tx = transactions.get(index);
//		tx.calTransaction();
		return tx;
	}

	public void calMerkleRoot() {
		String txsHash = "";
		for (Transaction tx: getTransactions()) {
			txsHash += tx.getHash();
		}
		this.merkleRoot = CryptoUtil.SHA256(txsHash);
	}

	public String getMerkleRoot() {
		if (merkleRoot == null) {
			calMerkleRoot();
		}
		return merkleRoot;
	}

	public String getPreviousHash() {
		return previousHash;
	}

	public int getHight() {
		return hight;
	}

	public void setHight(int index) {
		this.hight = index;
	}

	public int getDifficulty() {
		return difficulty;
	}

	public void setDifficulty(int difficulty) {
		this.difficulty = difficulty;
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


