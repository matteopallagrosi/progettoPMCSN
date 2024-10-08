package it.metro.centers;

import it.metro.events.Event;
import it.metro.utils.Area;
import it.metro.utils.Rvgs;
import it.metro.utils.Server;
import it.metro.utils.Time;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

//Multi Server multi queue Center (like frequency division multiplexing system)
//Per ogni server c'è una coda a lui dedicata (quindi numero code = numServer)
public abstract class MsmqCenter extends Center {

    public int[] queues;            //tiene traccia della popolazione in ogni coda del centro
    public double [] firstArrive;   ////tiene traccia del primo arrivo in ogni server
    public double[] lastArrive;     //tiene traccia dell'ultimo arrivo in ogni server
    public double[] lastDeparture;  //tiene traccia dell'ultimo completamento per ogni server
    public int numActiveServer;     //tiene traccia del numero di tornelli correntemente attivo
    public double totalService = 0;
    public double totalServed = 0;
    public double totalAreaNode = 0;
    public double totalAreaQueue = 0;

    public MsmqCenter(int id, int numServer, Rvgs v, String name) {
        super(id, numServer, v, name);
        queues = new int[numServer];
        this.area = new Area[numServer];
        for (int i = 0; i < numServer; i++) {
            this.area[i] = new Area();
        }
        this.firstArrive = new double[numServer];
        this.lastArrive = new double[numServer];
        this.lastDeparture = new double[numServer];
    }

    public int processArrival() {
        numJobs += 1;

        //caso con ripartizione uniforme del flusso tra serventi (solo per verifica)
        //modifico il centro in modo da verificare se avendo Poisson in ingresso ho Poisson anche in uscita
        /*v.rngs.selectStream(20);
        int chosenServer = (int)v.equilikely(0, numServer-1);
        if (servers.get(chosenServer).idle) {
            lastService = getService();
            servers.get(chosenServer).service += lastService;
            servers.get(chosenServer).served++;
            servers.get(chosenServer).idle = false;
            numBusyServers++;
            lastArrive[chosenServer] = currentEvent.getTime();
            return chosenServer;
        }*/

        //se è disponibile un server, il job viene immediatamente servito
        //ritorna l'indice del server per cui deve essere prodotto un tempo di completamento, altrimenti -1 se job va in coda
        if (isSomeServerIdle()) {
            int serverIndex = findIdleServer();
            Server selectedServer = servers.get(serverIndex);
            lastService = generateService(serverIndex);
            selectedServer.service += lastService;
            selectedServer.served++;
            selectedServer.idle = false;
            numBusyServers++;
            lastArrive[serverIndex] = currentEvent.getTime();
            //il controller produce un evento di completamento
            return serverIndex;
        }
        //altrimenti il job viene inserito in una delle code secondo la politica di accodamento creata
        int selectedQueue = selectQueue();
        queues[selectedQueue]++;
        if (firstArrive[selectedQueue] == 0) {
            firstArrive[selectedQueue] = currentEvent.getTime();
        }
        lastArrive[selectedQueue] = currentEvent.getTime();
        return -1;
    }

    @Override
    public int processDeparture() {
        lastDeparture[currentEvent.getServer().id] = currentEvent.getTime();
        completedJobs += 1;
        numJobs -= 1;
        //aggiorna l'ultimo completamento presso il server coinvolto (necessario per selezionare il server libero da più tempo)
        currentEvent.getServer().lastDeparture = currentEvent.getTime();
        //se è presente almeno un job nella coda del server in cui c'è stato il completamento (e non devo disattivare il server), questo viene prelevato e mandato in servizio in questo server
        if (queues[currentEvent.getServer().id] > 0 && numServerToRemove == 0) {
            queues[currentEvent.getServer().id]--;
            lastService = generateService(currentEvent.getServer().id);
            currentEvent.getServer().service += lastService;
            currentEvent.getServer().served++;
            //il controller produce un evento di completamento
            return 0;
        }
        //se necessario disattivo il server, e sposto i suoi job in coda nella coda di un server attivo
        else if (numServerToRemove != 0) {
            currentEvent.getServer().active = false;
            currentEvent.getServer().idle = true;
            numServerToRemove--;
            numActiveServer--;
            int i = 0;
            //trova un server attivo
            while (!servers.get(i).active) {
                i++;
            }
            queues[i] += queues[currentEvent.getServer().id];
            queues[currentEvent.getServer().id] = 0;
            numBusyServers--;

            /*if (numServerToRemove == 0) {
                System.out.println("centro: " + name);
                for (Server server: servers) {
                    System.out.println("server " + server.id + " attivo: " + server.active + " idle: " + server.idle);
                    System.out.println("job in coda: " + queues[server.id]);
                }
                System.out.println("server ancora da rimuovere: " + numServerToRemove);
                System.out.println("");
            }*/
        }
        //altrimenti il server torna a essere libero
        else {
            currentEvent.getServer().idle = true;
            numBusyServers--;
        }
        return -1;
    }

    @Override
    public void updateStatistics(Event newEvent) {
        for (Server server : servers) {
            int currentQueue = server.id;
            int jobsInService = (!server.idle) ? 1 : 0;
            if ((queues[currentQueue] + jobsInService) > 0) {
                double delta = (newEvent.getTime() - currentEvent.getTime());
                area[currentQueue].node += delta * (queues[currentQueue] + 1);
                area[currentQueue].queue += delta * (queues[currentQueue]);
                area[currentQueue].service += delta;
            }
        }

        //aggiorna l'evento corrente per il centro
        currentEvent = newEvent;
    }

    @Override
    public void printStatistics() {
        DecimalFormat f = new DecimalFormat("###0.000");
        DecimalFormat g = new DecimalFormat("###0.000");

        System.out.println("\nfor " + completedJobs + " jobs the service node statistics are:\n");

        for (int i = 0; i < numServer; i++) {
            System.out.println("Queue + Server " + i + ":");
            System.out.println("last arrive: " + lastArrive[i]);
            System.out.println("  avg interarrivals .. =   " + f.format(lastArrive[i] / servers.get(i).served));
            System.out.println("  avg wait ........... =   " + f.format(area[i].node / servers.get(i).served));
            System.out.println("serviti: " + servers.get(i).served);
            System.out.println("  avg # in node ...... =   " + f.format(area[i].node / lastDeparture[i]));
            System.out.println("  avg delay .......... =   " + f.format(area[i].queue / servers.get(i).served));
            System.out.println("  avg # in queue ..... =   " + f.format(area[i].queue / lastDeparture[i]));
        }

        System.out.println("\nthe server statistics are:\n");
        System.out.println("    server     utilization     avg service      share");
        for (int s = 0; s < numServer; s++) {
            System.out.print("       " + s + "          " + g.format(servers.get(s).service / lastDeparture[s]) + "            ");
            System.out.println(f.format(servers.get(s).service / servers.get(s).served) + "         " + g.format(servers.get(s).served / (double)completedJobs));
        }
        //share = percentuale di job processati da quel server sul totale

        System.out.println("");

        System.out.println("Statistiche globali del centro multicoda: ");
        aggregateStatistics();
        System.out.println("  avg interarrivals .. =   " + f.format(findMax(lastArrive) / totalServed));
        System.out.println("  avg wait ........... =   " + f.format(totalAreaNode / totalServed));
        System.out.println("serviti: " + totalServed);
        System.out.println("  avg # in node ...... =   " + f.format(totalAreaNode / findMax(lastDeparture)));
        System.out.println("  avg delay .......... =   " + f.format(totalAreaQueue / totalServed));
        System.out.println("  avg # in queue ..... =   " + f.format(totalAreaQueue / findMax(lastDeparture)));
        System.out.println("   utilization ..... =   " + f.format(((totalService / findMax(lastDeparture))/numServer)));
    }

    public static double findMax(double[] array) {
        if (array == null || array.length == 0) {
            throw new IllegalArgumentException("L'array non può essere nullo o vuoto.");
        }

        double max = array[0];  // Inizializza `max` con il primo elemento dell'array
        for (int i = 1; i < array.length; i++) {  // Scorri l'array partendo dal secondo elemento
            if (array[i] > max) {  // Confronta ogni elemento con `max`
                max = array[i];  // Aggiorna `max` se trovi un valore più grande
            }
        }

        return max;  // Ritorna il valore massimo trovato
    }

    public static double findMin(double[] array) {
        if (array == null || array.length == 0) {
            throw new IllegalArgumentException("L'array non può essere nullo o vuoto.");
        }

        double min = array[0];  // Inizializza `min` con il primo elemento dell'array
        for (int i = 1; i < array.length; i++) {  // Scorri l'array partendo dal secondo elemento
            if (array[i] < min) {  // Confronta ogni elemento con `min`
                min = array[i];  // Aggiorna `min` se trovi un valore più piccolo
            }
        }

        return min;  // Ritorna il valore minimo trovato
    }

    // Ritorna l'indice della coda (di un server attivo) con il numero minimo di utenti accodati.
    // Se diverse code presentano lo stesso numero minimo di utenti, sceglie uniformemente una tra di esse.
    private int selectQueue() {
        //sceglie randomicamente una delle code (per motivi di testing)
        /*v.rngs.selectStream(4);
        //ritorna casualmente una coda tra quella dei server attivi (da modificare)
        int selectedQueue =  (int)v.equilikely(0, numServer-1);
        while (!servers.get(selectedQueue).active) {
            selectedQueue = (int)v.equilikely(0, numServer-1);
        }
        return selectedQueue;*/
        int minValue = 0;
        for (int i = 0; i < queues.length; i++) {
            if (servers.get(i).active) {
                minValue = queues[i];
                break;
            }
        }
        List<Integer> minIndices = new ArrayList<>();

        // Trova il valore minimo e raccoglie gli indici delle sue posizioni nell'array
        for (int i = 0; i < queues.length; i++) {
            if (queues[i] < minValue && servers.get(i).active) {
                minValue = queues[i];
                minIndices.clear(); // Reset della lista degli indici
                minIndices.add(i); // Aggiungi il nuovo indice del minimo
            } else if (queues[i] == minValue && servers.get(i).active) {
                minIndices.add(i); // Aggiungi l'indice del minimo esistente
            }
        }

        // Seleziona uniformemente un indice tra quelli raccolti
        v.rngs.selectStream(4);
        int selectedIndex =  (int)v.equilikely(0, minIndices.size()-1);

        return minIndices.get(selectedIndex);
    }

    //verifica se è presente un server libero
    private boolean isSomeServerIdle() {
        for (int i = 0; i < numServer; i++) {
            if (servers.get(i).active && servers.get(i).idle) {
                return true;
            }
        }
        return false;
    }

    private double generateService(int serverIndex) {
        v.rngs.selectStream(5 + serverIndex);
        return getService();
    }

    //seleziona tra quelli liberi un server casuale con equa probabilità
    @Override
    protected int findIdleServer() {
        List<Server> serversIdle = new ArrayList<>();
        for (Server server : servers) {
            if (server.idle && server.active) {
                serversIdle.add(server);
            }
        }
        v.rngs.selectStream(15);
        int selectedServerIndex = (int)v.equilikely(0, serversIdle.size()-1);
        return serversIdle.get(selectedServerIndex).id;
    }

    //i metodi sottostanti ritornano le statistiche dell'i-esimo centro (coppia server-coda)
    public double getAvgInterarrival(int i) {
        return (lastArrive[i] - firstArrive[i]) / servers.get(i).served;
    }

    public double getAvgWait(int i) {
        return area[i].node / servers.get(i).served;
    }

    public double getAvgDelay(int i) {
        return area[i].queue / servers.get(i).served;
    }

    public double getAvgNode(int i) {
        return area[i].node / (lastDeparture[i] - firstArrive[i]);
    }

    public double getAvgQueue(int i) {
        return area[i].queue / (lastDeparture[i] - firstArrive[i]);
    }


    public double getUtilization(int i) {
        return servers.get(i).service / (lastDeparture[i] - firstArrive[i]);
    }

    public double getAvgService(int i) {
        return servers.get(i).service / servers.get(i).served;
    }

    //i metodi sottostanti ritornano le statistiche globali dell'intero centro
    //ossia aggrega le popolazioni (tempi di servizio e job serviti) che costituiscono le coppie coda-server del centro Msmq, e calcola le statistiche sempre utilizzando Little
    public void aggregateStatistics() {
        totalService = 0;
        totalServed = 0;
        totalAreaNode = 0;
        totalAreaQueue = 0;
        for (int i = 0; i < numServer; i++) {
            if (servers.get(i).active) {
                totalService += servers.get(i).service;
                totalServed += servers.get(i).served;
                totalAreaNode += area[i].node;
                totalAreaQueue += area[i].queue;
            }
        }
    }

    public double getTotalAvgInterarrival() {
        return (findMax(lastArrive) - findMin(firstArrive)) / totalServed;
    }

    public double getTotalAvgWait() {
        return totalAreaNode / totalServed;
    }

    public double getTotalAvgDelay() {
        return totalAreaQueue / totalServed;
    }

    public double getTotalAvgNode() {
        return totalAreaNode / (findMax(lastDeparture) - findMin(firstArrive));
    }

    public double getTotalAvgQueue() {
        return totalAreaQueue / (findMax(lastDeparture) -findMin(firstArrive));
    }

    //in realtà l'utilizzazione del centro multiserver coincide con l'utilizzazione del singolo servente
    //per completezza inseriamo anche questo metodo
    public double getTotalUtilization() {
        return (totalService / (findMax(lastDeparture) - findMin(firstArrive)))/numActiveServer;
    }
}
