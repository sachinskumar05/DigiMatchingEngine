package com.digi.matching.exchange;

import com.digi.matching.exchange.order.FXOrder;
import com.digi.matching.symbols.FXSymbol;
import com.digi.matching.types.Side;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static com.digi.matching.types.OrderType.LIMIT;
import static com.digi.matching.types.OrderType.MARKET;
import static com.digi.matching.types.Side.BUY;
import static com.digi.matching.types.Side.SELL;

public class FXOrderBook implements OrderBook, Serializable {
    private static final Logger logger = LogManager.getLogger(FXOrderBook.class);

    private static final Map<FXSymbol, FXOrderBook> orderBookCache = new ConcurrentHashMap<>();

    private static final AtomicLong currentTradeId = new AtomicLong();


    private final transient ReadWriteLock rrwLock = new ReentrantReadWriteLock();
    public  final transient Lock readLock = rrwLock.readLock();
    public  final transient Lock writeLock = rrwLock.writeLock();

    private final FXSymbol fxSymbol;
    private final SortedMap<Double,List<FXOrder>> fxBidOrderSortedMap = new ConcurrentSkipListMap<>();
    private final SortedMap<Double,List<FXOrder>> fxAskOrderSortedMap = new ConcurrentSkipListMap<>();

    private final Map<Long,FXOrder> orderHistory = new LinkedHashMap<>();

    private FXOrderBook( FXSymbol fxSymbol ) {
        this.fxSymbol = fxSymbol;
    }

    public FXSymbol getSymbol() {
        return fxSymbol;
    }

    public static FXOrderBook get(FXSymbol fxSymbol) {//Flyweight
        orderBookCache.putIfAbsent(fxSymbol, new FXOrderBook(fxSymbol));
        return orderBookCache.get(fxSymbol);
    }

    @Override
    public boolean setOrder(FXOrder fxOrder) {
        try {
            writeLock.lock();
            orderHistory.put(fxOrder.getOrderId(),fxOrder);
            if (fxOrder.getSide() == BUY) {
                return setBid(fxOrder);
            } else if (fxOrder.getSide() == SELL) {
                return setAsk(fxOrder);
            }

        } finally {
            writeLock.unlock();
        }
        return false;
    }

    public boolean processOrder(FXOrder fxOrder) {
        try {
            writeLock.lock();
            FXOrderBook fxOrderBook = get(fxOrder.getSymbol());
            fxOrderBook.setOrder(fxOrder);

            logger.debug(()->"Acquiring Transaction Lock for matching on OrderBook of symbol = " + fxOrderBook.getSymbol());

            List<FXOrder> bestOppositeOrderList = null;
            Side side = fxOrder.getSide();
            String clOrdId = fxOrder.getClientOrderId();
            logger.debug(()-> clOrdId +", side "+ side + " order received... will try to match with opposite side for best price.");
            if( side == BUY) {
                bestOppositeOrderList = fxOrderBook.getBestAsk();
            } else {
                bestOppositeOrderList = fxOrderBook.getBestBid();
            }
            if ( null == bestOppositeOrderList || bestOppositeOrderList.isEmpty() ) {
                logger.info( ()->"No Opposite Order Exists for side = " + side );
                return false;
            }
            logger.debug("before loop {}, {}" , fxOrder::getClientOrderId, fxOrder::getLeavesQty);

            while ( fxOrder.getLeavesQty() > 0 && null != bestOppositeOrderList && !bestOppositeOrderList.isEmpty() ) {

                logger.debug("inside loop --- {}, {}" , fxOrder::getClientOrderId, fxOrder::getLeavesQty);
                if( fxOrder.getLeavesQty() <= 0 || fxOrder.isClosed() || bestOppositeOrderList.isEmpty() ) {
                    break;
                }
                double bestOppositePrice;
                if( side == BUY) {
                    bestOppositePrice = fxOrderBook.getBestAskPrice();
                    if( fxOrder.getOrdPx() < bestOppositePrice ) {
                        double finalBestOppositePrice = bestOppositePrice;
                        logger.debug("Price can't Match as Bid/BUY price {} is lower than best opposite price {}",
                                ()->fxOrder.getOrdPx(), ()-> finalBestOppositePrice);
                        break;
                    }
                } else {
                    bestOppositePrice = fxOrderBook.getBestBidPrice();
                    if( fxOrder.getOrdPx() > bestOppositePrice ) {
                        double finalBestOppositePrice = bestOppositePrice;
                        logger.debug("Price can't Match as Ask/SELL price {} is higher than best opposite price {}",
                                ()->fxOrder.getOrdPx(), ()-> finalBestOppositePrice);
                        break;
                    }
                }

                List<FXOrder> finalList = bestOppositeOrderList;
                logger.debug( "--- clOrdId {}, Opposite Orders {}" , fxOrder::getClientOrderId, ()-> finalList);

                ListIterator<FXOrder> listIterator = bestOppositeOrderList.listIterator();
                while (listIterator.hasNext()) { //Iterate based on receiving sequence
                    FXOrder bestOppositeOrder = listIterator.next();
                    if(fxOrder.getOrderType() == MARKET && bestOppositeOrder.getOrderType() == MARKET) {
                        logger.debug(() -> "Matching can't be done as BUY and SELL both orders are MARKET Order");
                        continue;
                    }
                    if (null != bestOppositeOrder &&
                            (fxOrder.getOrderType() == MARKET || bestOppositeOrder.getOrderType() == MARKET
                                    || fxOrder.getOrdPx() == 0.0d || //0.0d => MKT order
                                    ( (side == BUY && fxOrder.getOrdPx() >= bestOppositeOrder.getOrdPx()) ||
                                       (side == SELL && fxOrder.getOrdPx() <= bestOppositeOrder.getOrdPx())
                                    )
                            )
                    ) {
                        double matchQty = Math.min(fxOrder.getLeavesQty(), bestOppositeOrder.getLeavesQty());
                        logger.debug("Match qty {} for side {} and clOrdId {} with opposite side {} and clOrdId {}" ,
                                ()->matchQty, ()->side, ()->clOrdId, ()->bestOppositeOrder.getSide(),
                                ()->bestOppositeOrder.getClientOrderId());

                        if (matchQty <= 0.0d) {
                            logger.warn(() -> "Match qty should be larger than 0, no matching found");
                            continue;
                        }
                        double matchPx = bestOppositeOrder.getOrdPx();
                        if( fxOrder.getOrderType() == MARKET || bestOppositeOrder.getOrderType() == LIMIT ) {
                            matchPx = bestOppositeOrder.getOrdPx();
                        } else if ( fxOrder.getOrderType() == LIMIT || bestOppositeOrder.getOrderType() == MARKET ) {
                            matchPx = fxOrder.getOrdPx();
                        }

                        double finalMatchPx = matchPx;
                        logger.debug("Match price {} for side {} and clOrdId {} with opposite side {} and clOrdId {}" ,
                                ()-> finalMatchPx, ()->side, ()->clOrdId, ()->bestOppositeOrder.getSide(),
                                ()->bestOppositeOrder.getClientOrderId());

                        //Generate aggressive trade
                        fxOrder.execute(currentTradeId.incrementAndGet(), matchPx, matchQty, bestOppositeOrder.getClientOrderId());

                        //# Generate the passive executions
                        bestOppositeOrder.execute(currentTradeId.incrementAndGet(), matchPx, matchQty, fxOrder.getClientOrderId());

                    }
                    if (bestOppositeOrder.getLeavesQty() == 0) {
                        listIterator.remove();
                        List<FXOrder> finalList2 = bestOppositeOrderList;
                        logger.debug(()-> bestOppositeOrder.getClientOrderId() + " is removed from matching book as bestOppositeOrder? " +
                                !finalList2.contains(bestOppositeOrder));
                    }
                    if (bestOppositeOrder.getLeavesQty() < 0) {
                        logger.info(()->"Order over executed [Check fill logic if happened ] fxOrder = " + bestOppositeOrder);
                        listIterator.remove();
                        List<FXOrder> finalList2 = bestOppositeOrderList;
                        logger.debug(()-> "Overfilled however " + bestOppositeOrder.getClientOrderId() +
                                " is removed from matching book as bestOppositeOrder " + !finalList2.contains(bestOppositeOrder));
                    }
                    if (fxOrder.getLeavesQty() == 0) {
                        boolean isRemoved = fxOrderBook.removeOrder(fxOrder);
                        List<FXOrder> finalList2 = bestOppositeOrderList;
                        logger.debug(()-> fxOrder.getClientOrderId() + " is Removed from matching book? " +
                                !finalList2.contains(bestOppositeOrder));
                        return isRemoved;
                    }
                    if (fxOrder.getLeavesQty() < 0) {
                        logger.info(()->"Order over executed [Check fill logic if happened ] fxOrder = " + fxOrder);
                        boolean isRemoved = fxOrderBook.removeOrder(fxOrder);
                        List<FXOrder> finalList2 = bestOppositeOrderList;
                        logger.debug(()-> "Overfilled but is Removed bestOppositeOrder " +
                                !finalList2.contains(bestOppositeOrder));
                        return isRemoved;
                    }

                }
                if (fxOrder.getLeavesQty() > 0 && (null == bestOppositeOrderList || bestOppositeOrderList.isEmpty()) ) {
                    logger.debug(()->"Check for the next best price opposite side of order " + fxOrder);
                    if( fxOrder.getSide() == BUY) {
                        bestOppositeOrderList = fxOrderBook.getBestAsk();
                    } else {
                        bestOppositeOrderList = fxOrderBook.getBestBid();
                    }
                    if( null == bestOppositeOrderList || bestOppositeOrderList.isEmpty()) break;
                }

            }

        }catch (Exception e) {
            logger.error("Exception while order matching {}", ()-> e.getCause(), ()-> e );
        } finally {
            logger.debug(()->"Releasing Transaction Lock for matching");
            writeLock.unlock();
        }

        return false;
    }

    private boolean setAsk( FXOrder fxOrder ) {
        if( fxOrder.getSide() != SELL ) {
            logger.error("Wrong side, only SELL can be set for ask fxOrder = {}" , ()-> fxOrder);
            return false;
        }
        List<FXOrder> fxOrderList = fxAskOrderSortedMap.computeIfAbsent(fxOrder.getOrdPx(), k-> new ArrayList<>());
        return setOrderToList(fxOrderList, fxOrder);
    }

    private boolean setOrderToList(List<FXOrder> fxOrderList, FXOrder fxOrder) {

        if(fxOrderList.contains(fxOrder)) {
            logger.error("Duplicate Ask order received {}" , ()-> fxOrder);
            return false;
        }

        return fxOrderList.add(fxOrder);
    }

    private boolean setBid(FXOrder fxOrder) {
        if( fxOrder.getSide() != BUY ) {
            logger.error("Wrong side, only BUY can be set for bid fxOrder = {}" , ()->fxOrder);
            return false;
        }
        List<FXOrder> fxOrderList = fxBidOrderSortedMap.computeIfAbsent(fxOrder.getOrdPx(), k-> new ArrayList<>());
        return setOrderToList(fxOrderList, fxOrder);
    }

    public List<FXOrder> getBestBid() {
        if( fxBidOrderSortedMap.isEmpty() ) return null;
        return fxBidOrderSortedMap.get(fxBidOrderSortedMap.lastKey());
    }

    public double getBestBidPrice() {
        if( fxBidOrderSortedMap.isEmpty() ) return Double.NaN;
        return (fxBidOrderSortedMap.lastKey());
    }

    public boolean removeBid(FXOrder fxOrder) {
        if( fxBidOrderSortedMap.isEmpty() ) {
            logger.error(" fxBidOrderSortedMap is empty, potential indication of race condition bug, can't removed order {}" , ()->fxOrder);
            return false;
        }
        List<FXOrder> fxoList = fxBidOrderSortedMap.computeIfPresent(fxOrder.getOrdPx(), (px,fxol)-> {fxol.remove(fxOrder); return fxol;});
        logger.debug("List of Bids on price {}, {} " , ()->fxOrder.getOrdPx(), ()->fxBidOrderSortedMap.get(fxOrder.getOrdPx()));
        return null!=fxoList && !fxoList.contains(fxOrder);
    }

    public List<FXOrder> getBestAsk() {
        if( fxAskOrderSortedMap.isEmpty() ) return null;
        return fxAskOrderSortedMap.get(fxAskOrderSortedMap.firstKey());
    }

    public double getBestAskPrice() {
        if( fxAskOrderSortedMap.isEmpty() ) return Double.NaN;
        return (fxAskOrderSortedMap.firstKey());
    }

    public boolean removeAsk(FXOrder fxOrder) {
        if( fxAskOrderSortedMap.isEmpty() ) {
            logger.error(" fxAskOrderSortedMap is empty, potential indication of race condition bug, can't removed order {}" , ()->fxOrder);
            return false;
        }
        List<FXOrder> fxoList = fxAskOrderSortedMap.computeIfPresent(fxOrder.getOrdPx(), (px,fxol)-> {fxol.remove(fxOrder); return fxol;});
        logger.debug("List of Asks on price {}, {} " , ()->fxOrder.getOrdPx(), ()->fxAskOrderSortedMap.get(fxOrder.getOrdPx()));
        return null!=fxoList && !fxoList.contains(fxOrder);
    }

    @Override
    public boolean removeOrder(FXOrder fxOrder) {
        if( fxOrder.getSide() == BUY ) {
            return removeBid(fxOrder);
        } else if (fxOrder.getSide() == SELL ) {
            return removeAsk(fxOrder);
        }
        logger.error("Un-identified side to find order to be removed order {}" , ()-> fxOrder);
        return false;
    }

    @Override
    public Collection<FXOrder> getOrderHistory() {
        return this.orderHistory.values();
    }

    @Override
    public FXOrder getOrder(Long orderId) {
        return this.orderHistory.get(orderId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FXOrderBook that = (FXOrderBook) o;

        return fxSymbol != null ? fxSymbol.equals(that.fxSymbol) : that.fxSymbol == null;
    }

    @Override
    public int hashCode() {
        return fxSymbol != null ? fxSymbol.hashCode() : 0;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("\n=================== ORDER BOOK ===================\nFXOrderBook{");
        sb.append("\nfxSymbol=").append(fxSymbol);
        sb.append("-hashCode=").append(fxSymbol.hashCode()).append('-');
        sb.append(",\n\n fxBidOrderSortedMap=").append(fxBidOrderSortedMap);
        sb.append(",\n\n fxAskOrderSortedMap=").append(fxAskOrderSortedMap);
        sb.append("}\n=================== END of ORDER BOOK ===================");
        return sb.toString();
    }


}
