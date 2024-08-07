package it.metro.centers;

import it.metro.utils.Rvgs;

//rappresenta il centro dei tornelli
public class TurnstilesCenter extends Center {

    public TurnstilesCenter(int numServer, Rvgs r) {
        super(3, numServer, r);
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
