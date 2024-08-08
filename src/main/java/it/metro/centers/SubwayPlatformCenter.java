package it.metro.centers;

import it.metro.utils.Rvgs;

//rappresenta il centro della banchina della metro
public class SubwayPlatformCenter extends MssqCenter {

    public SubwayPlatformCenter(int numServer, Rvgs v) {
        super(6, numServer, v);
    }

    @Override
    public Center getNextCenter() {
        return null;
    }

    @Override
    public double getService() {
        return 1.0;
    }
}
