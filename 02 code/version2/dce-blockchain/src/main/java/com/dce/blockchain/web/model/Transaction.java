package com.dce.blockchain.web.model;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * 业务数据模型
 * 这里面重要的就是 构造coinbaseTx以及之后取miner的publicKey
 * @author Jared Jia
 *
 */
public class Transaction implements Serializable {

	public class Input {
		/** hash of the Transaction whose output is being used */
		public byte[] prevTxHash;
		/** used output's index in the previous transaction */
		public int outputIndex;
		/** the signature produced to check validity */
		public byte[] signature;

		public Input(byte[] prevHash, int index) {
			if (prevHash == null)
				prevTxHash = null;
			else
				prevTxHash = Arrays.copyOf(prevHash, prevHash.length);
			outputIndex = index;
		}

	}

	public class Output {
		/** value in bitcoins of the output */
		public double value;
		/** the address or public key of the recipient */
		public String address;

		public Output(double v, String addr) {
			value = v;
			address = addr;
		}

		public String getPublicKey(){return address;}

	}

	/** hash of the transaction, its unique id */
	private byte[] hash;
	private ArrayList<Input> inputs;
	private ArrayList<Output> outputs;
	private boolean coinbase;

	public Transaction() {
		inputs = new ArrayList<Input>();
		outputs = new ArrayList<Output>();
		coinbase = false;
	}


	/** create a coinbase transaction of value {@code coin} and calls finalize on it */
	public Transaction(double coin, String address) {
		coinbase = true;
		inputs = new ArrayList<Input>();
		outputs = new ArrayList<Output>();
		addOutput(coin, address);
		finalize();
	}
	public void addOutput(double value, String address) {
		Output op = new Output(value, address);
		outputs.add(op);
	}

	public Output getOutput(int index) {
		if (index < outputs.size()) {
			return outputs.get(index);
		}
		return null;
	}
	public byte[] getRawTx() {
		ArrayList<Byte> rawTx = new ArrayList<Byte>();
		for (Input in : inputs) {
			byte[] prevTxHash = in.prevTxHash;
			ByteBuffer b = ByteBuffer.allocate(Integer.SIZE / 8);
			b.putInt(in.outputIndex);
			byte[] outputIndex = b.array();
			byte[] signature = in.signature;
			if (prevTxHash != null)
				for (int i = 0; i < prevTxHash.length; i++)
					rawTx.add(prevTxHash[i]);
			for (int i = 0; i < outputIndex.length; i++)
				rawTx.add(outputIndex[i]);
			if (signature != null)
				for (int i = 0; i < signature.length; i++)
					rawTx.add(signature[i]);
		}
		for (Output op : outputs) {
			ByteBuffer b = ByteBuffer.allocate(Double.SIZE / 8);
			b.putDouble(op.value);
			byte[] value = b.array();
			for (int i = 0; i < value.length; i++)
				rawTx.add(value[i]);
		}
		byte[] tx = new byte[rawTx.size()];
		int i = 0;
		for (Byte b : rawTx)
			tx[i++] = b;
		return tx;
	}

	public void finalize() {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			md.update(getRawTx());
			hash = md.digest();
		} catch (NoSuchAlgorithmException x) {
			x.printStackTrace(System.err);
		}
	}


	private static final long serialVersionUID = 1L;
	/**
	 * 唯一标识
	 */
	private String id;
	/**
	 * 业务数据
	 */
	private String businessInfo;

	/**
	 * 新增！！： miner的Address，用于blackList
	 */
	private String minerAddress;
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getBusinessInfo() {
		return businessInfo;
	}
	public void setBusinessInfo(String businessInfo) {
		this.businessInfo = businessInfo;
	}
	public void setMinerAddress(String minerAddress){
		this.minerAddress = minerAddress;
	}
	public String getMinerAddress(){
		return minerAddress;
	}

}
