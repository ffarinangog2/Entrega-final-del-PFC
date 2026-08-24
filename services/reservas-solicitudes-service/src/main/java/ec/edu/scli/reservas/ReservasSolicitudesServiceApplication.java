package ec.edu.scli.reservas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;

@SpringBootApplication
@EnableRetry
public class ReservasSolicitudesServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ReservasSolicitudesServiceApplication.class, args);
    }

}
