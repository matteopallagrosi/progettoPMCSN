package it.metro.centers;

//rappresenta il centro della banchina della metro
public class SubwayPlatformCenter extends Center {

    public SubwayPlatformCenter(int numServer) {
        super(6, numServer);
    }

    @Override
    public Center getNextCenter() {
        return null;
    }

    @Override
    public void processArrival() {

    }

    @Override
    public void processDeparture() {

    }
}
