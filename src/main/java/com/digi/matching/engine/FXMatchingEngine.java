package com.digi.matching.engine;

import com.digi.matching.exception.OrderCreationException;
import com.digi.matching.exchange.OrderBook;

import com.digi.matching.exchange.FXOrderBook;
import com.digi.matching.exchange.order.FXOrder;
import com.digi.matching.exchange.order.Order;
import com.digi.matching.exchange.order.Trade;
import com.digi.matching.symbols.FXSymbol;
import com.digi.matching.symbols.Symbol;
import com.digi.matching.types.Side;

import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static com.digi.matching.types.OrderType.LIMIT;
import static com.digi.matching.types.OrderType.MARKET;
import static com.digi.matching.types.Side.BUY;
import static com.digi.matching.types.Side.SELL;


public class FXMatchingEngine implements MatchingEngine {
    private static final Logger logger = LogManager.getLogger(FXMatchingEngine.class);

    private static final AtomicLong currentOrderId = new AtomicLong();

    ExecutorService executorForMatching = Executors.newFixedThreadPool(5);

    private FXMatchingEngine() {
         for(FXSymbol fxSymbol :FXSymbol.getAllSymbols() ) {
            FXOrderBook.get(fxSymbol);//Pre initialization
        }
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        throw new CloneNotSupportedException("FXMatchingEngine can't be cloned");
    }

    @Override
    public OrderBook getOrderBook(Symbol symbol) {
        if (symbol instanceof FXSymbol) {
            return FXOrderBook.get((FXSymbol) symbol);
        }
        return null;
    }

    @Override
    public List<Trade> getTrades(Symbol symbol) {
          return null;
    }

    @Override
    public void addOrder(Order order) throws OrderCreationException {
        FXOrder fxOrder = (FXOrder) order;

        if(fxOrder.getSide() != BUY && fxOrder.getSide() != SELL) {
            logger.error("Invalid SIDE {} for clOrdId {} ",
                    fxOrder::getSide, fxOrder::getClientOrderId);
            return ;
        }
        if(fxOrder.getOrderType() != LIMIT && fxOrder.getOrderType() != MARKET) {
            logger.error("Invalid ORDER TYPE {} for clOrdId {} ",
                    fxOrder::getOrderType, fxOrder::getClientOrderId);
            return ;
        }
        //Locate the order book
        FXOrderBook orderBook = (FXOrderBook) getOrderBook(fxOrder.getSymbol());
        if(null == orderBook ) {
            throw new OrderCreationException("Unknown security/security received symbol in order " + fxOrder.getSymbol());
        }
        fxOrder.setOrderId(currentOrderId.incrementAndGet());
        logger.info("Received to add clOrdId {}, side {}, price {}, qty {}, order id {} ",
                fxOrder::getClientOrderId, fxOrder::getSide, fxOrder::getOrdPx, fxOrder::getOrdQty, fxOrder::getOrderId);

        executorForMatching.submit( ()-> orderBook.processOrder(fxOrder));//Submitted for possible execution

    }


    @Override
    public void cancelOrder(Order order) {
        throw new UnsupportedOperationException("Order Cancellation is not supported. This project is for execution demo only");
    }

    @Override
    public void amendOrder(Order order) {
        throw new UnsupportedOperationException("Order Modification is not supported. This project is for execution demo only");

    }


    private static FXMatchingEngine instance = new FXMatchingEngine();
    public static FXMatchingEngine getInstance() {
        if (instance == null) {
            synchronized (FXMatchingEngine.class) {
                if ( instance == null )
                    instance = new FXMatchingEngine();
            }
        }
        return instance;
    }
}
