## Optimal bitcoin transaction fee payment strategy based on queuing game

At the peak of bitcoin transactions, users need to increase the transaction fee to compete for the limited block space in order to pack the transactions into the block as soon as possible. An optimal transaction fee payment strategy was proposed to solve the problem of how to choose the appropriate transaction fees. Taken the impact of transaction fee on transaction time into consideration, the authers analyse the relationship between transaction time and transaction fee and come out the nash equilibrium payment strategy.

1. Problem Formulation

   The authors assume that users don't know the number of transactions in the current system and the fees paied to the minners. The objective is to find the best strategy to minimize the total costs. 

2. the total costs of a transaction:
   $$
   totalCosts = x + cW(x)
   $$
   

   x: transaction fee, W(x): transaction time
   $$
   W(x) = \frac{1+x-\rho}{1+x-\rho}\frac{\rho}{1-\rho}\frac{1}{\mu}
   $$
   In this formulation, \rho means the traffic level and \miu means the transaction processing time.

3. Using the Nash equilibrium to solve the problem, we get
   $$
   x = \sqrt{\frac{cp^2(2-\rho)}{(1-\rho)\mu}}+\rho-1
   $$

4. 



## Applications of Game Theory in Blockchain

Kumaresean and Bentov have studied how to use Bitcoin to motivate participants. There are four ways.

- Verifiable computation. We consider a setting where a del- egator outsources computation to a worker who expects to get paid in return for delivering correct outputs. We design protocols that compile both public and private verification schemes to support incentivizations described above.

- Secure computation with restricted leakage. Building on the recent work of Huang et al. (Security and Privacy 2012), we show an efficient secure computation protocol that mone- tarily penalizes an adversary that attempts to learn one bit of information but gets detected in the process.

- Fair secure computation. Inspired by recent work, we con-

  sider a model of secure computation where a party that aborts

  after learning the output is monetarily penalized. We then

  propose an ideal transaction functionality F⋆ and show a ML

  constant-round realization on the Bitcoin network. Then, in the F⋆ -hybrid world we design a constant round protocol

  for secure computation in this model.

- Noninteractivebounties.Weprovideformaldefinitionsand candidate realizations of noninteractive bounty mechanisms on the Bitcoin network which (1) allow a bounty maker to place a bounty for the solution of a hard problem by sending a single message, and (2) allow a bounty collector (unknown at the time of bounty creation) with the solution to claim the bounty, while (3) ensuring that the bounty maker can learn the solution whenever its bounty is collected, and (4) pre- venting malicious eavesdropping parties from both claiming the bounty as well as learning the solution.

