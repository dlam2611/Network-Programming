package model;

public class ExchangeRate {

    private String time;
    private String tokyo;
    private String newYork;
    private String hongKong;

    public ExchangeRate(
            String time,
            String tokyo,
            String newYork,
            String hongKong
    ) {
        this.time = time;
        this.tokyo = tokyo;
        this.newYork = newYork;
        this.hongKong = hongKong;
    }

    public String getTime() {
        return time;
    }

    public String getTokyo() {
        return tokyo;
    }

    public String getNewYork() {
        return newYork;
    }

    public String getHongKong() {
        return hongKong;
    }

    public static ExchangeRate fromString(
            String data
    ) {

        String[] values = data.split("\\|");

        String time =
                values[0].substring(5);

        String tokyo =
                values[1].substring(6);

        String newYork =
                values[2].substring(8);

        String hongKong =
                values[3].substring(9);

        return new ExchangeRate(
                time,
                tokyo,
                newYork,
                hongKong
        );
    }

    @Override
    public String toString() {
        return "ExchangeRate{" +
                "time='" + time + '\'' +
                ", tokyo='" + tokyo + '\'' +
                ", newYork='" + newYork + '\'' +
                ", hongKong='" + hongKong + '\'' +
                '}';
    }
}

