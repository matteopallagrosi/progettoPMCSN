package it.metro.centers;

import it.metro.utils.Rvgs;

//rappresenta il centro della banchina della metro
public class SubwayPlatformCenter extends Center {

    public SubwayPlatformCenter(int numServer, Rvgs r) {
        super(6, numServer, r);
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
