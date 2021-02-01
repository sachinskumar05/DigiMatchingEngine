package com.digi.matching.types;

public enum MsgType {//FixTag#35
    NOS("D"), EXEC_REPORT("8"), CANCEL_REQ("F"), CANCEL_REPLACE("G"), REJECT("3"), ORDER_CANCEL_REJECT("9");
    private String fixValue;
    MsgType(String msgType) {
        fixValue = msgType;
    }
    public String getFixValue() {
        return fixValue;
    }
}
