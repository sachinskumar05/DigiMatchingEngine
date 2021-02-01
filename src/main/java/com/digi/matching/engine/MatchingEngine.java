package com.digi.matching.engine;

import com.digi.matching.exception.OrderCreationException;
import com.digi.matching.exchange.OrderBook;
import com.digi.matching.exchange.order.Order;
import com.digi.matching.exchange.order.Trade;
import com.digi.matching.symbols.Symbol;

import java.util.List;

public interface MatchingEngine {

    OrderBook getOrderBook(Symbol symbol);
    List<Trade> getTrades(Symbol symbol);
    void addOrder(Order order) throws OrderCreationException;
    void cancelOrder(Order order);
    void amendOrder(Order order);

}
