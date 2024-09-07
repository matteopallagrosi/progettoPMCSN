package it.metro.utils;

public class Statistics {

    public double[] avgInterarrivals;         //tempo medio di interarrivo al centro (1/lambda) (nel caso di Msls rappresenta 1/lamba accepted)
    public double[] avgWait;                  //tempo medio di risposta
    public double[] avgDelay;                 //tempo medio di attesa in coda
    public double[] avgNode;                  //popolazione media nel centro
    public double[] avgQueue;                 //popolazione media in coda
    public double[] utilization;              //utilizzazione di ciascuno dei server (tutti i server hanno pari utilizzazione data la politica di assegnamento dei job)
    public double[] avgService;               //tempo medio di servizio presso ciascun server (tutti i server hanno valori uguali data la politica di assegnamento dei job)
    public double lossProbability;            //nel caso dei centri con perdita (come Msls) rappresenta la probabilità che un job trovi il centro occupato e venga quindi rifiutato
    public double totalInterrarival;          //nel caso del loss system rappresenta il tempo medio di interarrivo al centro, prima che i job vengano scartati

    public Statistics(int numServer) {
        avgInterarrivals = new double[numServer];
        avgWait = new double[numServer];
        avgDelay = new double[numServer];
        avgNode = new double[numServer];
        avgQueue = new double[numServer];
        utilization = new double[numServer];
        avgService = new double[numServer];
    }
}
