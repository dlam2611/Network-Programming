package model;

public class ExchangeRate {

    private String time;
    private double tokyo;
    private double newYork;
    private double hongKong;

    public ExchangeRate(
            String time,
            double tokyo,
            double newYork,
            double hongKong
    ) {
        this.time = time;
        this.tokyo = tokyo;
        this.newYork = newYork;
        this.hongKong = hongKong;
    }

    public String getTime() {
        return time;
    }

    public double getTokyo() {
        return tokyo;
    }

    public double getNewYork() {
        return newYork;
    }

    public double getHongKong() {
        return hongKong;
    }
    @Override
    public String toString() {
        return String.format(
                "TIME=%s|Tokyo=%.4f|NewYork=%.4f|HongKong=%.4f",
                time,
                tokyo,
                newYork,
                hongKong
        );
    }
    public String toProtocolString() {
        return "time=" + time + "|tokyo=" + tokyo + "|newYork=" + newYork + "|hongKong=" + hongKong;
    }

}
