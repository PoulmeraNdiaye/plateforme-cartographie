package sn.esmt.isi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class PlateformeCartographieApplication {

    public static void main(String[] args) {
        SpringApplication.run(PlateformeCartographieApplication.class, args);
    }

}
