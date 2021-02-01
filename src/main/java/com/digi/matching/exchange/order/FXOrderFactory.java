package com.digi.matching.exchange.order;

import com.digi.matching.exception.OrderCreationException;

public class FXOrderFactory implements OrderFactory {
    private FXOrderFactory() {}

    public static FXOrderFactory instance = new FXOrderFactory();

    public static FXOrderFactory getInstance() {
        if ( null == instance ) {
            synchronized (FXOrderFactory.class) {
                if ( null == instance ) {
                    instance = new FXOrderFactory();
                }
            }
        }
        return instance;
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        throw new CloneNotSupportedException("FXOrderFactory is not cloneable");
    }

    public FXOrder createOrder(FXOrder.FXOrderBuilder fxOrderBuilder) throws OrderCreationException {
        return fxOrderBuilder.build();
    }

}
