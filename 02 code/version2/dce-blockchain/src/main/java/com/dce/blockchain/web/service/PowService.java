package com.dce.blockchain.web.service;

import java.io.UnsupportedEncodingException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
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

	@Value("${address.mineradd}")
	public String minerAddress;
//	KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance("RSA");

	public PowService() throws NoSuchAlgorithmException {
	}

	/**
	 * 通过“挖矿”进行工作量证明，实现节点间的共识
	 * 
	 * @return
//	 * @throws UnknownHostException
	 */
	public Block mine() throws UnsupportedEncodingException {

//		KeyPair keyPairMiner;
//		keyPairMiner = keyPairGen.generateKeyPair();
		//TODO: 把下面的换成coinBaseTxs之类的
		List<Transaction> tsaList = new ArrayList<Transaction>();
		Transaction tsa1 = new Transaction(COINBASE,minerAddress);
		tsa1.setId("1");
		tsa1.setBusinessInfo("这是IP为："+CommonUtil.getLocalIp()+"，端口号为："+blockCache.getP2pport()+"的节点挖矿生成的区块\n" +
				"Miner Address = "+minerAddress);
		tsa1.setMinerAddress(minerAddress);
		tsaList.add(tsa1);
		Transaction tsa2 = new Transaction();
		tsa2.setId("2");
		tsa2.setBusinessInfo("区块链高度为："+(blockCache.getLatestBlock().getIndex()+1));
		tsaList.add(tsa2);
		
		// 定义每次哈希函数的结果 
		String newBlockHash;
		int nonce = 0;
		long start = System.currentTimeMillis();
		System.out.println("开始挖矿");
		while (true) {
			// 计算新区块hash值
			newBlockHash = blockService.calculateHash(blockCache.getLatestBlock().getHash(), tsaList, nonce);
			// 校验hash值
			if (blockService.isValidHash(newBlockHash)) {
				System.out.println("挖矿完成，正确的hash值：" + newBlockHash);
//				System.out.println(blockCache.getLatestBlock().getHash());
//				System.out.println(tsaList);
//				System.out.println(nonce);

				System.out.println("挖矿耗费时间：" + (System.currentTimeMillis() - start) + "ms");
				break;
			}
			System.out.println("第"+(nonce+1)+"次尝试计算的hash值：" + newBlockHash);
			nonce++;
		}
		// 创建新的区块
		Block block = blockService.createNewBlock(nonce, blockCache.getLatestBlock().getHash(), tsaList, newBlockHash);
		
		//创建成功后，全网广播出去
		Message msg = new Message();
		msg.setType(BlockConstant.RESPONSE_LATEST_BLOCK);
		msg.setData(JSON.toJSONString(block));
		p2PService.broatcast(JSON.toJSONString(msg));
		
		return block;
	}
	
}
