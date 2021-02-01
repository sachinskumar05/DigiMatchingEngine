package com.digi.matching.exchange.order;

import com.digi.matching.symbols.FXSymbol;
import com.digi.matching.types.Side;

/**
 * KISS -> keeping final immutable variables public to avoid getter/setter creation..
 * TODO not a standard for prod env any delivery however, just ok for small demo
 */
public class Trade {
    public final long orderId;
    public final FXSymbol fxSymbol;
    public final double tradePrice;
    public final double tradeQty;
    public final Side tradeSide;
    public final long tradeId;
    public final String counterClOrdIdId;

    public Trade(long orderId, FXSymbol fxSymbol,
                 double tradePrice, double tradeQty,
                 Side tradeSide, long tradeId, String counterClOrdIdId){
        this.orderId = orderId;
        this.fxSymbol = fxSymbol;
        this.tradePrice = tradePrice;
        this.tradeQty = tradeQty;
        this.tradeSide = tradeSide;
        this.tradeId = tradeId;
        this.counterClOrdIdId = counterClOrdIdId;
    }

    @Override
    public String toString() {
        return "Trade{" +
                "orderId=" + orderId +
                ", fxSymbol=" + fxSymbol +
                ", tradePrice=" + tradePrice +
                ", tradeQty=" + tradeQty +
                ", tradeSide=" + tradeSide +
                ", tradeId=" + tradeId +
                ", counterClOrdIdId=" + counterClOrdIdId +
                '}';
    }
}
