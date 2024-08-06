package it.metro.centers;

import it.metro.utils.Rvgs;

//rappresenta il centro con gli ascensori per raggiungere la banchina
public class ElevatorsCenter extends Center {

    public ElevatorsCenter(int numServer, Rvgs r) {
        super(5, numServer, r);
    }

    @Override
    public Center getNextCenter() {
        return null;
    }

    @Override
    public double getService() {
        return 1.0;
    }

    @Override
    public void processArrival() {

    }

    @Override
    public void processDeparture() {

    }
}
