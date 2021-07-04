# 更改的部分--Lethe

## Transaction

原文写的非常简单，hard coding，Tx只是msg的载体

Tx的功能`setBussinessInfo`，`setId` 

原文假定一个block只有两个tx（体现在`PoWService.mine()`和`createGenisisBlock`里面）

- tx1包含msg（这个区块是创世区块 或者 Miner的IP port信息）
- tx2包含msg（区块链的高度是`blockCache.getLatestBlock().getIndex()+1`），这里高度应该是用HashMap类型的blockChain记录（参考hwk2）

修改：

- 把hwk2里面的Tx内容部分黏贴过来，主要有`input output coinbase `的内容
- 对于后面block使用`block.getMinerAddress`来说，其实是从这个txs里面拿到coinbaseTx，然后getoutput(0)就拿到miner的publicKey作为miner的address（为了后面的blackList和whiteList）

## Block

原文的block中，就是储存`prevBlockHash,tsaList,nonce,blockHash`的东西，这里我们简单起见，也是这么设置

```java
public Block(byte[] prevHash, List<Transaction> blockTxs, byte[] thisHash) {
		prevBlockHash = prevHash;
		coinbase = blockTxs.get(0);
		txs = blockTxs;
		hash = thisHash;
	}
public void setNonce(int nonce) {
		this.nonce = nonce;
	}
```

就直接输入相关内容，上面是修改完的block

## PoWService

`public Block mine()`里面对于`Transaction`的部分都进行了修改

```java
// 封装业务数据集合，记录区块产生的节点信息，临时硬编码实现
List<Transaction> tsaList = new ArrayList<Transaction>();
Transaction tsa1 = new Transaction();
tsa1.setId("1");
tsa1.setBusinessInfo("这是IP为："+CommonUtil.getLocalIp()+"，端口号为："+blockCache.getP2pport()+"的节点挖矿生成的区块");
tsaList.add(tsa1);
Transaction tsa2 = new Transaction();
tsa2.setId("2");
tsa2.setBusinessInfo("区块链高度为："+(blockCache.getLatestBlock().getIndex()+1));
tsaList.add(tsa2);
```

改成

```java
KeyPair keyPairMiner;
keyPairMiner = keyPairGen.generateKeyPair();
//TODO: 把下面的换成coinBaseTxs之类的
List<Transaction> tsaList = new ArrayList<Transaction>();
Transaction tsa1 = new Transaction(COINBASE,keyPairMiner.getPublic());
tsa1.setId("1");
tsa1.setBusinessInfo("这是IP为："+CommonUtil.getLocalIp()+"，端口号为："+blockCache.getP2pport()+"的节点挖矿生成的区块\n" +
		"Miner Address = "+keyPairMiner.getPublic());
tsa1.setMinerIPAddress(CommonUtil.getLocalIp()+":"+blockCache.getP2pport());
tsaList.add(tsa1);
Transaction tsa2 = new Transaction();
tsa2.setId("2");
tsa2.setBusinessInfo("区块链高度为："+(blockCache.getLatestBlock().getIndex()+1));
tsaList.add(tsa2);
```

这里对于miner，创建keyPair，**注意TODO：每次进行mine的时候，keyPair应该是一致的，如何实现？？？**用`keyPairMiner.getPublic()`来作为coinbaseTx的receiver，也就是记录了这个block的miner的address



原文对于`blockHash`都是以`String`类型，我们改成和作业一致的`byte[]`，创建新区块的时候，我们直接输入`prevBlockHash,tsaList,nonce`因为`thisBlockHash = H(prevBlockHash|tsaList|nonce)`，方便起见，我们直接把计算好的blockHash也放进去

```java
String newBlockHash = ""; //line 53
// 创建新的区块
Block block = blockService.createNewBlock(nonce, blockCache.getLatestBlock().getHash(), newBlockHash, tsaList);
		
```

```java
byte[] newBlockHash;
// 创建新的区块
Block block = blockService.createNewBlock(nonce, blockCache.getLatestBlock().getHash(), tsaList, newBlockHash);
```
