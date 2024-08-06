package it.metro.centers;

public abstract class Center {

    final int ID;                   //identificativo univoco del centro
    public int numServer;           //numero di servers nel centro
    public int numJobs = 0;             //numero di jobs attualmente nel centro (servers + coda)
    public int completedJobs = 0;       //numero di jobs processati dal server



    public Center(int id, int numServer) {
        ID = id;
        this.numServer = numServer; //inizializza il numero di server per il centro
    }

    //ritorna il centro successivo (il raggiungimento del centro successivo può essere probabilistico)
    public abstract Center getNextCenter();

    //processa l'arrivo di un job nel centro
    public abstract void processArrival();

    //processa il completamento di un job nel centro
    public abstract void processDeparture();

}
