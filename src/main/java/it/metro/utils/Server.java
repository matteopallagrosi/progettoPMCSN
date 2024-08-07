package it.metro.utils;

public class Server {
    public boolean idle = true;
    public double service = 0;                 //tempo totale di servizio svolto dal server
    public long served = 0;                    //numero di job serviti
    public double lastDeparture = 0;           //tempo dell'ultima partenza di un job dal server
}
