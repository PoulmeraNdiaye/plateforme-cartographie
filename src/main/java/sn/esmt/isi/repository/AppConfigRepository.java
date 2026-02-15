package sn.esmt.isi.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sn.esmt.isi.model.AppConfig;

@Repository
public interface AppConfigRepository extends JpaRepository<AppConfig, Long> {
}
