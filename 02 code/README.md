# final0715

这里我们对于selfish mining的策略进行了进一步的修改，优化了该策略的reward

# finalVersion4pre

这是我们最终pre的版本，实现功能

- difficulty level：前面多少个0，demo方便设置为4
- scan：展示整个block chain
- create：创世区块，这个miner永远是Genesis，所以谁create无所谓

- mining：可以一直挖矿，目前设置是30s
- selfishMing：一直自私挖矿，目前设置是30s
  - 可以拉人入伙一起selfish mining，你们之间信息是共享的
- reset：直接清空chain上内容，不用大家一起重新启动code了
- display：只显示目前每个block的高度和mineradd

## application.yml

```yml
server:
    port: 8080

block:
    ## 设定哈希值前difficulty位都是0时,满足计算结果
    difficulty: 4
    ## 当前节点p2p server端口号
    p2pport: 7020
    ## 要连接的正常节点地址
    address: https://10.2.96.237:7020,https://10.2.98.173:7020,https://10.2.82.101:7020,https://10.2.82.56:7020
    ## 是否自私挖矿
    selfish: false
    ## 要连接的自私节点地址
    selfishAddress: https://10.2.96.237:7020

address:
    ## 我的ID
    mineradd: Lethe
    ## 创世区块的ID
    genesisadd: Genesis
    ## 黑名单（不接受其区块）
    blacklist: SunBo
    ## 白名单（一起自私挖矿的ID），只有block.selfish为true时有用
    whitelist: SunBo
```

address--mineradd修改成自己的名字（按理说应该是publicKey之类的，为了demo展示的方便，直接用String表示）

blacklist：如果有想屏蔽的人的block，那么就把这个人的mineradd加进来

whitelist：如果你想和别人一起selfish mine，把这个人的mineradd加进来

# 更改的部分-- Version1

@author Lethe

**请看Version2 orz 谢谢**

这里对于blockHash和publicKey的修改没有用！

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



# Version2

@author Lethe

## 变量类型更改！！hash和minerAdd

为了展示方便，这里把blockHash还是改为了String类型，minerAddress也是String（之前是byte[], PublicKey）

为了让每次运行这个程序的时候minerAddress都是一样的（按理说一个account就是一个固定的keyPair，我们加入blackList的也是这个keyPair的publicKey），我们直接在`application.yml`文件里加入一个配置`address`其下有`mineradd`和`genesisadd`代表这个miner的address（方便起见直接设置一个String比如minderN1，每个矿工的不一样呢）以及创世区块的miner（中本聪大哥）的address（这里每个矿工的都是一样的）

## Chrome JSONView 插件

可以直接用这个插件，把blockChain的信息变成JSON，预览版本在这里

# selfMine

在最原版的ref的基础上修改的

实现selfishMining的策略了！！！



