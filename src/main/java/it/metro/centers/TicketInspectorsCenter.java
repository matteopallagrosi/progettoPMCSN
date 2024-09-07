package it.metro.centers;

import it.metro.utils.Rvgs;

//rappresenta il centro dei controllori del biglietto
public class TicketInspectorsCenter extends MslsCenter {

    public boolean lastInspectionDone = true;

    public TicketInspectorsCenter(int numServer, Rvgs v) {
        super(4, numServer, v, "Ticket Inspectors Center");
    }

    @Override
    public int getNextCenter() {
        //probabilità con cui nextCenter è Elevator oppure Platform
        v.rngs.selectStream(60);
        double random = v.rngs.random();
        //utente potrebbe non aver passato i controlli
        if (lastInspectionDone && random <= 0.1) {
            return 0;                        //pO --> utente che esce dal sistema poiché non ha superato i controlli
        }
        //se supera i controlli
        else {
            v.rngs.selectStream(61);
            random = v.rngs.random();
            if (random <= 0.05) {
                return 5;                        //pE --> utente che si dirige verso gli ascensori
            } else {
                return 6;                        //1-pE --> utente che si dirige verso la banchina del treno
            }
        }
    }

    @Override
    public double getService() {
        v.rngs.selectStream(50);
        //return v.exponential(0.5);
        double alfa = rvms.cdfExponential(10, 2);
        double u = v.uniform(alfa, 1);
        return rvms.idfExponential(10, u);
    }
}
