package it.metro.controller;

import it.metro.centers.*;
import it.metro.events.Event;
import it.metro.events.EventType;
import it.metro.utils.Rngs;
import it.metro.utils.Rvgs;
import it.metro.utils.Server;
import it.metro.utils.Time;

import java.text.DecimalFormat;
import java.util.*;

public class Simulation {

    private Queue<Event> events;                            //lista che tiene traccia degli eventi generati durante la simulazione
    private static double arrival = 0;
    static double STOP    = 200000.0;                       //"close the door" --> il flusso di arrivo viene interrotto
    public boolean closeTheDoor = false;
    private Time t;                                         //clock di simulazione
    private Rngs r;                                         //generatore di valori randomici Uniform(0,1)
    private Rvgs v;                                         //generatore di variate aleatorie
    private ElectronicTicketCenter electronicTicketCenter;
    private TicketCenter ticketCenter;
    private TurnstilesCenter turnstilesCenter;
    private TicketInspectorsCenter ticketInspectorsCenter;
    private ElevatorsCenter elevatorsCenter;
    private SubwayPlatformCenter subwayPlatformCenter;

    //Run simulation
    public static void main(String[] args) {
        Simulation simulation = new Simulation();
        simulation.run();
    }

    //Inizializza la configurazione dei vari centri
    private void initCenters() {
        electronicTicketCenter = new ElectronicTicketCenter(4, v);
        ticketCenter = new TicketCenter(4, v);
        turnstilesCenter = new TurnstilesCenter(4, v);
        ticketInspectorsCenter = new TicketInspectorsCenter(2, v);
        elevatorsCenter = new ElevatorsCenter(2, v);
        subwayPlatformCenter = new SubwayPlatformCenter(1, v);
    }

    private void initGenerators() {
        r = new Rngs();                  //istanzia la libreria per la generazione dei valori randomici
        r.plantSeeds(123456789);      //inizializza seed da cui produce i seed dei diversi stream
        v = new Rvgs(r);                 //istanzia la libreria per la generazione delle variate aleatorie
    }

    //inizializza la lista degli eventi, mantenuta ordinata secondo il clock degli eventi
    private void initEvents() {
        events = new PriorityQueue<>(new Comparator<Event>() {
            @Override
            public int compare(Event e1, Event e2) {
                return Double.compare(e1.getTime(), e2.getTime());
            }
        });
    }

    //genera il centro a cui l'utente è diretto all'arrivo (tornelli se abbonato, oppure alle casse automatiche oppure alle casse fisiche)
    private Center getEventUser() {
        r.selectStream(2);
        double random = r.random();
        if (random <= 0.33) {
            return turnstilesCenter;                //pA --> utente abbonato che si dirige ai tornelli
        }
        else if (random > 0.33 && random <= 0.83) {
            return electronicTicketCenter;          //pB --> utente si dirige verso biglietteria automatica
        }
        else {
            return ticketCenter;                    //pF --> utente si dirige verso biglietteria fisica
        }
    }

    //genera il prossimo istante di arrivo (arrivi random = tempo di interarr. esp.)
    private double getArrival() {
        r.selectStream(0);
        arrival += v.exponential(0.166);
        return (arrival);
    }

    private Event generateArrivalEvent() {
        Event arrival = new Event(EventType.ARRIVAL, getArrival());

        //l'arrivo può essere di utente abbonato, o diretto verso casse automatiche o fisiche
        arrival.setCenter(getEventUser());
        return arrival;
    }

    //prende in input centro e server per cui produrre l'evento di completamento
    private Event generateDepartureEvent(Center center, Server server) {
        Event departure = new Event(EventType.DEPARTURE, t.current + center.lastService);
        departure.setCenter(center);
        departure.setServer(server);
        return departure;
    }

    private void run() {
        this.initGenerators();
        this.initCenters();
        this.initEvents();

        //inizializza il clock di simulazione
        t = new Time(0, 0);

        //produce il primo evento, che è necessariamente un arrivo
        events.add(generateArrivalEvent());
        int i = 0;

        //procede a processare gli eventi, finché non si supera il "close the door" e la lista degli eventi non viene svuotata
        while (!closeTheDoor || !events.isEmpty()) {
            //estraggo il prossimo evento (in ordine di clock di simulazione)
            Event event = events.poll();

            //recupera il centro a cui è diretto l'evento
            Center currentCenter = event.getCenter();

            //aggiorna le statistiche del centro interessato dall'evento (e aggiorna l'evento corrente)
            currentCenter.updateStatistics(event);

            //aggiorna il clock di simulazione
            t.current = event.getTime();

            //processa l'evento di arrivo
            if (event.getType() == EventType.ARRIVAL) {
                int serverDeparture = currentCenter.processArrival();

                //produce l'arrivo successivo
                Event newArrival = generateArrivalEvent();
                if (newArrival.getTime() > STOP) {
                    t.last = t.current;
                    closeTheDoor = true;
                }
                else {
                    events.add(newArrival);
                }

                //se il job è stato mandato in servizio produce l'evento di completamento
                if (serverDeparture != -1) {
                    events.add(generateDepartureEvent(currentCenter, currentCenter.servers[serverDeparture]));
                }
            }
            //processa l'evento di completamento
            else if (event.getType() == EventType.DEPARTURE) {
                int generateDeparture = currentCenter.processDeparture();
                if (generateDeparture != -1) {
                    events.add(generateDepartureEvent(currentCenter, event.getServer()));
                }
            }
            i++;
        }

        //stampa le statistiche
        printCentersStatistics();

    }

    //stampa le statistiche di ogni centro
    private void printCentersStatistics() {
        System.out.println("Electronic Ticket Center:");
        electronicTicketCenter.printStatistics();
        System.out.println("Ticket Center:");
        ticketCenter.printStatistics();
        System.out.println("TurnstilesCenter");
        turnstilesCenter.printStatistics();
    }

}
