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
    static double STOP    = 20000.0;                        //"close the door" --> il flusso di arrivo viene interrotto
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
        ticketCenter = new TicketCenter(2, v);
        turnstilesCenter = new TurnstilesCenter(2, v);
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
        else if (random > 0.33 && random < 0.66) {
            return electronicTicketCenter;          //pB --> utente si dirige verso biglietteria automatica
        }
        else {
            return ticketCenter;                    //pC --> utente si dirige verso biglietteria fisica
        }
    }

    //genera il prossimo istante di arrivo (arrivi random = tempo di interarr. esp.)
    private double getArrival() {
        r.selectStream(0);
        arrival += v.exponential(2.0);
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

        //procede a processare gli eventi, finché non si supera il "close the door" e la lista degli eventi non viene svuotata
        while (t.current < STOP || !events.isEmpty()) {
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
                    t.last = newArrival.getTime();
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
        }

        //stampa le statistiche
        printCentersStatistics();

    }

    //stampa le statistiche di ogni centro
    private void printCentersStatistics() {
        System.out.println("Electronic Ticket Center:\n");
        printStatistics(electronicTicketCenter);
    }

    //stampa le statistiche di un centro
    private  void printStatistics(Center center) {
        DecimalFormat f = new DecimalFormat("###0.00");
        DecimalFormat g = new DecimalFormat("###0.000");

        System.out.println("\nfor " + center.completedJobs + " jobs the service node statistics are:\n");
        System.out.println("  avg interarrivals .. =   " + f.format(t.last / center.completedJobs));
        System.out.println("  avg wait ........... =   " + f.format(center.area.node / center.completedJobs));
        System.out.println("  avg # in node ...... =   " + f.format(center.area.node / t.current));
        System.out.println("  avg delay .......... =   " + f.format(center.area.queue / center.completedJobs));
        System.out.println("  avg # in queue ..... =   " + f.format(center.area.queue / t.current));


        System.out.println("\nthe server statistics are:\n");
        System.out.println("    server     utilization     avg service      share");
        for (int s = 0; s < center.numServer; s++) {
            System.out.print("       " + s + "          " + g.format(center.servers[s].service / t.current) + "            ");
            System.out.println(f.format(center.servers[s].service / center.servers[s].served) + "         " + g.format(center.servers[s].served / (double)center.completedJobs));
        }
        //share = percentuale di job processati da quel server sul totale

        System.out.println("");
    }

}
