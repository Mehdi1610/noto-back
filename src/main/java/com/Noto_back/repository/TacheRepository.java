package com.Noto_back.repository;

import com.Noto_back.model.StatutTache;
import com.Noto_back.model.Tache;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TacheRepository extends JpaRepository<Tache,Long> {

    // Toutes les tâches d'un dossier
    List<Tache> findByDossierId(Long dossierId);

    // Tâches d'un dossier filtrées par statut
    List<Tache> findByDossierIdAndStatut(Long dossierId, StatutTache statut);

    // Une tâche précise, en vérifiant l'ownership via le dossier -> user
    @Query("""
        SELECT t FROM Tache t
        WHERE t.id = :id AND t.dossier.user.id = :userId
    """)
    Optional<Tache> findByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    // Vérifie que la tâche appartient bien à l'utilisateur (avant update/delete)
    @Query("""
        SELECT COUNT(t) > 0 FROM Tache t
        WHERE t.id = :id AND t.dossier.user.id = :userId
    """)
    boolean existsByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);
}
