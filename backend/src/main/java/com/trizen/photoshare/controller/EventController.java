package com.trizen.photoshare.controller;

import com.trizen.photoshare.dto.EventDtos.AddMemberRequest;
import com.trizen.photoshare.dto.EventDtos.CreateEventRequest;
import com.trizen.photoshare.dto.EventDtos.EventDto;
import com.trizen.photoshare.dto.EventDtos.MemberDto;
import com.trizen.photoshare.security.AppUserPrincipal;
import com.trizen.photoshare.service.EventService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @GetMapping
    public List<EventDto> list(@AuthenticationPrincipal AppUserPrincipal principal) {
        return eventService.listVisible(principal);
    }

    @PostMapping
    public ResponseEntity<EventDto> create(@AuthenticationPrincipal AppUserPrincipal principal,
                                           @Valid @RequestBody CreateEventRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(eventService.create(principal, request));
    }

    @GetMapping("/{eventId}")
    public EventDto get(@AuthenticationPrincipal AppUserPrincipal principal,
                        @PathVariable Long eventId) {
        return eventService.getOne(eventId, principal);
    }

    @GetMapping("/{eventId}/members")
    public List<MemberDto> members(@AuthenticationPrincipal AppUserPrincipal principal,
                                   @PathVariable Long eventId) {
        return eventService.listMembers(eventId, principal);
    }

    @PostMapping("/{eventId}/members")
    public ResponseEntity<MemberDto> addMember(@AuthenticationPrincipal AppUserPrincipal principal,
                                               @PathVariable Long eventId,
                                               @Valid @RequestBody AddMemberRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(eventService.addMember(eventId, principal, request));
    }

    @DeleteMapping("/{eventId}/members/{userId}")
    public ResponseEntity<Void> removeMember(@AuthenticationPrincipal AppUserPrincipal principal,
                                             @PathVariable Long eventId,
                                             @PathVariable Long userId) {
        eventService.removeMember(eventId, userId, principal);
        return ResponseEntity.noContent().build();
    }
}
