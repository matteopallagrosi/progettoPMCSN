package it.metro.utils;

//tiene traccia del tempo di simulazione
public class Time {

    public double current;      //clock di simulazione corrente
    public double next;         //tempo del prossimo evento

    public Time(double current, double next) {
        this.current = current;
        this.next = next;
    }
}
