package it.metro.centers;

import it.metro.utils.Rvgs;

//rappresenta il centro con gli ascensori per raggiungere la banchina
public class ElevatorsCenter extends MssqCenter {

    public ElevatorsCenter(int numServer, Rvgs v) {
        super(5, numServer, v);
    }

    @Override
    public int getNextCenter() {
        return 6;
    }

    @Override
    public double getService() {
        v.rngs.selectStream(70);
        return v.exponential(2);
    }
}
