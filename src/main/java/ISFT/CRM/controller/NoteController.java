package ISFT.CRM.controller;


import ISFT.CRM.entity.Note;
import ISFT.CRM.service.NoteService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notes")
public class NoteController {

    private final NoteService noteService;

    public NoteController(NoteService noteService) {
        this.noteService = noteService;
    }

    @GetMapping
    public List<Note> getAllNotes() {
        return noteService.getAllNotes();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Note> getNoteById(
            @PathVariable Long id) {

        return noteService.getNoteById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public Note updateNote(
            @PathVariable Long id,
            @RequestBody Note noteDetails) {

        return noteService.updateNote(id, noteDetails);
    }

    @GetMapping("/lead/{leadId}")
    public List<Note> getNotesByLead(
            @PathVariable Long leadId) {

        return noteService.getNotesByLead(leadId);
    }

    @GetMapping("/user/{userId}")
    public List<Note> getNotesByUser(
            @PathVariable Long userId) {

        return noteService.getNotesByUser(userId);
    }

    @PostMapping
    public Note createNote(
            @RequestBody Note note) {

        return noteService.createNote(note);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNote(@PathVariable Long id) {

        noteService.deleteNote(id);

        return ResponseEntity.noContent().build();
    }
}