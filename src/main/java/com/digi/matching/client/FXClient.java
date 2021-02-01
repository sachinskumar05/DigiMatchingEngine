package com.digi.matching.client;

import com.digi.matching.engine.FXMatchingEngine;

public interface FXClient extends Client {
    FXMatchingEngine fxMatchingEngine = FXMatchingEngine.getInstance();

}
