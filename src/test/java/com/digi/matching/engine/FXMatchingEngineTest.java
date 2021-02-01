package com.digi.matching.engine;

import com.digi.matching.client.FXClientBuy;
import com.digi.matching.client.FXClientSell;
import com.digi.matching.exchange.order.FXOrder;
import com.digi.matching.exchange.order.Trade;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.digi.matching.types.OrderType.LIMIT;
import static org.junit.jupiter.api.Assertions.*;

class FXMatchingEngineTest {


    private static FXMatchingEngine fxMatchingEngine;
    private static FXClientSell fxClientSell ;
    private static FXClientBuy fxClientBuy ;
    private static ExecutorService executorService ;

    @BeforeAll
    public static void init(){
        fxMatchingEngine = FXMatchingEngine.getInstance();
        fxClientSell = new FXClientSell();
        fxClientBuy = new FXClientBuy();
        executorService = Executors.newFixedThreadPool(5);
    }
    @Test
    public void testSimpleBuyAndSell() throws InterruptedException {
        fxClientSell.createAndSubmitOrder(1, 20000.0d, 10, LIMIT);
        fxClientBuy.createAndSubmitOrder(1, 20000.0d, 10, LIMIT);
        Thread.sleep(100);

        List<FXOrder> fxClientSellOrdList = fxClientSell.getClientOrders();
        List<FXOrder> fxClientBuyOrdList  = fxClientBuy.getClientOrders();
        FXOrder sellFXOrder =  fxClientSellOrdList.get(0);
        FXOrder buyFXOrder =  fxClientBuyOrdList.get(0);

        System.out.println(sellFXOrder);
        System.out.println(buyFXOrder);

        assertEquals(sellFXOrder.getTradeCount(), 1);
        assertEquals(buyFXOrder.getTradeCount(), 1);

        List<Trade> sellTrades = new ArrayList<>(sellFXOrder.getTrades());
        List<Trade> buyTrades = new ArrayList<>(buyFXOrder.getTrades());

        assertEquals(sellTrades.get(0).counterClOrdIdId, buyFXOrder.getClientOrderId());
        assertEquals(buyTrades.get(0).counterClOrdIdId, sellFXOrder.getClientOrderId());

        assertEquals(sellTrades.get(0).tradeQty, sellFXOrder.getOrdQty()-sellFXOrder.getLeavesQty());
        assertEquals(sellTrades.get(0).tradeQty, buyFXOrder.getOrdQty()-buyFXOrder.getLeavesQty());
        assertEquals(buyTrades.get(0).tradeQty, sellTrades.get(0).tradeQty);

    }
}