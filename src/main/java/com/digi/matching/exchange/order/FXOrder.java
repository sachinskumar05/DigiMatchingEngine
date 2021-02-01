package com.digi.matching.exchange.order;

import com.digi.matching.exception.OrderCreationException;
import com.digi.matching.symbols.FXSymbol;
import com.digi.matching.types.OrderType;
import com.digi.matching.types.Side;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;

import static com.digi.matching.types.OrderType.LIMIT;

public class FXOrder implements Order {
    // Order is serializable for persistence / network (not featured as of today)
    private static final Logger logger = LogManager.getLogger(FXOrder.class);

    private final String clOrdId;
    private long orderId = Long.MIN_VALUE;
    private final FXSymbol fxSymbol;
    private final Side side;
    private final OrderType orderType;

    private double ordPx = 0.0d;
    private double avgPx = 0.0d;     //Average Execution Price
    private double lastPrice = 0.0d;    //Last Executed Price

    private double ordQty = Double.NaN;   //Order Qty
    private double cumQty = 0.0d;       // Cumulative executed Qty
    private double leavesQty = 0.0d;     //Remaining Qty
    private double lastQty = 0.0d;      //Last Executed Qty

    private String currency;

    private final Map<Long,Trade> tradeMap = new ConcurrentHashMap<>();

    private ReadWriteLock rwLock = new ReentrantReadWriteLock();
    private Lock readLock = rwLock.readLock();
    private Lock writeLock = rwLock.writeLock();

    private AtomicBoolean isOpen = new AtomicBoolean(true);

    private FXOrder(String clOrdId, FXSymbol fxSymbol, Side side, OrderType orderType) {
        this.clOrdId = clOrdId;
        this.fxSymbol = fxSymbol;
        this.side = side;
        this.orderType = orderType;
    }

    @Override
    public Trade execute(long execId, double fillPx, double fillQty, String ctrbClOrdId) {
        Trade trade = null;
        try {
            writeLock.lock();
            logger.debug( ()->"acquired order lock " );
            logger.debug( "executing execId,fillPx,fillQty=[{},{},{}] for clOrdId {} leavesQty is {} ",
                                execId, fillPx, fillQty, clOrdId, leavesQty);
            if (leavesQty == 0) {
                logger.error(()->"Order is fully filled");
                this.isOpen.set(false);
                return null;
            } else if (leavesQty < 0) {
                logger.error(()->"Order is over filled, please check");
                this.isOpen.set(false);
                return null;
            }

            double avgPx = ((this.avgPx * this.cumQty) + fillPx) / (this.cumQty + fillQty);
            double leavesQty = this.leavesQty - fillQty;

            this.lastPrice = fillPx;
            this.lastQty = fillQty;
            this.cumQty += fillQty;
            //Post all computation updating the order values with successful execution
            this.avgPx = avgPx;
            logger.debug("Before leaves qty {}, clOrdId {} ", this::getLeavesQty, this::getClientOrderId );
            this.leavesQty = leavesQty;
            logger.debug("After leaves qty {}, clOrdId {} ", this::getLeavesQty, this::getClientOrderId );
            trade = new Trade(getOrderId(), getSymbol(), fillPx, fillQty, getSide(), execId, ctrbClOrdId);
            addTrade( trade );
            if( this.leavesQty <= 0 )
                isOpen.set(false);
            else if( this.leavesQty > 0 )
                isOpen.set(false);
        } catch (Exception e) {
            logger.error(this.toString(), e);
        } finally {
            logger.debug(()->"unlocking order lock ");
            writeLock.unlock();
        }
        return trade;
    }


    @Override
    public Trade rollback( long execId, double fillPx, double fillQty, String ctrbClOrdId ) {
        Trade trade = null;
        try {
            writeLock.lock();
            logger.debug( ()->"will rollback execId,fillPx,fillQty=" + execId +"," + fillPx + "," + fillQty + ",  leavesQty=" + leavesQty);

            double avgPx = ((this.avgPx / this.cumQty) - fillPx) / (this.cumQty - fillQty);
            double leavesQty = this.leavesQty + fillQty;

            this.lastPrice = fillPx;
            this.lastQty = fillQty;
            this.cumQty -= fillQty;
            //Post all computation updating the order values with successful execution
            this.avgPx = avgPx;
            this.leavesQty = leavesQty;
            trade = new Trade(getOrderId(), getSymbol(), fillPx, fillQty, getSide(), execId, ctrbClOrdId);
            addTrade( trade );
        } catch (Exception e) {
            logger.error(this.toString(), e);
        } finally {
            logger.debug(()->"unlocking write lock ");
            writeLock.unlock();
        }
        return trade;
    }

    private void sendExecReport(Order order) {
        System.out.println("TODO: send execution report implementation");
    }

    ;

    @Override
    public boolean isOpen() {
        return isOpen.get();
    }

    public boolean isClosed() {
        return !isOpen();
    }

    public void setOrderId(long orderId) {
        if (Long.MIN_VALUE != this.orderId) return; //Already initialized
        this.orderId = orderId;
    }

    public long getOrderId() {
        return orderId;
    }

    public String getClientOrderId() {
        return clOrdId;
    }

    public FXSymbol getSymbol() {
        return fxSymbol;
    }

    public Side getSide() {
        return side;
    }

    public OrderType getOrderType() {
        return orderType;
    }

    public double getOrdPx() {
        return ordPx;
    }

    public double getAvgPx() {
        return avgPx;
    }

    public double getLastPrice() {
        return lastPrice;
    }

    public double getOrdQty() {
        return ordQty;
    }

    public double getCumQty() {
        return cumQty;
    }

    public double getLeavesQty() {
        return leavesQty;
    }

    public double getLastQty() {
        return lastQty;
    }

    public String getCurrency() {
        return currency;
    }

    public void addTrade(Trade trade) {
        tradeMap.put(trade.tradeId, trade);
    }
    public void removeTrade(Long tradeId) {
        tradeMap.remove(tradeId);
    }
    public void removeTrade(Trade trade) {
        tradeMap.remove(trade.tradeId);
    }

    public int getTradeCount() {
        return tradeMap.size();
    }

    public Collection<Trade> getTrades() {
        return (tradeMap.values());//Make the Trade objects immutable
    }

    public AtomicBoolean getIsOpen() {
        return isOpen;
    }

    public static class FXOrderBuilder {

        private final String clOrdId;
        private final FXSymbol instrument;
        private final Side side;
        private final OrderType ordTyp;

        public String currency;
        public double price = Double.NaN;
        public double qty;
        public double cumQty;
        public double leavesQty;

        public FXOrderBuilder(String clOrdId, String fxSymbolStr, Side side, OrderType ordTyp) {
            this.clOrdId  = clOrdId;
            this.instrument = FXSymbol.valueOf(fxSymbolStr);
            this.side = side;
            this.ordTyp = ordTyp;
        }

        /**
         * Use sample
         * FXOrder fxOrder = new FXOrderBuilder(O001, "BTC/USD", Side.BUY, OrderType.MARKET)
         *         .with(fxOrderBuilder -> {
         *             fxOrderBuilder.price = 20000.00;
         *             fxOrderBuilder.qty = 10.0;
         *             fxOrderBuilder.currency = "USD";
         *         })
         *         .build();
         * @param builderConsumer
         * @return
         */
        public FXOrderBuilder with(Consumer<FXOrderBuilder> builderConsumer) {
            builderConsumer.accept(this);
            return this;
        }

        public FXOrder build() throws OrderCreationException {
            FXOrder fxOrder = new FXOrder(this.clOrdId, this.instrument, this.side, this.ordTyp);
            if (this.ordTyp == LIMIT) {
                if(Double.isNaN(this.price)) {
                    throw new OrderCreationException("Limit order must have some price");
                }
                fxOrder.ordPx = this.price;
            }
            if(this.qty <= 0.0d) {
                throw new OrderCreationException("Invalid order Quantity " + qty + " for clOrdId = " + clOrdId );
            }
            fxOrder.leavesQty = this.qty;
            fxOrder.ordQty = this.qty;
            fxOrder.currency = this.currency;
            return fxOrder;
        }

    }

    @Override
    public Object clone() {
        FXOrder fxOrder = new FXOrder(this.clOrdId, this.fxSymbol, this.side, this.orderType);
        fxOrder.orderId = this.orderId;
        fxOrder.ordPx = this.ordPx;
        fxOrder.avgPx = this.avgPx;                 //Average Execution Price
        fxOrder.lastPrice = this.lastPrice;         //Last Executed Price
        fxOrder.ordQty = this.ordQty;               //Order Qty
        fxOrder.cumQty = this.cumQty;               // Cumulative executed Qty
        fxOrder.leavesQty = this.leavesQty;         //Remaining Qty
        fxOrder.lastQty = this.lastQty;             //Last Executed Qty
        fxOrder.currency = this.currency;
        fxOrder.tradeMap.putAll(this.tradeMap);     //Trade is immutable class
        fxOrder.isOpen.set(this.isOpen.get());
        return fxOrder;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FXOrder)) return false;

        FXOrder fxOrder = (FXOrder) o;

        if (!clOrdId.equals(fxOrder.clOrdId)) return false;
        if (!fxSymbol.equals(fxOrder.fxSymbol)) return false;
        return side == fxOrder.side;

    }

    @Override
    public int hashCode() {
        int result = clOrdId.hashCode();
        result = 31 * result + fxSymbol.hashCode();
        result = 31 * result + side.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "\nFXOrder{" +
                " clOrdId='" + clOrdId + '\'' +
                (orderId != Long.MIN_VALUE ? (", orderId=" + orderId) : ("")) +
                ", fxSymbol=" + fxSymbol +
                ", side=" + side +
                ", orderType=" + orderType +
                ", ordPx=" + ordPx +
                ", avgPx=" + avgPx +
                ", lastPrice=" + lastPrice +
                ", ordQty=" + ordQty +
                ", cumQty=" + cumQty +
                ", leavesQty=" + leavesQty +
                ", lastQty=" + lastQty +
                ", currency='" + currency + '\'' +
                ", isOpen=" + isOpen +
                ",\n Trade History " + tradeMap +
                '}';
    }
}
