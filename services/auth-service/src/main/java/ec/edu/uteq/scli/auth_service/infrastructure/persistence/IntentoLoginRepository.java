package ec.edu.uteq.scli.auth_service.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface IntentoLoginRepository extends JpaRepository<IntentoLogin, UUID> { }
