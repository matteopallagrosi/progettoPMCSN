package it.metro.centers;

import it.metro.utils.Rvgs;

//rappresenta il centro con gli ascensori per raggiungere la banchina
public class ElevatorsCenter extends MssqCenter {

    public ElevatorsCenter(int numServer, Rvgs v) {
        super(5, numServer, v);
    }

    @Override
    public int getNextCenter() {
        return 0;
    }

    @Override
    public double getService() {
        return 1.0;
    }
}
