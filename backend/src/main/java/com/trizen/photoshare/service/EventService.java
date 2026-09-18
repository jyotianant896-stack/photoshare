package com.trizen.photoshare.service;

import com.trizen.photoshare.dto.EventDtos.AddMemberRequest;
import com.trizen.photoshare.dto.EventDtos.CreateEventRequest;
import com.trizen.photoshare.dto.EventDtos.EventDto;
import com.trizen.photoshare.dto.EventDtos.MemberDto;
import com.trizen.photoshare.entity.Event;
import com.trizen.photoshare.entity.EventMember;
import com.trizen.photoshare.entity.Gallery;
import com.trizen.photoshare.entity.Role;
import com.trizen.photoshare.entity.User;
import com.trizen.photoshare.exception.BadRequestException;
import com.trizen.photoshare.exception.ConflictException;
import com.trizen.photoshare.exception.ForbiddenException;
import com.trizen.photoshare.exception.NotFoundException;
import com.trizen.photoshare.repository.EventMemberRepository;
import com.trizen.photoshare.repository.EventRepository;
import com.trizen.photoshare.repository.GalleryRepository;
import com.trizen.photoshare.repository.PhotoRepository;
import com.trizen.photoshare.repository.UserRepository;
import com.trizen.photoshare.security.AppUserPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;
import java.util.Optional;

@Service
public class EventService {

    private static final String PASSWORD_ALPHABET =
            "abcdefghijkmnopqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    private final EventRepository eventRepository;
    private final EventMemberRepository eventMemberRepository;
    private final PhotoRepository photoRepository;
    private final GalleryRepository galleryRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom random = new SecureRandom();

    public EventService(EventRepository eventRepository,
                        EventMemberRepository eventMemberRepository,
                        PhotoRepository photoRepository,
                        GalleryRepository galleryRepository,
                        UserRepository userRepository,
                        PasswordEncoder passwordEncoder) {
        this.eventRepository = eventRepository;
        this.eventMemberRepository = eventMemberRepository;
        this.photoRepository = photoRepository;
        this.galleryRepository = galleryRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public EventDto create(AppUserPrincipal principal, CreateEventRequest request) {
        if (!principal.isAdmin()) {
            throw new ForbiddenException("Only an admin can create events");
        }
        User owner = userRepository.getReferenceById(principal.getId());
        Event event = new Event(request.name().trim(), request.description(),
                request.eventDate(), owner);
        eventRepository.save(event);
        return toDto(event, principal);
    }

    @Transactional(readOnly = true)
    public List<EventDto> listVisible(AppUserPrincipal principal) {
        return eventRepository.findVisibleTo(principal.getId()).stream()
                .map(event -> toDto(event, principal))
                .toList();
    }

    @Transactional(readOnly = true)
    public EventDto getOne(Long eventId, AppUserPrincipal principal) {
        Event event = requireReadAccess(eventId, principal);
        return toDto(event, principal);
    }

    /**
     * An event is readable by its owner and by the team members assigned to it.
     * Everyone else gets a 404 rather than a 403 so ids cannot be probed.
     */
    @Transactional(readOnly = true)
    public Event requireReadAccess(Long eventId, AppUserPrincipal principal) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event not found"));
        boolean owner = event.getOwner().getId().equals(principal.getId());
        boolean member = eventMemberRepository.existsByEventIdAndUserId(eventId, principal.getId());
        if (!owner && !member) {
            throw new NotFoundException("Event not found");
        }
        return event;
    }

    /** Managing means curating and publishing: the owning admin only. */
    @Transactional(readOnly = true)
    public Event requireManageAccess(Long eventId, AppUserPrincipal principal) {
        Event event = requireReadAccess(eventId, principal);
        if (!principal.isAdmin() || !event.getOwner().getId().equals(principal.getId())) {
            throw new ForbiddenException("Only the admin who owns this event can do that");
        }
        return event;
    }

    public boolean canManage(Event event, AppUserPrincipal principal) {
        return principal.isAdmin() && event.getOwner().getId().equals(principal.getId());
    }

    @Transactional
    public MemberDto addMember(Long eventId, AppUserPrincipal principal, AddMemberRequest request) {
        Event event = requireManageAccess(eventId, principal);
        String email = request.email().trim().toLowerCase();

        Optional<User> existing = userRepository.findByEmailIgnoreCase(email);
        String generatedPassword = null;
        User member;

        if (existing.isPresent()) {
            member = existing.get();
            if (member.getId().equals(event.getOwner().getId())) {
                throw new BadRequestException("The event owner is already on this event");
            }
            if (eventMemberRepository.existsByEventIdAndUserId(eventId, member.getId())) {
                throw new ConflictException("That person is already on this event");
            }
        } else {
            String password = request.password();
            if (password == null || password.isBlank()) {
                password = generatePassword();
                generatedPassword = password;
            }
            member = new User(request.name().trim(), email,
                    passwordEncoder.encode(password), Role.TEAM_MEMBER);
            userRepository.save(member);
        }

        EventMember assignment = eventMemberRepository.save(new EventMember(event, member));
        return new MemberDto(member.getId(), member.getName(), member.getEmail(),
                member.getRole().name(), assignment.getAddedAt(), generatedPassword);
    }

    @Transactional(readOnly = true)
    public List<MemberDto> listMembers(Long eventId, AppUserPrincipal principal) {
        requireReadAccess(eventId, principal);
        return eventMemberRepository.findMembersOf(eventId).stream()
                .map(m -> new MemberDto(m.getUser().getId(), m.getUser().getName(),
                        m.getUser().getEmail(), m.getUser().getRole().name(),
                        m.getAddedAt(), null))
                .toList();
    }

    @Transactional
    public void removeMember(Long eventId, Long userId, AppUserPrincipal principal) {
        requireManageAccess(eventId, principal);
        EventMember assignment = eventMemberRepository.findByEventIdAndUserId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("That person is not on this event"));
        eventMemberRepository.delete(assignment);
    }

    private String generatePassword() {
        StringBuilder builder = new StringBuilder(12);
        for (int i = 0; i < 12; i++) {
            builder.append(PASSWORD_ALPHABET.charAt(random.nextInt(PASSWORD_ALPHABET.length())));
        }
        return builder.toString();
    }

    private EventDto toDto(Event event, AppUserPrincipal principal) {
        boolean published = galleryRepository.findByEventId(event.getId())
                .map(Gallery::isAccessible)
                .orElse(false);
        return new EventDto(
                event.getId(),
                event.getName(),
                event.getDescription(),
                event.getEventDate(),
                event.getOwner().getName(),
                photoRepository.countByEventId(event.getId()),
                eventMemberRepository.countByEventId(event.getId()),
                canManage(event, principal),
                published,
                event.getCreatedAt());
    }
}
