package com.digi.matching;

import com.digi.matching.client.FXClientBuy;
import com.digi.matching.client.FXClientSell;
import com.digi.matching.engine.FXMatchingEngine;
import com.digi.matching.symbols.FXSymbol;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.digi.matching.types.OrderType.LIMIT;
import static com.digi.matching.util.StringUtil.BTC_USD;

public class DigiMatchingEngineApplication {
	private static final Logger logger = LogManager.getLogger(DigiMatchingEngineApplication.class);



	public static void main(String[] args) {
		logger.info("Welcome to Digital Matching Engine Application ");

		FXMatchingEngine fxMatchingEngine = FXMatchingEngine.getInstance();

		FXClientSell fxClientSell = new FXClientSell();
		FXClientBuy fxClientBuy = new FXClientBuy();


		ExecutorService executorService = Executors.newFixedThreadPool(2);

		executorService.submit(()->fxClientSell.createAndSubmitOrder(1, 20000.0d, 10, LIMIT));

		executorService.submit(()->fxClientBuy.createAndSubmitOrder(1, 20000.0d, 10, LIMIT));

		try {
			Thread.sleep(3000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		logger.info( ()-> fxMatchingEngine.getOrderBook(FXSymbol.valueOf(BTC_USD)));
		logger.info( "Order History {} " , ()-> fxMatchingEngine.getOrderBook(FXSymbol.valueOf(BTC_USD)).getOrderHistory());


		Runtime.getRuntime().addShutdownHook(new Thread(executorService::shutdown));

	}



}
