package it.metro.centers;

import it.metro.events.Event;
import it.metro.utils.Area;
import it.metro.utils.Rvgs;
import it.metro.utils.Server;
import it.metro.utils.Time;

import java.text.DecimalFormat;

//Multi Server Single Queue Center
public abstract class MssqCenter extends Center {

    public double firstArrive;                     //primo arrivo presso questo centro (nel batch corrente in caso di simulazione batch means)
    public double lastArrive;                      //ultimo arrivo presso questo centro
    public double lastDeparture;                   //istante di completamento dell'ultimo job del centro

    public MssqCenter(int id, int numServer, Rvgs v, String name) {
        super(id, numServer, v, name);
        this.area = new Area[1];
        this.area[0] = new Area();
    }

    //processa l'arrivo di un job nel centro
    public int processArrival() {
        lastArrive = currentEvent.getTime();
        numJobs += 1;

        //setta il primo arrivo presso il centro corrente
        //necessario per calcolare il corrente tau di simulazione del batch corrente per il calcolo delle statistiche
        if (firstArrive == 0) {
            firstArrive = currentEvent.getTime();
        }

        //se è disponibile un server, il job viene immediatamente servito
        //ritorna l'indice del server per cui deve essere prodotto un tempo di completamento, altrimenti -1 se job va in coda
        if (numJobs <= numServer) {
            lastService = getService();
            int serverIndex = findIdleServer();
            Server selectedServer = servers.get(serverIndex);
            selectedServer.service += lastService;
            selectedServer.served++;
            selectedServer.idle = false;
            numBusyServers++;
            //il controller produce un evento di completamento
            return serverIndex;
        }
        return -1;  //nel caso in cui non deve essere prodotto un evento di completamento ritorna -1
    };

    //processa il completamento di un job nel centro
    //ritorna valore != -1 se è presente un ulteriore job in coda da processare
    public int processDeparture() {
        lastDeparture = currentEvent.getTime();
        completedJobs += 1;
        numJobs -= 1;
        //aggiorna l'ultimo completamento presso il server coinvolto (necessario per selezionare il server libero da più tempo)
        currentEvent.getServer().lastDeparture = currentEvent.getTime();
        //se è presente almeno un job in coda (e non devo rimuovere il server per il cambiamento di configurazione), questo viene prelevato e mandato in servizio nel server appena liberato
        if ((numJobs >= numServer) && (numServerToRemove == 0)) {
            lastService = getService();
            currentEvent.getServer().service += lastService;
            currentEvent.getServer().served++;
            //il controller produce un evento di completamento
            return 0;
        }
        //se ci sono ancora server da rimuovere per cambiare la configurazione del centro
        else if (numServerToRemove != 0) {
            //rimuovo il server che si è appena liberato
            servers.remove(currentEvent.getServer());
            numServer--;
            numBusyServers--;
            numServerToRemove--;

            /*if (numServerToRemove == 0) {
                System.out.println("centro " + name);
                System.out.println("jobs nel sistema: " + numJobs);
                System.out.println("jobs in coda: " + (numJobs - numBusyServers));
                System.out.println("numServer: " + numServer);
                System.out.println("numBusyServer: " + numServer);
                System.out.println("server ancora da rimuovere: " + numServerToRemove);
                for (Server server: servers) {
                    System.out.println("server " + server.id + " libero: " + server.idle);
                }
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

    //aggiorna le statistiche per il centro
    public void updateStatistics(Event newEvent) {
        if (numJobs > 0) {
            double delta = (newEvent.getTime() - currentEvent.getTime());
            area[0].node    += delta * numJobs;
            area[0].queue   += delta * (numJobs - numBusyServers);
        }

        //aggiorna l'evento corrente per il centro
        currentEvent = newEvent;
    }

    public void printStatistics() {
        DecimalFormat f = new DecimalFormat("###0.000");
        DecimalFormat g = new DecimalFormat("###0.000");

        System.out.println("\nfor " + completedJobs + " jobs the service node statistics are:\n");

        System.out.println("  avg interarrivals .. =   " + f.format(lastArrive / completedJobs));
        System.out.println("  avg wait ........... =   " + f.format(area[0].node / completedJobs));
        System.out.println("  avg # in node ...... =   " + f.format(area[0].node / lastDeparture));
        System.out.println("  avg delay .......... =   " + f.format(area[0].queue / completedJobs));
        System.out.println("  avg # in queue ..... =   " + f.format(area[0].queue / lastDeparture));


        System.out.println("\nthe server statistics are:\n");
        System.out.println("    server     utilization     avg service      share");
        for (int s = 0; s < numServer; s++) {
            System.out.print("       " + s + "          " + g.format(servers.get(s).service / lastDeparture) + "            ");
            System.out.println(f.format(servers.get(s).service / servers.get(s).served) + "         " + g.format(servers.get(s).served / (double)completedJobs));
        }
        //share = percentuale di job processati da quel server sul totale

        System.out.println("");
    }

    public double getAvgInterarrival(int i) {
        return (lastArrive - firstArrive) / completedJobs;
    }

    public double getAvgWait(int i) {
        return area[0].node / completedJobs;
    }

    public double getAvgDelay(int i) {
        return area[0].queue / completedJobs;
    }

    public double getAvgNode(int i) {
        return area[0].node / (lastDeparture - firstArrive);
    }

    public double getAvgQueue(int i) {
        return area[0].queue / (lastDeparture - firstArrive);
    }


    //ritorna l'utilizzazione dell'i-esimo server del centro
    public double getUtilization(int i) {
        return servers.get(i).service / (lastDeparture - firstArrive);
    }
}
