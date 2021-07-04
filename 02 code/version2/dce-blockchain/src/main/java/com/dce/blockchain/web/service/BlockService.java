package com.dce.blockchain.web.service;

import java.io.UnsupportedEncodingException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.dce.blockchain.web.util.CommonUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;
import com.dce.blockchain.web.model.Block;
import com.dce.blockchain.web.model.Transaction;
import com.dce.blockchain.web.util.BlockCache;
import com.dce.blockchain.web.util.CryptoUtil;

/**
 * 区块链核心服务
 * 
 * @author Jared Jia
 *
 */
@ConfigurationProperties(prefix = "address")
@Service
public class BlockService {

	public static final double COINBASE = 25;
	@Autowired
	BlockCache blockCache;

	@Value("${address.genesisadd}")
	public String genesisAddress;
	public BlockService() throws NoSuchAlgorithmException {
	}

	/**
	 * 创建创世区块
	 * @return
	 */
	public String createGenesisBlock() throws UnsupportedEncodingException {

		System.out.println("keyPairGenesis");
		//TODO: 把下面的换成coinBaseTxs之类的
		List<Transaction> tsaList = new ArrayList<Transaction>();
		Transaction tsa1 = new Transaction(COINBASE,genesisAddress);
		System.out.println("tsa1");

		tsa1.setId("1");
		tsa1.setBusinessInfo("这是创世区块,Miner Address = "+genesisAddress);
		tsaList.add(tsa1);
		Transaction tsa2 = new Transaction();
		tsa2.setId("2");
		tsa2.setBusinessInfo("区块链高度为：1");
		tsaList.add(tsa2);
		System.out.println("tsa2");

		String genesisPrevHash = "0000002009genesishash";
		String genesisBlockHash = calculateHash(genesisPrevHash,tsaList,1);
		System.out.println("genesisBlockHash");

		Block genesisBlock = new Block(genesisPrevHash,tsaList,genesisBlockHash);
		System.out.println("genesisBlock");

		//设置创世区块高度为1
		genesisBlock.setIndex(1);
		genesisBlock.setTimestamp(System.currentTimeMillis());
		genesisBlock.setNonce(1);

		//添加到已打包保存的业务数据集合中
		blockCache.getPackedTransactions().addAll(genesisBlock.getTransactions());
		System.out.println("getPackedTransactions");

		//添加到区块链中
		blockCache.getBlockChain().add(genesisBlock);
		return JSON.toJSONString(genesisBlock);
	}

	/**
	 * 创建新区块
	 * @param nonce
	 * @param previousHash
	 * @param blockTxs
	 * @param thisBlockHash
	 * @return
	 */
	public Block createNewBlock(int nonce, String previousHash, List<Transaction> blockTxs, String thisBlockHash) {
		Block block = new Block(previousHash,blockTxs,thisBlockHash);
		block.setIndex(blockCache.getBlockChain().size() + 1);
		//时间戳
		block.setTimestamp(System.currentTimeMillis());
		//工作量证明，计算正确hash值的次数
		block.setNonce(nonce);
		if (addBlock(block)) {
			return block;
		}
		return null;
	}

	/**
	 * 添加新区块到当前节点的区块链中
	 * 
	 * @param newBlock
	 */
	public boolean addBlock(Block newBlock) {
		//先对新区块的合法性进行校验
		System.out.println(blockCache.getLatestBlock());
		System.out.println("blockCache.getLatestBlock()");
		if (isValidNewBlock(newBlock, blockCache.getLatestBlock())) {
			blockCache.getBlockChain().add(newBlock);
			// 新区块的业务数据需要加入到已打包的交易集合里去
			blockCache.getPackedTransactions().addAll(newBlock.getTransactions());
			return true;
		}
		return false;
	}
	
	/**
	 * 验证新区块是否有效
	 * 
	 * @param newBlock
	 * @param previousBlock
	 * @return
	 */
	public boolean isValidNewBlock(Block newBlock, Block previousBlock) {
		ArrayList<PublicKey> blackList = new ArrayList<PublicKey>();
//		blackList.add("someone's publicKey");
		if (blackList.contains(newBlock.getMinerAddress())){
			System.out.println("该区块的miner在黑名单中，验证不通过");
			return false;
		}

		if (!previousBlock.getHash().equals(newBlock.getPreviousHash())) {
			System.out.println("新区块的前一个区块hash验证不通过");
			return false;
		} else {
			// 验证新区块hash值的正确性
			String hash = "";
			try {
				hash = calculateHash(newBlock.getPreviousHash(), newBlock.getTransactions(), newBlock.getNonce());
				System.out.println("prevHash"+newBlock.getPreviousHash());
				System.out.println("getTransactions"+newBlock.getTransactions());
				System.out.println("newBlock.getNonce()"+newBlock.getNonce());
				System.out.println("getHash"+newBlock.getHash());
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
			// !Arrays.equals(hash,newBlock.getHash())
			if (!hash.equals(newBlock.getHash())) {
				System.out.println("新区块的hash无效: " + hash + "\n" + newBlock.getHash());
				return false;
			}
			if (!isValidHash(newBlock.getHash())) {
				return false;
			}
		}

		return true;
	}
	
	/**
	 * 验证hash值是否满足系统条件
	 * 
	 * @param hashStr
	 * @return
	 */
	public boolean isValidHash(String hashStr) {
		return hashStr.startsWith("0000");
	}
	
	/**
	 * 验证整个区块链是否有效
	 * @param chain
	 * @return
	 */
	public boolean isValidChain(List<Block> chain) {
		Block block = null;
		Block lastBlock = chain.get(0);
		int currentIndex = 1;
		while (currentIndex < chain.size()) {
			block = chain.get(currentIndex);

			if (!isValidNewBlock(block, lastBlock)) {
				return false;
			}

			lastBlock = block;
			currentIndex++;
		}
		return true;
	}

	/**
	 * 替换本地区块链
	 * 
	 * @param newBlocks
	 */
	public void replaceChain(List<Block> newBlocks) {
		List<Block> localBlockChain = blockCache.getBlockChain();
		List<Transaction> localpackedTransactions = blockCache.getPackedTransactions();
		if (isValidChain(newBlocks) && newBlocks.size() > localBlockChain.size()) {
			localBlockChain = newBlocks;
			//替换已打包保存的业务数据集合
			localpackedTransactions.clear();
			localBlockChain.forEach(block -> {
				localpackedTransactions.addAll(block.getTransactions());
			});
			blockCache.setBlockChain(localBlockChain);
			blockCache.setPackedTransactions(localpackedTransactions);
			System.out.println("替换后的本节点区块链："+JSON.toJSONString(blockCache.getBlockChain()));
		} else {
			System.out.println("接收的区块链无效");
		}
	}

	/**
	 * 计算区块的hash
	 * 
	 * @param previousHash
	 * @param currentTransactions
	 * @param nonce
	 * @return
	 */
	public String calculateHash(String previousHash, List<Transaction> currentTransactions, int nonce) throws UnsupportedEncodingException {
		return CryptoUtil.SHA256(previousHash + JSON.toJSONString(currentTransactions) + nonce);
	}

}
