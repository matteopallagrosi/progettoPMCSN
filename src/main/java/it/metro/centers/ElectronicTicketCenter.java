package it.metro.centers;

import it.metro.utils.Rvgs;

//rappresenta il centro delle casse automatiche
public class ElectronicTicketCenter extends MssqCenter {

    public ElectronicTicketCenter(int numServer, Rvgs v) {
        super(1, numServer, v, "Electronic Ticket Center");
    }

    @Override
    public int getNextCenter() {
        return 3;
    }

    @Override
    public double getService() {
        v.rngs.selectStream(1);
        //return (v.uniform(2.0, 10.0));
        return v.exponential(0.5);
    }
}
