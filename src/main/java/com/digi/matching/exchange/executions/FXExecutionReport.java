package com.digi.matching.exchange.executions;

import com.digi.matching.types.ExecType;
import com.digi.matching.types.OrderType;

public class FXExecutionReport implements ExectionReport {
    private final String execId ;
    private final String orderId ;
    private final ExecType execType;
    private final OrderType orderType;

    private final double execQty;
    private final double execPrice;

    public static class ERBuilder {
        private  String execId ;
        private  String orderId ;
        private  ExecType execType;
        private  OrderType orderType;

        private  double execQty;
        private  double execPrice;

    }

    public FXExecutionReport(String execId, String orderId, ExecType execType, OrderType ordTyp, double execQty, double execPrice){

          this.execId = execId;
          this.orderId = orderId;
          this.execType = execType;
          this.orderType = ordTyp;

          this.execQty = execQty;
          this.execPrice = execPrice;

    }

    public String getExecId() {
        return execId;
    }

    public String getOrderId() {
        return orderId;
    }

    public ExecType getExecType() {
        return execType;
    }

    public OrderType getOrderType() {
        return orderType;
    }

    public double getExecQty() {
        return execQty;
    }

    public double getExecPrice() {
        return execPrice;
    }
}
