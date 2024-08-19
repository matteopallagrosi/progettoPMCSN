package it.metro.centers;

import it.metro.utils.Rvgs;

//rappresenta il centro dei controllori del biglietto
public class TicketInspectorsCenter extends MslsCenter {

    public TicketInspectorsCenter(int numServer, Rvgs v) {
        super(4, numServer, v, "Ticket Inspectors Center");
    }

    @Override
    public int getNextCenter() {
        //probabilità con cui nextCenter è Elevator oppure Platform
        v.rngs.selectStream(60);
        double random = v.rngs.random();
        if (random <= 0.1) {
            return 5;                        //pE --> utente che si dirige verso gli ascensori
        }
        else {
            return 6;                        //1-pE --> utente che si dirige verso la banchina del treno
        }
    }

    @Override
    public double getService() {
        v.rngs.selectStream(50);
        return v.exponential(0.5);
    }
}
