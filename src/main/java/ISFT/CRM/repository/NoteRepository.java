package ISFT.CRM.repository;


import ISFT.CRM.entity.Note;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NoteRepository extends JpaRepository<Note, Long> {

    List<Note> findByLeadId(Long leadId);
    void deleteByLeadId(Long leadId);

    List<Note> findByUserId(Long userId);
}