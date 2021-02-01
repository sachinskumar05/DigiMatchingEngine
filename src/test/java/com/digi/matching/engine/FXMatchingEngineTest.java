package com.digi.matching.engine;

import com.digi.matching.client.FXClientBuy;
import com.digi.matching.client.FXClientSell;
import com.digi.matching.exchange.order.FXOrder;
import com.digi.matching.exchange.order.Trade;
import org.junit.jupiter.api.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.digi.matching.types.OrderType.LIMIT;
import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FXMatchingEngineTest {


    private static FXMatchingEngine fxMatchingEngine;
    private static FXClientSell fxClientSell ;
    private static FXClientBuy fxClientBuy ;
    private static ExecutorService executorService ;

    @BeforeAll
    public static void init(){
        fxMatchingEngine = FXMatchingEngine.getInstance();
        executorService = Executors.newFixedThreadPool(5);
    }

    @BeforeEach
    public void clientInit(){
        fxClientSell = new FXClientSell();
        fxClientBuy = new FXClientBuy();
    }
    @Test
    @Order(1)
    public void testSimpleBuyAndSell() throws InterruptedException {
        fxClientSell.createAndSubmitOrder(1, 20000.0d, 10, LIMIT);
        fxClientBuy.createAndSubmitOrder(1, 20000.0d, 10, LIMIT);

        Thread.sleep(50);

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

    @Test
    @Order(2)
    public void testSimpleBuyLowAndSellHigh() throws InterruptedException {

        Thread.sleep(50);
        fxClientSell.createAndSubmitOrder(1, 20000.0d, 10, LIMIT);
        fxClientBuy.createAndSubmitOrder(1, 19000.0d, 10, LIMIT);
        List<FXOrder> fxClientSellOrdList = fxClientSell.getClientOrders();
        List<FXOrder> fxClientBuyOrdList  = fxClientBuy.getClientOrders();
        Thread.sleep(50);

        FXOrder sellFXOrder =  fxClientSellOrdList.get(0);
        FXOrder buyFXOrder =  fxClientBuyOrdList.get(0);

        System.out.println(sellFXOrder);
        System.out.println(buyFXOrder);

        assertEquals(sellFXOrder.getTradeCount(), 0);
        assertEquals(buyFXOrder.getTradeCount(), 0);

        Thread.sleep(50);
        fxClientSell.createAndSubmitOrder(1, 19000.0d, 10, LIMIT);//Clearing book
        fxClientBuy.createAndSubmitOrder(1, 20000.0d, 10, LIMIT);//Clearing book

    }
}