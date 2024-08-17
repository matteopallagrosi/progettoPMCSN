package it.metro.utils;

public class Statistics {

    public double[] avgInterarrivals;         //tempo medio di interarrivo al centro (lambda)
    public double[] avgWait;                  //tempo medio di risposta
    public double[] avgDelay;                 //tempo medio di attesa in coda
    public double[] avgNode;                  //popolazione media nel centro
    public double[] avgQueue;                 //popolazione media in coda
    public double[] utilization;            //utilizzazione di ciascuno dei server (tutti i server hanno pari utilizzazione data la politica di assegnamento dei job)

    public Statistics(int numServer) {
        avgInterarrivals = new double[numServer];
        avgWait = new double[numServer];
        avgDelay = new double[numServer];
        avgNode = new double[numServer];
        avgQueue = new double[numServer];
        utilization = new double[numServer];
    }
}
