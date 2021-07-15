package com.dce.blockchain.web.service;

import java.io.UnsupportedEncodingException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import com.dce.blockchain.web.util.CryptoUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Service;
import com.alibaba.fastjson.JSON;
import com.dce.blockchain.web.model.Block;
import com.dce.blockchain.web.model.Message;
import com.dce.blockchain.web.model.Transaction;
import com.dce.blockchain.web.util.BlockCache;
import com.dce.blockchain.web.util.BlockConstant;
import com.dce.blockchain.web.util.CommonUtil;

import static com.dce.blockchain.web.model.Block.COINBASE;

/**
 * 共识机制
 * 采用POW即工作量证明实现共识
 * @author Administrator
 *
 */
@ConfigurationProperties(prefix = "address")
@Service
public class PowService {

	@Autowired
	BlockCache blockCache;
	
	@Autowired
	BlockService blockService;
	
	@Autowired
	P2PService p2PService;

	@Value("${block.difficulty}")
	public int difficulty;

	@Value("${address.mineradd}")
	public String minerAddress;

	public PowService() throws NoSuchAlgorithmException {
	}

	/**
	 * 通过“挖矿”进行工作量证明，实现节点间的共识
	 * 
	 * @return
	 * @throws UnsupportedEncodingException
	 */
	public Block mine() throws UnsupportedEncodingException {

		if (blockCache.getBlockChain().size() == 0) {
			blockService.createGenesisBlock();
			return blockCache.getLatestBlock();
		}

		List<Transaction> tsaList = new ArrayList<Transaction>();
		Transaction tsa1 = new Transaction();

		tsa1.setId("1");
		tsa1.setAdditionOutput(COINBASE, minerAddress);

		tsaList.add(tsa1);
		
		// 定义每次哈希函数的结果 
		String newBlockHash;
		int nonce = 0;
		long start = System.currentTimeMillis();
		System.out.println("开始挖矿");
		while (true) {
			String transactionsHash = "";
			for (Transaction tx: tsaList) {
				transactionsHash += tx.getHash();
			}
			String merkleRoot = CryptoUtil.SHA256(transactionsHash);
			// 计算新区块hash值
			newBlockHash = CryptoUtil.calculateHash(blockCache.getLatestBlock().getHash(), merkleRoot, nonce);
			// 校验hash值
			if (blockService.isValidHash(newBlockHash)) {
				System.out.println("挖矿完成，正确的hash值：" + newBlockHash);
				System.out.println("挖矿耗费时间：" + (System.currentTimeMillis() - start) + "ms");
				break;
			}
			System.out.println("第"+(nonce+1)+"次尝试计算的hash值：" + newBlockHash);
			nonce++;
		}
		// 创建新的区块
		Block block = blockService.createNewBlock(nonce, blockCache.getLatestBlock().getHash(), tsaList, newBlockHash, difficulty);
		
		//创建成功后，全网广播出去
		Message msg = new Message();
		msg.setType(BlockConstant.RESPONSE_LATEST_BLOCK);
		msg.setData(JSON.toJSONString(block));
		p2PService.broadcast(JSON.toJSONString(msg));

		return block;
	}

	/**
	 * 自私挖矿，挖到后保存本地，发送给自私节点
	 * 但不广播给其他节点
	 *
	 * @return
	 * @throws UnsupportedEncodingException
	 */
	public Block selfishMine() throws UnsupportedEncodingException {

		if (blockCache.getBlockChain().size() == 0) {
			blockService.createGenesisBlock();
			return blockCache.getLatestBlock();
		}

		List<Transaction> tsaList = new ArrayList<Transaction>();
		Transaction tsa1 = new Transaction();

		tsa1.setId("1");
		tsa1.setAdditionOutput(COINBASE, minerAddress);

		tsaList.add(tsa1);

		// 定义每次哈希函数的结果
		String newBlockHash;
		int nonce = 0;
		long start = System.currentTimeMillis();
		System.out.println("开始挖矿");
		while (true) {
			String transactionsHash = "";
			for (Transaction tx: tsaList) {
				transactionsHash += tx.getHash();
			}
			String merkleRoot = CryptoUtil.SHA256(transactionsHash);
			// 计算新区块hash值
			newBlockHash = CryptoUtil.calculateHash(blockCache.getLatestBlock().getHash(), merkleRoot, nonce);
			// 校验hash值
			if (blockService.isValidHash(newBlockHash)) {
				System.out.println("挖矿完成，正确的hash值：" + newBlockHash);
				System.out.println("挖矿耗费时间：" + (System.currentTimeMillis() - start) + "ms");
				break;
			}
//			System.out.println("第"+(nonce+1)+"次尝试计算的hash值：" + newBlockHash);
			nonce++;
		}
		// 创建新的区块
		Block block = blockService.createNewBlock(nonce, blockCache.getLatestBlock().getHash(), tsaList, newBlockHash, difficulty);
		// 创建成功后，加入待广播Message
		Message msg = new Message();
		if (p2PService.getFork()) {
			System.out.println("正常发布Block，设置fork=false");
			p2PService.setFork(false);
			msg.setType(BlockConstant.RESPONSE_BLOCKCHAIN);
			msg.setData(JSON.toJSONString(blockCache.getBlockChain()));
			p2PService.handleSelfMessage(JSON.toJSONString(msg));
			// 广播给全部节点
			p2PService.broadcast(JSON.toJSONString(msg));
		} else {
			msg.setType(BlockConstant.RESPONSE_LATEST_BLOCK);
			msg.setData(JSON.toJSONString(block));
			p2PService.handleSelfMessage(JSON.toJSONString(msg));
			// 广播给自私节点
			p2PService.selfishBroadcast(JSON.toJSONString(msg));
		}

		return block;
	}

}
