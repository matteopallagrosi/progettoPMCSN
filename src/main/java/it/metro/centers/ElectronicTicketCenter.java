package it.metro.centers;

import it.metro.utils.Rvgs;

//rappresenta il centro delle casse automatiche
public class ElectronicTicketCenter extends Center {

    public ElectronicTicketCenter(int numServer, Rvgs r) {
        super(1, numServer, r);
    }

    @Override
    public Center getNextCenter() {
        return null;
    }

    @Override
    public double getService() {
        r.rngs.selectStream(1);
        return (r.uniform(2.0, 10.0));
    }
}
