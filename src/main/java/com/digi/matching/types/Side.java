package com.digi.matching.types;

public enum Side {
    BUY(1), SELL(2);
    private int fixValue = 0;
    Side(int fixSide) {
        this.fixValue = fixSide;
    }
    public int getFixValue(){ return fixValue;}
}
