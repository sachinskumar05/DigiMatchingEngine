package com.digi.matching.client;

import com.digi.matching.exception.OrderCreationException;
import com.digi.matching.exchange.order.FXOrder;
import com.digi.matching.exchange.order.Order;
import com.digi.matching.types.OrderType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import static com.digi.matching.types.Side.BUY;
import static com.digi.matching.util.StringUtil.*;

public class FXClientBuy implements FXClient {
    private static final Logger logger = LogManager.getLogger(FXClientBuy.class);

   private final List<FXOrder> fxOrderList = new ArrayList<>();
   private final StringBuilder clOrdIdBuilder = new StringBuilder();


    public void createAndSubmitOrder(int orderCount, double px, double qty, OrderType ot) {

        for (int i = 0; i < orderCount; i++) {
            clOrdIdBuilder.setLength(0);
            clOrdIdBuilder.append(clOrdId_Prefix_Buy).append(System.nanoTime());
            FXOrder.FXOrderBuilder fxoBuilder = new FXOrder.FXOrderBuilder(clOrdIdBuilder.toString(), BTC_USD, BUY, ot);

            try {
                fxOrderList.add(fxoBuilder.with(fxobj -> { fxobj.price = px; fxobj.qty = qty; fxobj.currency = USD; }) .build());
            } catch (OrderCreationException e) {
                e.printStackTrace();
                logger.error("Failed to build FXOrder using its builder {} \n{}", ()->fxoBuilder, ()->e);
            }
        }

        for(FXOrder fxOrder : fxOrderList) {
            try {
                submitOrder(fxOrder);
            } catch (Exception e) {
                e.printStackTrace();
                logger.error("Failed to submit SELL order {}", ()->fxOrder);
            }
        }

    }

    /**
     * Returns copy of orders to save the original copy
     * @return
     */
    @Override
    public List<FXOrder> getClientOrders() {
        final List<FXOrder> fxOrderListCopy = new ArrayList<>();
        for( FXOrder fxo: fxOrderList ) {
            fxOrderListCopy.add((FXOrder) fxo.clone());
        }
        return fxOrderListCopy;
    }

    @Override
    public void submitOrder(Order order) {
        FXOrder fxOrder = (FXOrder) order;
        try {
            logger.info("Sending client(B) order id {}, side {}, px {}, qty {}",
                    fxOrder::getClientOrderId, fxOrder::getSide,
                    fxOrder::getOrdPx, fxOrder::getOrdQty);
            fxMatchingEngine.addOrder(fxOrder);
        } catch(Exception ex) {
            logger.error("Failed to submit order for matching engine {}", ()->fxOrder, ()-> ex );
        }
    }

    @Override
    public void replaceOrder(Order order) {

    }

    @Override
    public void cancelOrder(Order order) {

    }
}
