```java
//创建成功后，全网广播出去
Message msg = new Message();
msg.setType(BlockConstant.RESPONSE_LATEST_BLOCK);
msg.setData(JSON.toJSONString(block));

//先不broadcast
p2PService.broatcast(JSON.toJSONString(msg));

```

- 一直mine，直到收到别人的消息
  - 如果消息 的index里面比我blockCache里面的index小（Block.getIndex方法既可以）
    - 把我blockCache里blockChain的从这个block开始到broadcast两个出来，这样别人会承认这个链
  - 如果我还没mine到，就接着mine

- ！！！原来的code
  - mine成功之后先broadcast，别人收到后，把msg返回
  - 收到返回的msg的时候，才开始处理这个返回的msg里面的信息，更新自己的blockCache里面的blockchain
- 当我node1.selfMine之后，node2.mine的msg传到node1.client.onMessage--> p2pservice.handleBlockResponse(type=2)
  - latestBlock.getHash().equals(latestBlockReceived.getPreviousHash()))是false，也就是已经hash对不上了，所以直接不进行处理了
  - 这里应该需要改一下了

## 改动--test0里面改的

- 注意目前的index是从1开始的！！
  - related：blockCache里面getPartialBlockChain

- powserivce mine就把最后几行注释

- p2pservice加一个handleSelfMessage，用来在本地blockCache上储存自己selfishmining的东西

- 把handleBlockResponse的第二个参数去掉了，这样handleSelfMessage可以直接用这个方法（第二个参数，这个函数一直没有用） handleBlockChainResponse也删掉

- client是用来收到别的节点msg的
  server是发送这边msg给别的节点的

- p2pservice:添加了responseBlockMsg方法--对应的block.getIndex方法

  - ```java
    // 如果拿到的block比我自己selfish mine的最后一个block的index小，说明我至少领先别人1个block
    // 我就应该把我自己的blockChain broadcast
    // TODO: 应该指broadcast 最近的两个block
    // 不如直接broadcast index = latestBlockReceived.getIndex() 和 +1 的这两个block？
    ```

    因为是要先收到别人的broadcast之后处理，唯一的优点是可以直接覆盖掉自己mine到的东西（mine完了 要别人发回给你才有效），但也有可能node3发回了，node1self发回自己的2个block，这样按理说node23都会更新新的blockChain？？？ 应该直接broadcast，blockChain

  - blockCache添加方法getPartialBlockChain，获取到i为止的blockChain

  - ```
    public List<Block> getPartialBlockChain(int id){
    List<Block> partialBlockChain = new CopyOnWriteArrayList<Block>();
    for (int i=0;i<= id;i++){
    partialBlockChain.add(blockChain.get(i));
    }
    return partialBlockChain;
    }
    ```

    

## 测试

- http://localhost:8080/create
- http://localhost:8081/create
- http://localhost:8080/mine
- http://localhost:8081/mine
- http://localhost:8081/mine
- http://localhost:8080/selfMine
- http://localhost:8080/selfMine（这个时候高度显示5）
- http://localhost:8081/mine（这个时候高度显示4）
- 这个时候应该会有覆盖blockChain的表现
- http://localhost:8081/scan（这个时候高度显示5，8080的chain已经覆盖！）