package it.metro.centers;

import it.metro.utils.Rvgs;

//rappresenta il centro con gli ascensori per raggiungere la banchina
public class ElevatorsCenter extends MssqCenter {

    public ElevatorsCenter(int numServer, Rvgs v) {
        super(5, numServer, v, "Elevators Center");
    }

    @Override
    public int getNextCenter() {
        return 6;
    }

    @Override
    public double getService() {
        v.rngs.selectStream(70);
        //return v.exponential(2);
        double alfa = rvms.cdfNormal(20,1,0);
        double u = v.uniform(alfa, 1);
        return rvms.idfNormal(20,1, u);
    }
}
