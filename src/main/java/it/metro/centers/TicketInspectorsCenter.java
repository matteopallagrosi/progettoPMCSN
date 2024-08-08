package it.metro.centers;

import it.metro.utils.Rvgs;

//rappresenta il centro dei controllori del biglietto
public class TicketInspectorsCenter extends MslsCenter {

    public TicketInspectorsCenter(int numServer, Rvgs v) {
        super(4, numServer, v);
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
