package it.metro.centers;

import it.metro.utils.Rvgs;

//rappresenta il centro delle casse fisiche (ossia con persona a servire)
public class TicketCenter extends Center {

    public TicketCenter(int numServer, Rvgs r) {
        super(2, numServer, r);
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
