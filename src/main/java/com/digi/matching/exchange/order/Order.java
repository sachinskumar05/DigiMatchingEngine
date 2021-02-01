package com.digi.matching.exchange.order;

import java.io.Serializable;

public interface Order extends Serializable {

    Trade execute(long execId, double price, double fillQty, String ctrbClOrdId);
    Trade rollback(long execId, double price, double fillQty, String ctrbClOrdId);
    boolean isOpen();

}
