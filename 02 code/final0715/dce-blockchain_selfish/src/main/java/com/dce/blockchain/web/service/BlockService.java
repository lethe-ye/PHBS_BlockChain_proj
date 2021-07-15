package com.dce.blockchain.web.service;

import java.io.UnsupportedEncodingException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.dce.blockchain.web.model.Message;
import com.dce.blockchain.web.util.BlockConstant;
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

import static com.dce.blockchain.web.model.Block.COINBASE;

/**
 * 区块链核心服务
 * 
 * @author Jared Jia
 *
 */
@Service
public class BlockService {

	public static final double COINBASE = 50;
	@Autowired

	BlockCache blockCache;

	@Autowired
	P2PService p2PService;

	@Value("${address.genesisadd}")
	public String genesisAddress;

	@Value("${block.difficulty}")
	public int difficulty;

	@Value("#{'${address.blacklist}'.split(',')}")
	public List<String> blackList;

	public BlockService() throws NoSuchAlgorithmException {
	}

	/**
	 * 创建创世区块
	 * @return
	 */
	public String createGenesisBlock() throws UnsupportedEncodingException {

		if (blockCache.getLatestBlock() != null) {
			return "Already Have GenesisBlock!";
		}

		List<Transaction> tsaList = new ArrayList<Transaction>();
		System.out.println("tsa1");
		Transaction tsa1 = new Transaction();

		tsa1.setId("1");
		tsa1.setAdditionOutput(COINBASE, genesisAddress);

		tsaList.add(tsa1);

		String genesisPrevHash = "0000002009genesishash";

		Block genesisBlock = new Block();
		genesisBlock.setDifficulty(difficulty);
		genesisBlock.setTransactions(tsaList);
		//上一区块的哈希
		genesisBlock.setPreviousHash(genesisPrevHash);
		//当前区块的哈希
		String genesisBlockHash = CryptoUtil.calculateHash(genesisPrevHash,genesisBlock.getMerkleRoot(),1);
		genesisBlock.setHash(genesisBlockHash);

		//设置创世区块高度为1
		genesisBlock.setHight(1);
		genesisBlock.setTimestamp(System.currentTimeMillis());
		genesisBlock.setNonce(1);

		//添加到已打包保存的业务数据集合中
		blockCache.getPackedTransactions().addAll(genesisBlock.getTransactions());

		//添加到区块链中
		blockCache.getBlockChain().add(genesisBlock);
		//全网广播
		Message msg = new Message();
		msg.setType(BlockConstant.RESPONSE_BLOCKCHAIN);
		msg.setData(JSON.toJSONString(blockCache.getBlockChain()));
		p2PService.broadcast(JSON.toJSONString(msg));

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
	public Block createNewBlock(int nonce, String previousHash, List<Transaction> blockTxs, String thisBlockHash, int difficulty) {
		Block block = new Block();
		block.setHight(blockCache.getBlockChain().size() + 1);
		//时间戳
		block.setTimestamp(System.currentTimeMillis());
		//交易列表
		block.setTransactions(blockTxs);
		//工作量证明，计算正确hash值的次数
		block.setNonce(nonce);
		//上一区块的哈希
		block.setPreviousHash(previousHash);
		//当前区块的哈希
		block.setHash(thisBlockHash);
		//难度
		block.setDifficulty(difficulty);
		//计算Merkle Root
		block.calMerkleRoot();

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
		if (blackList.contains(newBlock.minerAddress())){
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
				hash = CryptoUtil.calculateHash(newBlock.getPreviousHash(), newBlock.getMerkleRoot(), newBlock.getNonce());
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
			// !Arrays.equals(hash,newBlock.getHash())
			if (!hash.equals(newBlock.getHash())) {
				System.out.println("新区块的hash无效: " + hash + "\n" + newBlock.getHash());
				return false;
			}
			if (newBlock.getDifficulty()!= difficulty) {
				System.out.println("新区块的difficulty无效: " + newBlock.getDifficulty() + "\n" + difficulty);
				return false;
			}
			if (newBlock.getHight()!=1 & !isValidHash(newBlock.getHash())) {
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
		String startStr = String.format("%0" + blockCache.getDifficulty() + "d", 0);
		return hashStr.startsWith(startStr);
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

}
