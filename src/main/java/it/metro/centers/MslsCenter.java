package it.metro.centers;

import it.metro.events.Event;
import it.metro.utils.Area;
import it.metro.utils.Rvgs;
import it.metro.utils.Server;
import it.metro.utils.Time;

import java.text.DecimalFormat;

//Multi Server Loss System (quindi non è presente la coda)
public abstract class MslsCenter extends Center {

    public double lastArrive;                      //ultimo arrivo presso questo centro
    public double lastDeparture;
    public double arrivalJobs;                     //tiene traccia del numero totale di job che arrivano presso il centro
    public double rejectedJob;                     //tiene traccia del numero di job che trovano tutti i server occupati all'arrivo

    public MslsCenter(int id, int numServer, Rvgs v) {
        super(id, numServer, v);
        this.area = new Area[1];
        this.area[0] = new Area();
    }

    @Override
    public int processArrival() {
        arrivalJobs++;
        lastArrive = currentEvent.getTime();
        //se è disponibile un server, il job viene servito
        //ritorna l'indice del server per cui deve essere prodotto un tempo di completamento
        if (numJobs < numServer) {
            numJobs++;
            lastService = getService();
            int serverIndex = findIdleServer();
            Server selectedServer = servers[serverIndex];
            selectedServer.service += lastService;
            selectedServer.served++;
            selectedServer.idle = false;
            numBusyServers++;
            //il controller produce un evento di completamento
            return serverIndex;
        }

        //altrimenti il job viene "scartato", ossia supera il centro senza effettuare i controlli e raggiunge direttamente il centro successivo
        //deve quindi essere prodotto un arrivo al centro successivo
        rejectedJob++;
        return -1;  //nel caso in cui non deve essere prodotto un evento di completamento, ma un arrivo al centro successivo, ritorna -1
    }

    @Override
    public int processDeparture() {
        lastDeparture = currentEvent.getTime();
        completedJobs += 1;
        numJobs -= 1;
        //aggiorna l'ultimo completamento presso il server coinvolto (necessario per selezionare il server libero da più tempo)
        currentEvent.getServer().lastDeparture = currentEvent.getTime();
        //il server torna a essere libero
        currentEvent.getServer().idle = true;
        numBusyServers--;
        return -1;
    }

    //per completezza teniamo traccia anche del tempo di risposta medio, ma essendo il sistema senza coda,
    //questo dovrà essere pari al tempo medio di servizio (di un singolo servente)
    @Override
    public void updateStatistics(Event newEvent) {
        if (numJobs > 0) {
            double delta = (newEvent.getTime() - currentEvent.getTime());
            area[0].node    += delta * numJobs;
        }

        currentEvent = newEvent;
    }

    @Override
    public void printStatistics() {
        DecimalFormat f = new DecimalFormat("###0.000");
        DecimalFormat g = new DecimalFormat("###0.000");

        System.out.println("\nfor " + completedJobs + " jobs the service node statistics are:\n");

        System.out.println("arrival jobs: " + arrivalJobs);
        System.out.println("rejected jobs: " + rejectedJob);
        System.out.println("loss probability (a job finds all servers busy): " + f.format(rejectedJob/arrivalJobs));
        System.out.println("  avg interarrivals (all jobs).. =   " + f.format(lastArrive / arrivalJobs));
        System.out.println("  avg interarrivals (accepted jobs) .. =   " + f.format(lastArrive / completedJobs));
        System.out.println("  avg wait ........... =   " + f.format(area[0].node / completedJobs));
        System.out.println("  avg # in node ...... =   " + f.format(area[0].node / lastDeparture));
        System.out.println("  avg delay .......... =   " + f.format(0));
        System.out.println("  avg # in queue ..... =   " + f.format(0));


        System.out.println("\nthe server statistics are:\n");
        System.out.println("    server     utilization     avg service      share");
        for (int s = 0; s < numServer; s++) {
            System.out.print("       " + s + "          " + g.format(servers[s].service / lastDeparture) + "            ");
            System.out.println(f.format(servers[s].service / servers[s].served) + "         " + g.format(servers[s].served / (double)completedJobs));
        }
        //share = percentuale di job processati da quel server sul totale

        System.out.println("");
    }
    public double getAvgInterarrival(int i) {
        return lastArrive / completedJobs;
    }

    public double getAvgWait(int i) {
        return area[0].node / completedJobs;
    }

    public double getAvgDelay(int i) {
        return 0;
    }

    public double getAvgNode(int i) {
        return area[0].node / lastDeparture;
    }

    public double getAvgQueue(int i) {
        return 0;
    }

    //ritorna l'utilizzazione dell'i-esimo server del centro
    public double getUtilization(int i) {
        return servers[i].service / lastDeparture;
    }

}
