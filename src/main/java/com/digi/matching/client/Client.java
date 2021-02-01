package com.digi.matching.client;

import com.digi.matching.exception.OrderCreationException;
import com.digi.matching.exchange.executions.ExectionReport;
import com.digi.matching.exchange.order.FXOrder;
import com.digi.matching.exchange.order.Order;

import java.util.List;

public interface Client {
    void submitOrder(Order order) throws OrderCreationException;
    void replaceOrder(Order order) throws OrderCreationException;
    void cancelOrder(Order order) throws OrderCreationException;
    List<FXOrder> getClientOrders();
}
