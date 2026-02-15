package sn.esmt.isi.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sn.esmt.isi.model.Domaine;

@Repository
public interface DomaineRepository extends JpaRepository<Domaine, Long> {
}
