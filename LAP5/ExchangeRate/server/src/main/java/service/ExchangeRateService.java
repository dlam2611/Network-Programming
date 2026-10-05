package service;

import model.ExchangeRate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;

public class ExchangeRateService {

    private final Random random = new Random();
    public ExchangeRate getLatestRate() {

        double tokyo = randomRate(140, 160);

        double newYork = randomRate(130, 150);

        double hongKong = randomRate(135, 155);

        String time = LocalDateTime.now()
                .format(
                        DateTimeFormatter.ofPattern(
                                "dd/MM/yyyy HH:mm:ss"
                        )
                );

        return new ExchangeRate(
                time,
                tokyo,
                newYork,
                hongKong
        );
    }

    private double randomRate(
            double min,
            double max
    ) {
        return min
                + (max - min) * random.nextDouble();
    }
}

