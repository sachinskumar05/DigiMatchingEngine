package com.digi.matching.exchange;

import com.digi.matching.exchange.order.FXOrder;

import java.util.Collection;

public interface OrderBook extends Book {
    Collection<FXOrder> getOrderHistory();
    FXOrder getOrder(Long orderId);
    boolean setOrder(FXOrder fxOrder);
    boolean removeOrder(FXOrder fxOrder);
}
