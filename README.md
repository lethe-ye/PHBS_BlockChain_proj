This is for the final project of PHBS_m4_block_chain.

# 2021/07/04

## [preview](https://github.com/lethe-ye/PHBS_BlockChain_proj/blob/main/02%20code/version2/minePreview.htm)

## TODO

### p2pclient连接问题

miner1接收不到miner0的信息

miner0开始mine了之后，broadcast这个msg给miner1，但是miner1显示`create instance error`

- 原因应该是 block在序列化之后，反序列化的时候，关于Transaction的信息缺失了！！
- ![image-20210705165650389](C:\Users\Mengjie Ye\AppData\Roaming\Typora\typora-user-images\image-20210705165650389.png)
- 传入handleBlockChainResponse的信息和函数内的blockData不一样（后者直接是空的了！）
- ![image-20210705170416064](C:\Users\Mengjie Ye\AppData\Roaming\Typora\typora-user-images\image-20210705170416064.png)
- 问题在这里

### blockChain类型

block chain目前是在blockCache里面，以list的形式存储，要不要改成HashMap？？

把hwk2的关于这一块的code再搬过来

