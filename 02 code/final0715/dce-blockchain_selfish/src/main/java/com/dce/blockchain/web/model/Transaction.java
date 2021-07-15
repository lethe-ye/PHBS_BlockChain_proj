package com.dce.blockchain.web.model;

import com.alibaba.fastjson.JSON;
import com.dce.blockchain.web.service.BlockService;
import com.dce.blockchain.web.util.CryptoUtil;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 业务数据模型
 * 这里面重要的就是 构造coinbaseTx以及之后取miner的publicKey
 * @author Jared Jia
 *
 */
public class Transaction implements Serializable {

	private static final long serialVersionUID = 1L;
	/**
	 * 唯一标识
	 */
	private String id;
	/**
	 * 业务数据,input & output
	 */
	private String inputs = "[]";
	private String outputs = "[]";

	private String hash;

	public static class Input implements Serializable {
		/** hash of the Transaction whose output is being used */
		private String prevTxHash;
		/** used output's index in the previous transaction */
		private int outputIndex;
		/** the signature produced to check validity */
		private String signature;

		public void setPrevTxHash(String value) {
			this.prevTxHash = value;
		}

		public String getPrevTxHash(){ return prevTxHash; }

		public void setOutputIndex(int index) {
			this.outputIndex = index;
		}

		public int getOutputIndex() {
			return outputIndex;
		}

		public void setSignature(String value) {
			this.signature = value;
		}

		public String getSignature() {
			return signature;
		}

	}

	public static class Output implements Serializable {
		/** value in bitcoins of the output */
		private double value;
		/** the address or public key of the recipient */
		private String address;

		public void setValue(double value) {
			this.value = value;
		}

		public double getValue(){return value;}

		public void setPublicKey(String address) {
			this.address = address;
		}

		public String getPublicKey(){return address;}

	}

	public void setAdditionOutput(double value, String address) {
		List<Output> outputList = JSON.parseArray(outputs,Output.class);
		Output op = new Output();
		op.setValue(value);
		op.setPublicKey(address);
		outputList.add(op);
		this.outputs = JSON.toJSONString(outputList);
	}

	public Output getOutput(int index) {
		List<Output> outputList = new ArrayList<Output>();
		if (outputs != null) {
			outputList = JSON.parseArray(outputs,Output.class);
		}
		if (index < outputList.size()) {
			return outputList.get(index);
		}
		return null;
	}
	
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public List<Input> getInputs() {
		List<Input> inputList = JSON.parseArray(inputs,Input.class);
		return inputList;
	}

	public List<Output> getOutputs() {
		List<Output> outputList = JSON.parseArray(outputs,Output.class);
		return outputList;
	}

	public void setInputs(String inputs) {
		this.inputs = inputs;
	}

	public void setOutputs(String outputs) {
		this.outputs = outputs;
	}

	public void setHash() {
		String txInfo = id + inputs + outputs;
		this.hash =  CryptoUtil.SHA256(txInfo);
	}

	public String getHash() {
		if (hash == null) {
			setHash();
		}
		return hash;
	}

}
