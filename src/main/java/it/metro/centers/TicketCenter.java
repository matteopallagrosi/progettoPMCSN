package it.metro.centers;

import it.metro.utils.Rvgs;

//rappresenta il centro delle casse fisiche (ossia con persona a servire)
public class TicketCenter extends MssqCenter {

    public TicketCenter(int numServer, Rvgs v) {
        super(2, numServer, v, "Ticket Center");
    }

    @Override
    public int getNextCenter() {
        return 3;
    }

    @Override
    public double getService() {
        v.rngs.selectStream(100);
        //return (v.uniform(2.0, 10.0));
        //return v.exponential(3.33);
        double alfa = rvms.cdfExponential(40, 5);
        double u = v.uniform(alfa, 1);
        return rvms.idfExponential(40, u);
    }
}
