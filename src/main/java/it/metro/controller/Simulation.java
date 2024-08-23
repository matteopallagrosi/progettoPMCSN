package it.metro.controller;

import it.metro.centers.*;
import it.metro.events.Event;
import it.metro.events.EventType;
import it.metro.utils.*;

import java.io.*;
import java.text.DecimalFormat;
import java.util.*;

public class Simulation {

    private Queue<Event> events;                            //lista che tiene traccia degli eventi generati durante la simulazione
    private static double arrival = 0;
    private double STOP    = 2000000.0;                       //"close the door" --> il flusso di arrivo viene interrotto
    public boolean closeTheDoor = false;
    private double arrivalRate = 0;
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
    private int maxNumTurnstiles = 4;
    private int arrivalsTurtsiles = 0;
    private int arrivalsElectronic = 0;
    private int arrivalsTicket = 0;
    private static double trainArrival = 0;

    //Run simulation
    public static void main(String[] args) {
        Simulation simulation = new Simulation();
        simulation.setArrivalRate(0.166);
        simulation.run();
    }

    //Inizializza la configurazione dei vari centri
    private void initCenters(int[] config) {
        centers = new Center[6];
        electronicTicketCenter = new ElectronicTicketCenter(config[0], v);
        centers[0] = electronicTicketCenter;
        ticketCenter = new TicketCenter(config[1], v);
        centers[1] = ticketCenter;
        //il centro dei tornelli viene creato con il massimo numero di tornelli possibile
        //in ogni fascia oraria sarà attivo un numero di tornelli (<=maxNumTurnstiles) pari alla configurazione desiderata
        turnstilesCenter = new TurnstilesCenter(maxNumTurnstiles, v);
        //di default i tornelli sono tutti attivi, disattivo quelli in eccesso secondo la configurazione della prima fascia oraria
        for (int i = 0; i < (maxNumTurnstiles - config[2]); i++) {
            turnstilesCenter.servers.get(i).active = false;
        }
        turnstilesCenter.numActiveServer = config[2];
        centers[2] = turnstilesCenter;
        ticketInspectorsCenter = new TicketInspectorsCenter(config[3], v);
        centers[3] = ticketInspectorsCenter;
        elevatorsCenter = new ElevatorsCenter(config[4], v);
        centers[4] = elevatorsCenter;
        subwayPlatformCenter = new SubwayPlatformCenter(1, v);
        centers[5] = subwayPlatformCenter;
    }

    private void initGenerators() {
        r = new Rngs();                  //istanzia la libreria per la generazione dei valori randomici
        r.plantSeeds(123456789);      //inizializza seed da cui produce i seed dei diversi stream
        v = new Rvgs(r);                 //istanzia la libreria per la generazione delle variate aleatorie
    }

    public void initSeed(Rngs r, Rvgs v) {
        this.r = r;
        this.v = v;
    }

    public void setStop(double stopTime) {
        this.STOP = stopTime;
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

    //setta l'arrival rate da utilizzare nella simulazione
    public void setArrivalRate(double rate) {
        this.arrivalRate = rate;
    }

    //Cambia la configurazione corrente dei centri
    public void changeConfigurationCenters(int[] newConfig) {
        for (Center center : centers) {
            if (center instanceof MssqCenter) {
                //nel caso in cui aumenta il numero di server rispetto alla configurazione precedente
                if (newConfig[center.ID-1] > center.numServer) {
                    int oldNumServer = center.numServer;
                    center.numServer = newConfig[center.ID - 1];
                    //aggiungo alla lista dei server del centro i nuovi server (per default sono idle alla creazione)
                    for (int i = 0; i < (center.numServer - oldNumServer); i++) {
                        Server newServer = new Server(oldNumServer + i);
                        center.servers.add(newServer);
                    }
                    //se sono presenti job in coda, questi occuperanno i nuovi server
                    if (center.numJobs > oldNumServer) {
                        //per ogni job in coda (center.numJobs - oldNumServer = num. job in coda)
                        for (int i = 0; i < (center.numJobs - oldNumServer); i++) {
                            //il job è mandato in servizio se presente ancora un server libero
                            if (center.numBusyServers < center.numServer) {
                                Server idleServer = center.servers.get(oldNumServer + i);
                                assert idleServer.idle;
                                //il server selezionato non è più idle
                                idleServer.idle = false;
                                //aumenta il numero di server occupati
                                center.numBusyServers++;
                                //produce un tempo di servizio per il job
                                center.lastService = center.getService();
                                idleServer.service += center.lastService;
                                idleServer.served++;
                                //produce l'evento di completamento presso quel server
                                events.add(generateDepartureEvent(center, idleServer));
                            }
                            //altrimenti non ci sono più server liberi e i job rimanenti restano in coda
                            else {
                                break;
                            }
                        }
                    }
                }
                //se diminuisce il numero di server
                else if (newConfig[center.ID-1] < center.numServer) {
                    //se sono presenti almeno (center.numServer - newConfig[center.ID-1]) server liberi
                    if ((center.numServer - center.numBusyServers) >= (center.numServer - newConfig[center.ID-1])) {
                        int numServerToRemove = center.numServer - newConfig[center.ID-1];
                        //elimino i server liberi così da raggiungere la configurazione desiderata
                        int deletedServer = 0;
                        List<Server> serversToRemove = new ArrayList<>();
                        for (Server server : center.servers) {
                            //se il server è libero posso eliminarlo
                            if (server.idle) {
                                serversToRemove.add(server);
                                deletedServer++;
                                if (deletedServer == numServerToRemove) {
                                    break;
                                }
                            }
                        }
                        center.servers.removeAll(serversToRemove);
                        center.numServer = newConfig[center.ID-1];
                    }
                    //devo eliminare dei server occupati, ma solo dopo il completamento del job al loro interno
                    else {
                        int deletedServer = 0;
                        int numServerToRemove = center.numServer - newConfig[center.ID-1];
                        List<Server> serversToRemove = new ArrayList<>();
                        //tutti i server liberi vengono eliminati
                        for (Server server : center.servers) {
                            if (server.idle) {
                                serversToRemove.add(server);
                                deletedServer++;
                                center.numServer--;
                            }
                        }
                        center.servers.removeAll(serversToRemove);
                        //resta questo numero di server da rimuovere, necessariamente tra quelli occupati
                        numServerToRemove -= deletedServer;
                        //Nel momento in cui avvengono completamenti presso questo centro e i server si liberano, questi verranno rimossi
                        center.numServerToRemove = numServerToRemove;
                    }
                }
            }
            else if (center instanceof MslsCenter) {
                //nel caso in cui aumenta il numero di server rispetto alla configurazione precedente
                if (newConfig[center.ID-1] > center.numServer) {
                    int oldNumServer = center.numServer;
                    center.numServer = newConfig[center.ID - 1];
                    //aggiungo alla lista dei server del centro i nuovi server (per default sono idle alla creazione)
                    for (int i = 0; i < (center.numServer - oldNumServer); i++) {
                        Server newServer = new Server(oldNumServer + i);
                        center.servers.add(newServer);
                    }
                }
                //se invece diminuisce il numero di server rispetto alla configurazione precedente
                else if (newConfig[center.ID-1] < center.numServer) {
                    //se sono presenti almeno (center.numServer - newConfig[center.ID-1]) server liberi
                    if ((center.numServer - center.numBusyServers) >= (center.numServer - newConfig[center.ID-1])) {
                        int numServerToRemove = center.numServer - newConfig[center.ID-1];
                        //elimino i server liberi così da raggiungere la configurazione desiderata
                        int deletedServer = 0;
                        List<Server> serversToRemove = new ArrayList<>();
                        for (Server server : center.servers) {
                            //se il server è libero posso eliminarlo
                            if (server.idle) {
                                serversToRemove.add(server);
                                deletedServer++;
                                if (deletedServer == numServerToRemove) {
                                    break;
                                }
                            }

                        }
                        center.servers.removeAll(serversToRemove);
                        center.numServer = newConfig[center.ID-1];
                    }
                    //devo eliminare dei server occupati, ma solo dopo il completamento del job al loro interno
                    else {
                        int deletedServer = 0;
                        int numServerToRemove = center.numServer - newConfig[center.ID-1];
                        List<Server> serversToRemove = new ArrayList<>();
                        //tutti i server liberi vengono eliminati
                        for (Server server : center.servers) {
                            if (server.idle) {
                                serversToRemove.add(server);
                                deletedServer++;
                                center.numServer--;
                            }
                        }
                        center.servers.removeAll(serversToRemove);
                        //resta questo numero di server da rimuovere, necessariamente tra quelli occupati
                        numServerToRemove -= deletedServer;
                        //Nel momento in cui avvengono completamenti presso questo centro e i server si liberano, questi verranno rimossi
                        center.numServerToRemove = numServerToRemove;
                    }
                }
            }
            else if (center instanceof MsmqCenter) {
                //si presuppone che la nuova configurazione sia sempre <= maxNumTurstiles
                assert newConfig[center.ID-1] <= maxNumTurnstiles;
                //nel caso in cui aumenta il numero di server-coda attivi rispetto alla configurazione precedente
                if (newConfig[center.ID-1] > ((MsmqCenter) center).numActiveServer) {
                    int numServerToActive = newConfig[center.ID-1] - ((MsmqCenter) center).numActiveServer;
                    int i = 0;
                    while (numServerToActive != 0) {
                        if (!center.servers.get(i).active) {
                            center.servers.get(i).active = true;
                            numServerToActive--;
                        }
                        i++;
                    }
                    ((MsmqCenter) center).numActiveServer = newConfig[center.ID-1];
                }
                //se invece diminuisce il numero di server attivi
                else if (newConfig[center.ID-1] < ((MsmqCenter) center).numActiveServer) {
                    //se sono presenti server-code vuote a sufficienza, disattivo quei centri
                    int numServerToDeactivate = ((MsmqCenter) center).numActiveServer - newConfig[center.ID-1];
                    for (int i = 0; i < center.numServer; i++) {
                        //se trovo un server attualmente attivo, idle e con coda vuota lo disattivo immediatamente
                        if (center.servers.get(i).idle && center.servers.get(i).active && ((MsmqCenter) center).queues[i] == 0) {
                            center.servers.get(i).active = false;
                            numServerToDeactivate--;
                            ((MsmqCenter) center).numActiveServer--;
                        }
                        if (numServerToDeactivate == 0) {
                            break;
                        }
                    }
                    //se rimangono ancora server da disattivare (numServerToDeactivate != 0), devo disattivare i server attivi ma non vuoti una volta svuotati
                    center.numServerToRemove = numServerToDeactivate;
                }
            }
        }
    }

    //genera il prossimo istante di arrivo (arrivi random = tempo di interarr. esp.)
    private double getArrival() {
        r.selectStream(0);
        arrival += v.exponential(arrivalRate);
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
        if (closeTheDoor && subwayPlatformCenter.numJobs == 0 && events.isEmpty()) {
            return;
        }
        else {
        events.add(tArrival);
        }
    }

    //genera gli eventi di cambiamento di fascia oraria (che producono un cambiamento del tasso d'arrivo)
    //aggiunge questi eventi alla lista degli eventi
    private void generateSlotChange() {
        //il clock di simulazione parte da 0 secondi (corrispondente all'orario 5.30)
        //2^a fascia oraria (7.30-10.30) ; 7.30 = 7200 s (trascorsi dalle 5.30)
        events.add(new Event(EventType.SLOTCHANGE, 7200));
        //3^a fascia oraria (10.30-14-30) ; 10.30 = 18000 s (trascorsi dalle 5.30)
        events.add(new Event(EventType.SLOTCHANGE, 18000));
        //4^a fascia oraria (14.30-15-30) ; 14.30 = 32400 s (trascorsi dalle 5.30)
        events.add(new Event(EventType.SLOTCHANGE, 32400));
        //5^a fascia oraria (15.30-18-30) ; 15.30 = 36000 s (trascorsi dalle 5.30)
        events.add(new Event(EventType.SLOTCHANGE, 36000));
        //6^a fascia oraria (18.30-20-30) ; 18.30 = 46800 s (trascorsi dalle 5.30)
        events.add(new Event(EventType.SLOTCHANGE, 46800));
        //7^a fascia oraria (20.30-23-30) ; 20.30 = 54000 s (trascorsi dalle 5.30)
        events.add(new Event(EventType.SLOTCHANGE, 54000));
    }

    //Esegue una singola replica della simulazione
    private void run() {
        this.initGenerators();
        this.initCenters(new int[]{4,4,4,4,4});
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
                    events.add(generateDepartureEvent(currentCenter, currentCenter.servers.get(serverDeparture)));
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

    //Esegue la simulazione a orizzonte infinito del sistema (con il metodo dei batch means)
    public void runInfiniteHorizonSimulation(int numBatches, int batchSize) {
        int jobsProcessed = 0;      //jobs processati nel batch corrente
        int batchIndex = 0;                                                                      //tiene traccia del batch correntemente simulato
        Statistics[][] matrix = new Statistics[6][numBatches];      //matrice che tiene traccia delle statistiche medie per ogni centro (e anche overall del sistema) e per ogni batch
        this.initGenerators();
        this.initCenters(new int[]{4,4,4,4,4});
        this.initEvents();
        double firstArriveSystem = 0;
        double lastDepartureSystem = 0;

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
                //setta il primo arrivo all'intero sistema (nel batch corrente)
                if (firstArriveSystem == 0) {
                    firstArriveSystem = event.getTime();
                }

                //tiene traccia del numero di arrivi totale presso la banchina
                //questo è necessario per sapere quando completa un batch, in modo da passare al batch successivo
                //al completamento di un batch questa variabile viene azzerata
                if (currentCenter == subwayPlatformCenter) {
                    jobsProcessed++;
                    lastDepartureSystem = event.getTime();
                }
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
                    events.add(generateDepartureEvent(currentCenter, currentCenter.servers.get(serverDeparture)));
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

            //Se ho processato un numero di job pari alla batchSize devo calcolare le statistiche medie per quel batch
            if (jobsProcessed == batchSize) {
                //calcole le statistiche medie per ogni centro per quel batch
                generateStatistics(matrix, batchIndex, jobsProcessed, firstArriveSystem, lastDepartureSystem);
                //azzera le statistiche di ogni centro
                resetStatistics();
                firstArriveSystem = 0;
                //passa al batch successivo
                batchIndex++;
                jobsProcessed = 0;

                //La simulazione termina dopo numBatches
                if (batchIndex == numBatches) {
                    break;
                }
            }
        }

        //Calcolare gli intervalli di confidenza per le diverse statistiche dei vari centri
        generateEstimate(matrix, numBatches);
    }


    //esegue una singola replica della simulazione a orizzonte finito, che simula tutte le fasce orarie
    //sulle 18 ore di operatività della metropolitana
    public void runFiniteHorizonSimulation(int[][] configCenters, double[] slotRates) {
        //inizializza i centri con la configurazione della prima fascia oraria
        this.initCenters(configCenters[0]);
        this.initEvents();
        //indice che tiene traccia della fascia oraria corrente
        int slotIndex = 0;

        //inizializza il clock di simulazione
        t = new Time(0, 0);

        //produce gli eventi di cambiamento di fascia oraria
        generateSlotChange();

        //configura il tasso di arrivo della prima fascia oraria
        setArrivalRate(slotRates[slotIndex]);
        slotIndex++;

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
            //currentCenter.updateStatistics(event);

            if (event.getType() != EventType.SLOTCHANGE) {
                currentCenter.currentEvent = event;
            }

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
                    events.add(generateDepartureEvent(currentCenter, currentCenter.servers.get(serverDeparture)));
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
            //processa l'evento di cambiamento della fascia oraria
            else if (event.getType() == EventType.SLOTCHANGE) {
                //configura il tasso di arrivo della nuova fascia oraria
                setArrivalRate(slotRates[slotIndex]);

                //modifica i centri con la configurazione per la nuova fascia oraria
                changeConfigurationCenters(configCenters[slotIndex]);
                slotIndex++;

            }
        }
        System.out.println("turstiles: " + arrivalsTurtsiles);
        System.out.println("electronic " + arrivalsElectronic);
        System.out.println("ticket: " + arrivalsTicket);
        //stampa le statistiche di ogni centro
        //printCentersStatistics();

        //stampa le statistiche dell'intero sistema
        //printSystemStatistics();

    }


    private void generateEstimate(Statistics[][] matrix, int numBatches) {
        for (Center center: centers) {
            if ((center instanceof  MssqCenter) || (center instanceof  MslsCenter)) {
                double[] avgInterrarivals = new double[numBatches];
                double[] avgWait = new double[numBatches];
                double[] avgDelay = new double[numBatches];
                double[] avgNode = new double[numBatches];
                double[] avgQueue = new double[numBatches];
                double[] utilization = new double[numBatches];
                for (int i = 0; i < numBatches; i++) {
                    Statistics currentStatistics = matrix[center.ID - 1][i];
                    avgInterrarivals[i] = currentStatistics.avgInterarrivals[0];
                    avgWait[i] = currentStatistics.avgWait[0];
                    avgDelay[i] = currentStatistics.avgDelay[0];
                    avgNode[i] = currentStatistics.avgNode[0];
                    avgQueue[i] = currentStatistics.avgQueue[0];
                    //nei centri multiserver le utilizzazioni dei diversi server sono uguali per definizione, perciò possiamo considerare solo quelle del primo server
                    utilization[i] = currentStatistics.utilization[0];
                }
                //stampo gli intervalli di confidenza per le diverse statistiche di questo centro
                System.out.println("For " + center.name + ":");
                System.out.println("interrarivals:");
                Estimate.estimate(avgInterrarivals);
                System.out.println("wait:");
                Estimate.estimate(avgWait);
                System.out.println("delay:");
                Estimate.estimate(avgDelay);
                System.out.println("jobs in node:");
                Estimate.estimate(avgNode);
                System.out.println("jobs in queue:");
                Estimate.estimate(avgQueue);
                System.out.println("utilization:");
                Estimate.estimate(utilization);
                System.out.println("autocorrelation between batches:");
                //genera il file .dat da dare in input a acs per calcolo dell'autocorrelazione
                generateDatFile(avgWait, center.name);
                try {
                    Acs.autocorrelation(center.name + ".dat");
                } catch (IOException e) {
                    System.out.println("Autocorrelation failed");
                }
                //elimina il file .dat dopo aver calcolato l'autocorrelazione in quanto non più necessario
                File file = new File(center.name + ".dat");
                file.delete();
            }
            //le coppie server-code di questo centro sono sottocentri a se stanti, e le statistiche sono quindi separate
            else if (center instanceof  MsmqCenter) {
                //per ogni coppia server-coda
                for (int i = 0; i < center.numServer; i++) {
                    double[] avgInterrarivals = new double[numBatches];
                    double[] avgWait = new double[numBatches];
                    double[] avgDelay = new double[numBatches];
                    double[] avgNode = new double[numBatches];
                    double[] avgQueue = new double[numBatches];
                    double[] utilization = new double[numBatches];
                    for (int j = 0; j < numBatches; j++) {
                        Statistics currentStatistics = matrix[center.ID - 1][j];
                        avgInterrarivals[j] = currentStatistics.avgInterarrivals[i];
                        avgWait[j] = currentStatistics.avgWait[i];
                        avgDelay[j] = currentStatistics.avgDelay[i];
                        avgNode[j] = currentStatistics.avgNode[i];
                        avgQueue[j] = currentStatistics.avgQueue[i];
                        //nei centri multiserver le utilizzazioni dei diversi server sono uguali per definizione, perciò possiamo considerare solo quelle del primo server
                        utilization[j] = currentStatistics.utilization[i];
                    }
                    //stampo gli intervalli di confidenza per le diverse statistiche di questo centro
                    System.out.println("For " + center.name + ", server-queue " + i + ":");
                    System.out.println("interrarivals:");
                    Estimate.estimate(avgInterrarivals);
                    System.out.println("wait:");
                    Estimate.estimate(avgWait);
                    System.out.println("delay:");
                    Estimate.estimate(avgDelay);
                    System.out.println("jobs in node:");
                    Estimate.estimate(avgNode);
                    System.out.println("jobs in queue:");
                    Estimate.estimate(avgQueue);
                    System.out.println("utilization:");
                    Estimate.estimate(utilization);
                    System.out.println("autocorrelation between batches:");
                    //genera il file .dat da dare in input a acs per calcolo dell'autocorrelazione
                    generateDatFile(avgWait, center.name + i);
                    try {
                        Acs.autocorrelation(center.name + i + ".dat");
                    } catch (IOException e) {
                        System.out.println("Autocorrelation failed");
                    }
                    File file = new File(center.name + i + ".dat");
                    file.delete();
                }
            }
        }

        //genero l'intervallo di confidenza per le statistiche globali del sistema
        double[] avgWait = new double[numBatches];
        double[] avgNode = new double[numBatches];
        for (int i = 0; i < numBatches; i++) {
            Statistics currentStatistics = matrix[5][i];
            avgWait[i] = currentStatistics.avgWait[0];
            avgNode[i] = currentStatistics.avgNode[0];
        }
        System.out.println("For overall system:");
        System.out.println("wait:");
        Estimate.estimate(avgWait);
        System.out.println("jobs in node:");
        Estimate.estimate(avgNode);
        System.out.println("autocorrelation between batches:");
        generateDatFile(avgWait, "system");
        try {
            Acs.autocorrelation("system.dat");
        } catch (IOException e) {
            System.out.println("Autocorrelation failed");
        }
        File file = new File("system.dat");
        file.delete();
    }

    private static void generateDatFile(double[] sample, String centerName) {
        // Scrivere i valori double nel file
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(centerName + ".dat"))) {
            for (double x : sample) {
                // Scrive il double convertito in stringa e va a capo
                writer.write(Double.toString(x));
                writer.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void generateStatistics(Statistics[][] matrix, int batchIndex, int processedJobs, double firstArriveSystem, double lastDepartureSystem) {
        for (Center center : centers) {
            Statistics centerStat = new Statistics(center.numServer);
            //le statistiche sono settate in modo diverso a seconda del tipo di centro
            //MssqCenter utilizza solo lo slot 0 per le statistiche
            if ((center instanceof MssqCenter) || (center instanceof MslsCenter)) {
                centerStat.avgInterarrivals[0] = center.getAvgInterarrival(0);
                centerStat.avgWait[0] = center.getAvgWait(0);
                centerStat.avgDelay[0] = center.getAvgDelay(0);
                centerStat.avgNode[0] = center.getAvgNode(0);
                centerStat.avgQueue[0] = center.getAvgQueue(0);
                for (int i = 0; i < center.numServer; i++) {
                    centerStat.utilization[i] = center.getUtilization(i);
                }
            }
            //MsmqCenter ha uno slot per ogni coppia server-coda
            else if (center instanceof MsmqCenter) {
                for (int i = 0; i < center.numServer; i++) {
                    centerStat.avgInterarrivals[i] = center.getAvgInterarrival(i);
                    centerStat.avgWait[i] = center.getAvgWait(i);
                    centerStat.avgDelay[i] = center.getAvgDelay(i);
                    centerStat.avgNode[i] = center.getAvgNode(i);
                    centerStat.avgQueue[i] = center.getAvgQueue(i);
                    centerStat.utilization[i] = center.getUtilization(i);
                }
            }
            if (center != subwayPlatformCenter) {
                matrix[center.ID - 1][batchIndex] = centerStat;
            }
        }
        //l'ultima riga della matrice tiene traccia del tempo di risposta media e popolazione media dell'intero sistema in quel batch
        Statistics overallStatistics = new Statistics(1);
        double areaSystem = getAreaSystem();
        overallStatistics.avgWait[0] = areaSystem / processedJobs;
        overallStatistics.avgNode[0] = areaSystem / (lastDepartureSystem - firstArriveSystem);
        matrix[5][batchIndex] = overallStatistics;
    }

    //azzera le statistiche di ogni centro
    private void resetStatistics() {
        for (Center center : centers) {
            if (center instanceof MssqCenter) {
                center.completedJobs = 0;
                center.area[0].node = 0;
                center.area[0].queue = 0;
                ((MssqCenter)center).firstArrive = 0;
                for (int i = 0; i < center.numServer; i++) {
                    center.servers.get(i).service = 0;
                    center.servers.get(i).served = 0;
                }
            }
            else if (center instanceof MsmqCenter) {
                for (int i = 0; i < center.numServer; i++) {
                    center.completedJobs = 0;
                    center.servers.get(i).service = 0;
                    center.servers.get(i).served = 0;
                    center.area[i].node = 0;
                    center.area[i].queue = 0;
                    ((MsmqCenter)center).firstArrive[i] = 0;
                }
            }
            else if (center instanceof MslsCenter) {
                center.completedJobs = 0;
                ((MslsCenter) center).arrivalJobs = 0;
                ((MslsCenter) center).rejectedJob = 0;
                center.area[0].node = 0;
                ((MslsCenter)center).firstArrive = 0;
                for (int i = 0; i < center.numServer; i++) {
                    center.servers.get(i).service = 0;
                    center.servers.get(i).served = 0;
                }
            }
        }
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

    private void printSystemStatistics() {
        double areaSystem = getAreaSystem();

        DecimalFormat f = new DecimalFormat("###0.000");
        System.out.println("\nfor " + subwayPlatformCenter.completedJobs + " jobs the system statistics are:\n");
        //il tempo di risposta è il tempo per raggiungere la banchina (non viene quindi considerato il tempo di attesa dell'arrivo del treno)
        System.out.println("  avg wait ........... =   " + f.format(areaSystem / subwayPlatformCenter.completedJobs));
        //popolazione media nell'intero sistema
        System.out.println("  avg # in node ...... =   " + f.format(areaSystem / elevatorsCenter.lastDeparture));
    }

    //calcola il tempo medio di risposta per un job che attraversa l'intero sistema e raggiunge la banchina
    //viene però escluso il tempo di attesa sulla banchina (che non è un vero centro, ma serve per limitare la popolazione sulla banchina rendendo il sistema il più verosimile possibile)
    private double getAreaSystem() {
        double areaSystem = 0;              //tiene traccia dell'area sottesa al grafico di l(t) per l'intero sistema (somma di quella dei singoli centri)
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
        return areaSystem;
    }
}
