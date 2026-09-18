package com.trizen.photoshare;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The requirement calls these out explicitly, so each one gets a test. */
class EventAccessControlTest extends AbstractApiTest {

    private MockMultipartFile photo(String name) {
        return new MockMultipartFile("files", name, "image/jpeg", "fake-jpeg-bytes".getBytes());
    }

    @Test
    @DisplayName("a team member cannot reach an event they were not assigned to")
    void memberCannotSeeForeignEvent() throws Exception {
        Long mine = createEvent(adminToken, "Wedding A");

        String otherAdminToken = registerAdmin("Other Lead", uniqueEmail("other"), "Password@123");
        Long theirs = createEvent(otherAdminToken, "Wedding B");

        String memberEmail = uniqueEmail("member");
        addMember(adminToken, mine, "Camera One", memberEmail, "Password@123");
        String memberToken = login(memberEmail, "Password@123");

        mockMvc.perform(get("/api/events/" + mine).header("Authorization", bearer(memberToken)))
                .andExpect(status().isOk());

        // Not found rather than forbidden, so event ids cannot be enumerated.
        mockMvc.perform(get("/api/events/" + theirs).header("Authorization", bearer(memberToken)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("a team member cannot publish a gallery")
    void memberCannotPublish() throws Exception {
        Long eventId = createEvent(adminToken, "Reception");
        String memberEmail = uniqueEmail("member");
        addMember(adminToken, eventId, "Camera Two", memberEmail, "Password@123");
        String memberToken = login(memberEmail, "Password@123");

        mockMvc.perform(post("/api/events/" + eventId + "/gallery/publish")
                        .header("Authorization", bearer(memberToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of())))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("a team member sees only their own uploads, the admin sees everything")
    void memberSeesOnlyOwnPhotos() throws Exception {
        Long eventId = createEvent(adminToken, "Sangeet");
        String memberEmail = uniqueEmail("member");
        addMember(adminToken, eventId, "Camera Three", memberEmail, "Password@123");
        String memberToken = login(memberEmail, "Password@123");

        mockMvc.perform(multipart("/api/events/" + eventId + "/photos")
                        .file(photo("member-1.jpg"))
                        .header("Authorization", bearer(memberToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uploaded.length()").value(1));

        mockMvc.perform(multipart("/api/events/" + eventId + "/photos")
                        .file(photo("admin-1.jpg"))
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/events/" + eventId + "/photos")
                        .header("Authorization", bearer(memberToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(1));

        mockMvc.perform(get("/api/events/" + eventId + "/photos")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(2));
    }

    @Test
    @DisplayName("an unsupported file is reported without failing the rest of the batch")
    void badFileIsReportedNotFatal() throws Exception {
        Long eventId = createEvent(adminToken, "Haldi");
        MockMultipartFile bad = new MockMultipartFile("files", "notes.pdf",
                "application/pdf", "not-an-image".getBytes());

        mockMvc.perform(multipart("/api/events/" + eventId + "/photos")
                        .file(photo("good.jpg"))
                        .file(bad)
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uploaded.length()").value(1))
                .andExpect(jsonPath("$.failed.length()").value(1))
                .andExpect(jsonPath("$.failed[0].filename").value("notes.pdf"));
    }

    @Test
    @DisplayName("a member cannot upload into an event they are not on")
    void memberCannotUploadToForeignEvent() throws Exception {
        Long mine = createEvent(adminToken, "Mehendi");
        String otherAdminToken = registerAdmin("Other Lead", uniqueEmail("other"), "Password@123");
        Long theirs = createEvent(otherAdminToken, "Someone Else");

        String memberEmail = uniqueEmail("member");
        addMember(adminToken, mine, "Camera Four", memberEmail, "Password@123");
        String memberToken = login(memberEmail, "Password@123");

        mockMvc.perform(multipart("/api/events/" + theirs + "/photos")
                        .file(photo("sneaky.jpg"))
                        .header("Authorization", bearer(memberToken)))
                .andExpect(status().isNotFound());
    }
}
