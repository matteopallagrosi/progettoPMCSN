package it.metro.centers;

import it.metro.events.Event;
import it.metro.utils.Rvgs;
import it.metro.utils.Time;

//Multi Server Loss System
public class MslsCenter extends Center {

    public MslsCenter(int id, int numServer, Rvgs v) {
        super(id, numServer, v);
    }

    @Override
    public Center getNextCenter() {
        return null;
    }

    @Override
    public double getService() {
        return 0;
    }

    @Override
    public int processArrival() {
        return 0;
    }

    @Override
    public int processDeparture() {
        return 0;
    }

    @Override
    public void updateStatistics(Event newEvent) {

    }

    @Override
    public void printStatistics(Time t) {

    }
}
