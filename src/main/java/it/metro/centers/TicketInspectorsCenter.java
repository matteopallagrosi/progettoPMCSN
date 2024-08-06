package it.metro.centers;

import it.metro.utils.Rvgs;

//rappresenta il centro dei controllori del biglietto
public class TicketInspectorsCenter extends Center {

    public TicketInspectorsCenter(int numServer, Rvgs r) {
        super(4, numServer, r);
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
