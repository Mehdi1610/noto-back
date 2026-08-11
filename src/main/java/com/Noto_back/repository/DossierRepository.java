package com.Noto_back.repository;

import com.Noto_back.model.Dossier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DossierRepository extends JpaRepository<Dossier,Long> {

    // Dossiers racines d'un utilisateur (parent = null)
    List<Dossier> findByUserIdAndParentIsNull(Long userId);

    // Sous-dossiers directs d'un dossier donné
    List<Dossier> findByParentId(Long parentId);

    // Tous les dossiers d'un utilisateur (racines + sous-dossiers), pour reconstruire l'arbre en mémoire
    List<Dossier> findByUserId(Long userId);

    // Un dossier précis, en vérifiant qu'il appartient bien à l'utilisateur (sécurité/ownership)
    Optional<Dossier> findByIdAndUserId(Long id, Long userId);

    // Vérifie qu'un dossier appartient à un utilisateur (utile avant modification/suppression)
    boolean existsByIdAndUserId(Long id, Long userId);

    @Query("""
        SELECT d FROM Dossier d
        LEFT JOIN FETCH d.taches
        WHERE d.id = :id AND d.user.id = :userId
    """)
    Optional<Dossier> findByIdAndUserIdWithTaches(@Param("id") Long id, @Param("userId") Long userId);
}
