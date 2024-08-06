package it.metro.controller;

import it.metro.centers.*;
import it.metro.events.Event;
import it.metro.events.EventType;
import it.metro.utils.Rngs;
import it.metro.utils.Rvgs;
import it.metro.utils.Time;

import java.util.ArrayList;
import java.util.List;

public class Simulation {

    private List<Event> events = new ArrayList<>();     //lista che tiene traccia degli eventi generati durante la simulazione
    private static double arrival = 0;
    private Rngs r;                                     //generatore di valori randomici Uniform(0,1)
    private Rvgs v;                                     //generatore di variate aleatorie

    //Run simulation
    public static void main(String[] args) {
        Simulation simulation = new Simulation();
        simulation.run();
    }

    //Inizializza la configurazione dei vari centri
    private void initCenters() {
        ElectronicTicketCenter electronicTicketCenter = new ElectronicTicketCenter(2);
        TicketCenter ticketCenter = new TicketCenter(2);
        TurnstilesCenter turnstilesCenter = new TurnstilesCenter(2);
        TicketInspectorsCenter ticketInspectorsCenter = new TicketInspectorsCenter(2);
        ElevatorsCenter elevatorsCenter = new ElevatorsCenter(2);
        SubwayPlatformCenter subwayPlatformCenter = new SubwayPlatformCenter(1);
    }

    private void initGenerators() {
        r = new Rngs();                  //istanzia la libreria per la generazione dei valori randomici
        r.plantSeeds(123456789);      //inizializza seed da cui produce i seed dei diversi stream
        v = new Rvgs(r);                 //istanzia la libreria per la generazione delle variate aleatorie
    }

    //genera il prossimo istante di arrivo (arrivi random = tempo di interarr. esp.)
    double getArrival() {
        r.selectStream(0);
        arrival += v.exponential(2.0);
        return (arrival);
    }

    private void run() {
        this.initCenters();
        this.initGenerators();

        //inizializza il clock di simulazione
        Time t = new Time(0, 0);

        //produce il primo evento, che è necessariamente un arrivo
        Event firstArrival = new Event(EventType.ARRIVAL, getArrival());
        events.add(firstArrival);

    }
}
