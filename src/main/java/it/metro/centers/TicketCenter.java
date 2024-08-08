package it.metro.centers;

import it.metro.utils.Rvgs;

//rappresenta il centro delle casse fisiche (ossia con persona a servire)
public class TicketCenter extends MssqCenter {

    public TicketCenter(int numServer, Rvgs v) {
        super(2, numServer, v);
    }

    @Override
    public Center getNextCenter() {
        return null;
    }

    @Override
    public double getService() {
        v.rngs.selectStream(3);
        return (v.uniform(5.0, 15.0));
    }
}
