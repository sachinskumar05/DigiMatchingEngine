package com.digi.matching.symbols;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.digi.matching.util.StringUtil.BTC_USD;

/**
 * Immutable static data reference loaded at start up marked as tradable
 * This class will be also used for trade time locking (write lock) on its orderBook
 */
public class FXSymbol implements Symbol { /// when extended with more attributes, make it immutable
    private static final Logger logger = LogManager.getLogger(FXSymbol.class);
    private static final long serialVersionUID = 2405172041950251807L;

    private static final Map<String, FXSymbol> fxSymbolMap = new ConcurrentHashMap<>();

    static {
        //Only for the demo purpose, in industrial product.. Symbol class would be managed by static reference
        init();
    }

    private final String name;

    private double openingPx = 0.0d;

    /**
     * KISS => Keeping it Super Simple [for demo]
     * Only for the demo purpose, in industrial product. Symbol class would be managed by static reference
     */
    private static void init() {
        //Read from file / stream / messaging system or flat file and create the FXSymbol cache
        //for illustration I am hardcoding the array and keeping just one symbol here as asked in task BTC/USD
        Map<String, Double> symbolFile = new HashMap<>();
        // list is for illustration only, Will read it from data source
        symbolFile.put(BTC_USD, 2000.0d);
        for (Map.Entry<String, Double> syEntry : symbolFile.entrySet()) {
            logger.info(" Loading the FXSymbol in cache {} ", syEntry);
            fxSymbolMap.putIfAbsent(syEntry.getKey(), FXSymbol.valueOf(syEntry.getKey()).setOpeningPx(syEntry.getValue()) );
        }
    }

    private FXSymbol(String name) {
        this.name = name;
    }

    public static FXSymbol valueOf(String name) {
        FXSymbol fxSymbol = null;
        synchronized (fxSymbolMap) {//Making sure not a duplicate FXSymbol
            fxSymbol = fxSymbolMap.get(name);
            if (null == fxSymbol) {
                fxSymbol = new FXSymbol(name);
            }
        }
        return fxSymbol;
    }

    public double getOpeningPx() {
        return openingPx;
    }

    public FXSymbol setOpeningPx(double openingPx) {
        this.openingPx = openingPx;
        return this;
    }

    public static Collection<FXSymbol> getAllSymbols() {
        return fxSymbolMap.values();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FXSymbol fxSymbol = (FXSymbol) o;

        return name.equals(fxSymbol.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public String toString() {
        return "FXSymbol{" +
                "name='" + name + '\'' +
                '}';
    }

}
