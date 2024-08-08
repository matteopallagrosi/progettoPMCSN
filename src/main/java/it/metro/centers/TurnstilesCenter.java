package it.metro.centers;

import it.metro.utils.Rvgs;

//rappresenta il centro dei tornelli
public class TurnstilesCenter extends MsmqCenter {

    public TurnstilesCenter(int numServer, Rvgs v) {
        super(3, numServer, v);
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
