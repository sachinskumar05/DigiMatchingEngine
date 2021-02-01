package com.digi.matching.types;

public enum OrderType {//FixTag#40

    MARKET(1), LIMIT(2);
    private int fixValue = 1;
    OrderType(int ordTyp) {
        fixValue = ordTyp;
    }

    public int getFixValue(){return fixValue;}

}
