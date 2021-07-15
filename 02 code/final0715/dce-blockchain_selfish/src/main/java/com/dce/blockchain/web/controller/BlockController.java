package com.dce.blockchain.web.controller;

import javax.annotation.Resource;

import com.dce.blockchain.web.model.Message;
import com.dce.blockchain.web.service.P2PService;
import com.dce.blockchain.web.util.BlockConstant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.alibaba.fastjson.JSON;
import com.dce.blockchain.web.service.BlockService;
import com.dce.blockchain.web.service.PowService;
import com.dce.blockchain.web.util.BlockCache;
import com.dce.blockchain.web.model.Block;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

@Controller
public class BlockController {

	@Resource
	BlockService blockService;
	
	@Resource
	PowService powService;
	
	@Autowired
	BlockCache blockCache;

	@Autowired
	P2PService p2PService;
	
	/**
	 * 查看当前节点区块链数据
	 * @return
	 */
	@GetMapping("/scan")
	@ResponseBody
	public String scanBlock() {
		return JSON.toJSONString(blockCache.getBlockChain());
	}

	/**
	 * 展示当前节点区块链简要数据
	 * @return
	 */
	@GetMapping("/display")
	@ResponseBody
	public String displayBlock() {
		List<Block> currentBlockChain = blockCache.getBlockChain();
		List<String> displayStr = new ArrayList<String>();
		for (Block block : currentBlockChain) {
			String str = "The miner for Block "+block.getHight()+" : "+block.minerAddress();
			displayStr.add(str);
		}
		return JSON.toJSONString(displayStr);
	}

	/**
	 * 清空当前节点区块链数据
	 * @return
	 */
	@GetMapping("/reset")
	@ResponseBody
	public String resetBlock() {
		List<Block> newBlockChain = new ArrayList<Block>();
		blockCache.setBlockChain(newBlockChain);
		return scanBlock();
	}
	
	/**
	 * 查看当前节点最新区块数据
	 * @return
	 */
	@GetMapping("/data")
	@ResponseBody
	public String scanData() {
		return JSON.toJSONString(blockCache.getPackedTransactions());
	}
	
	/**
	 * 创建创世区块
	 * @return
	 */
	@GetMapping("/create")
	@ResponseBody
	public String createGenesisBlock() throws UnsupportedEncodingException {
		blockService.createGenesisBlock();
		return JSON.toJSONString(blockCache.getBlockChain());
	}
	
	/**
	 * 工作量证明PoW
	 * 挖矿生成新的区块 
	 */
	@GetMapping("/mine")
	@ResponseBody
	public String createNewBlock() throws UnsupportedEncodingException {
		powService.mine();
		return JSON.toJSONString(blockCache.getBlockChain());
	}

	/**
	 * 自私挖矿
	 * 挖矿生成新的区块
	 */
	@GetMapping("/selfishMine")
	@ResponseBody
	public String createSelfNewBlock() throws UnsupportedEncodingException {
		powService.selfishMine();
		return JSON.toJSONString(blockCache.getBlockChain());
	}

	/**
	 * 一直挖矿生成新的区块
	 */
	@GetMapping("/mining")
	@ResponseBody
	public String mining() throws UnsupportedEncodingException {
		long startTime = System.currentTimeMillis();
		while (System.currentTimeMillis() - startTime <= 300000) {
			powService.mine();
		}
		return JSON.toJSONString(blockCache.getBlockChain());
	}

	/**
	 * 一直自私挖矿
	 */
	@GetMapping("/selfishMining")
	@ResponseBody
	public String selfishMining() throws UnsupportedEncodingException {
		long startTime = System.currentTimeMillis();
		while (System.currentTimeMillis() - startTime <= 300000) {
			powService.selfishMine();
		}
		Message msg = new Message();
		msg.setType(BlockConstant.RESPONSE_BLOCKCHAIN);
		msg.setData(JSON.toJSONString(blockCache.getBlockChain()));
		p2PService.handleSelfMessage(JSON.toJSONString(msg));
		// 广播给全部节点
		p2PService.broadcast(JSON.toJSONString(msg));
		return JSON.toJSONString(blockCache.getBlockChain());
	}
}
