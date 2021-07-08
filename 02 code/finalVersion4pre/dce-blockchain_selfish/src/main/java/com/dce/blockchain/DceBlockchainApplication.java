package com.dce.blockchain;

import com.dce.blockchain.web.controller.BlockController;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.UnsupportedEncodingException;

@SpringBootApplication
public class DceBlockchainApplication {

    public static void main(String[] args) throws UnsupportedEncodingException {
        SpringApplication.run(DceBlockchainApplication.class, args);
    }

}


