package it.metro.centers;

import it.metro.utils.Rvgs;

//rappresenta il centro dei tornelli (in realtà è un sottosistema costituito da numServer centri, con probabilità di routing che variano a runtime)
public class TurnstilesCenter extends MsmqCenter {

    public TurnstilesCenter(int numServer, Rvgs v) {
        super(3, numServer, v, "Turnstiles Center");
    }

    public int getNextCenter() {
        return 4;
    }

    @Override
    public double getService() {
        //return (v.uniform(2.0, 10.0));
        return v.exponential(0.5);
    }
}
