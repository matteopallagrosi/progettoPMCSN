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
    static double STOP    = 2000000.0;                       //"close the door" --> il flusso di arrivo viene interrotto
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
    private Center[] centers;
    private int arrivalsTurtsiles = 0;
    private int arrivalsElectronic = 0;
    private int arrivalsTicket = 0;
    private static double trainArrival = 0;

    //Run simulation
    public static void main(String[] args) {
        Simulation simulation = new Simulation();
        simulation.run();
    }

    //Inizializza la configurazione dei vari centri
    private void initCenters() {
        centers = new Center[6];
        electronicTicketCenter = new ElectronicTicketCenter(4, v);
        centers[0] = electronicTicketCenter;
        ticketCenter = new TicketCenter(4, v);
        centers[1] = ticketCenter;
        turnstilesCenter = new TurnstilesCenter(4, v);
        centers[2] = turnstilesCenter;
        ticketInspectorsCenter = new TicketInspectorsCenter(4, v);
        centers[3] = ticketInspectorsCenter;
        elevatorsCenter = new ElevatorsCenter(4, v);
        centers[4] = elevatorsCenter;
        subwayPlatformCenter = new SubwayPlatformCenter(1, v);
        centers[5] = subwayPlatformCenter;
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
            arrivalsTurtsiles++;
            return turnstilesCenter;                //pA --> utente abbonato che si dirige ai tornelli
        }
        else if (random > 0.33 && random <= 0.83) {
            arrivalsElectronic++;
            return electronicTicketCenter;          //pB --> utente si dirige verso biglietteria automatica
        }
        else {
            arrivalsTicket++;
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
        arrival.setExternal(true);
        return arrival;
    }

    //prende in input centro e server per cui produrre l'evento di completamento
    private Event generateDepartureEvent(Center center, Server server) {
        Event departure = new Event(EventType.DEPARTURE, t.current + center.lastService);
        departure.setCenter(center);
        departure.setServer(server);
        return departure;
    }

    private void generateArrivalNextCenter(Event currentEvent) {
        Center currentCenter = currentEvent.getCenter();
        int nextCenterID = currentCenter.getNextCenter();

        //se nextCenterID == 0 --> non c'è un centro successivo al centro corrente (il job ha attraversato l'intero sistema)
        //altrimenti produce un evento di arrivo al centro successivo con clock pari a quello del completamento appena avvenuto
        //questo rappresenta il transito di un job da un centro al successivo
        if (nextCenterID != 0) {
            Center nextCenter = centers[nextCenterID-1];
            Event arrivalNextCenter = new Event(EventType.ARRIVAL, currentEvent.getTime());
            arrivalNextCenter.setCenter(nextCenter);
            arrivalNextCenter.setExternal(false);
            events.add(arrivalNextCenter);
        }
    }

    private void generateTrainArrivalEvent() {
        r.selectStream(80);
        trainArrival += v.exponential(10);
        Event tArrival = new Event(EventType.TRAINARRIVAL, trainArrival);
        tArrival.setCenter(subwayPlatformCenter);
        //se il flusso di arrivi è bloccato e non ci sono più passeggeri nella banchina, si interrompe il passaggio dei treni
        if (closeTheDoor && subwayPlatformCenter.numJobs == 0) {
            return;
        }
        else {
        events.add(tArrival);
        }
    }

    private void run() {
        this.initGenerators();
        this.initCenters();
        this.initEvents();

        //inizializza il clock di simulazione
        t = new Time(0, 0);

        //produce il primo evento, che è necessariamente un arrivo
        events.add(generateArrivalEvent());

        //produce il primo arrivo del treno
        generateTrainArrivalEvent();

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

                //produce l'arrivo successivo dall'esterno solo quando viene processato un arrivo "nuovo" (ossia dall'esterno, non conseguente a un completamento)
                if (event.isExternal()) {
                    Event newArrival = generateArrivalEvent();
                    if (newArrival.getTime() > STOP) {
                        t.last = t.current;
                        closeTheDoor = true;
                    } else {
                        events.add(newArrival);
                    }
                }

                //se il job è stato mandato in servizio produce l'evento di completamento
                if (serverDeparture != -1) {
                    events.add(generateDepartureEvent(currentCenter, currentCenter.servers[serverDeparture]));
                }
                //se il job non è stato mandato in servizio, e il centro corrente è quello dei controllori, il job deve raggiungere direttamente il centro successivo
                else if (currentCenter == ticketInspectorsCenter) {
                    generateArrivalNextCenter(event);
                }
            }
            //processa l'evento di completamento
            else if (event.getType() == EventType.DEPARTURE) {
                int generateDeparture = currentCenter.processDeparture();
                //se il completamento corrente ha portato in servizio il job successivo, produce un nuovo evento di completamento
                if (generateDeparture != -1) {
                    events.add(generateDepartureEvent(currentCenter, event.getServer()));
                }

                //genera l'evento di arrivo presso il centro successivo conseguente al completamento presso il centro corrente
                generateArrivalNextCenter(event);
            }
            //processa l'arrivo di un treno
            else if (event.getType() == EventType.TRAINARRIVAL) {
                //All'arrivo di un treno si verifica la partenza dei passeggeri dalla banchina del treno, i quali lasciano quindi il sistema
                subwayPlatformCenter.processDeparture();

                //genero il prossimo evento di arrivo del treno
                generateTrainArrivalEvent();
            }
        }
        System.out.println("turstiles: " + arrivalsTurtsiles);
        System.out.println("electronic " + arrivalsElectronic);
        System.out.println("ticket: " + arrivalsTicket);
        //stampa le statistiche di ogni centro
        printCentersStatistics();

        //stampa le statistiche dell'intero sistema
        printSystemStatistics();

    }



    //stampa le statistiche di ogni centro
    private void printCentersStatistics() {
        System.out.println("Electronic Ticket Center:");
        electronicTicketCenter.printStatistics();
        System.out.println("Ticket Center:");
        ticketCenter.printStatistics();
        System.out.println("TurnstilesCenter");
        turnstilesCenter.printStatistics();
        System.out.println("TicketInspectorsCenter");
        ticketInspectorsCenter.printStatistics();
        System.out.println("ElevatorsCenter");
        elevatorsCenter.printStatistics();
        System.out.println("SubwayPlatformCenter");
        subwayPlatformCenter.printStatistics();
    }

    //calcola il tempo medio di risposta per un job che attraversa l'intero sistema e raggiunge la banchina
    //viene però escluso il tempo di attesa sulla banchina (che non è un vero centro, ma serve per limitare la popolazione sulla banchina rendendo il sistema il più verosimile possibile)
    private void printSystemStatistics() {
        double areaSystem = 0;              //tiene traccia dell'area sottesa al gradico di l(t) per l'intero sistema (somma di quella dei singoli centri)
        for (Center center : centers) {
            if ((center instanceof MssqCenter) || (center instanceof MslsCenter)) {
                areaSystem += center.area[0].node;
            }
            else if (center instanceof MsmqCenter) {
                for (int i= 0; i < center.numServer; i++) {
                    areaSystem += center.area[i].node;
                }
            }
            //in questo caso ricade solo SubwayPlatformCenter
            else {
                areaSystem += 0;
            }
        }

        DecimalFormat f = new DecimalFormat("###0.000");
        System.out.println("\nfor " + subwayPlatformCenter.completedJobs + " jobs the service node statistics are:\n");
        //il tempo di risposta è il tempo per raggiungere la banchina (non viene quindi considerato il tempo di attesa dell'arrivo del treno)
        System.out.println("  avg wait ........... =   " + f.format(areaSystem / subwayPlatformCenter.completedJobs));
        //popolazione media nell'intero sistema
        System.out.println("  avg # in node ...... =   " + f.format(areaSystem / elevatorsCenter.lastDeparture));
    }

}
