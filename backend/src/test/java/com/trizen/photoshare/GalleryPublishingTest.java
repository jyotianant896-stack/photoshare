package com.trizen.photoshare;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GalleryPublishingTest extends AbstractApiTest {

    private Long uploadPhoto(Long eventId) throws Exception {
        MvcResult result = mockMvc.perform(multipart("/api/events/" + eventId + "/photos")
                        .file(new MockMultipartFile("files", "shot.jpg", "image/jpeg",
                                "fake-jpeg-bytes".getBytes()))
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andReturn();
        return json(result).get("uploaded").get(0).get("id").asLong();
    }

    private void select(Long eventId, List<Long> photoIds) throws Exception {
        mockMvc.perform(put("/api/events/" + eventId + "/gallery/selection")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("photoIds", photoIds))))
                .andExpect(status().isOk());
    }

    private JsonNode publish(Long eventId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/events/" + eventId + "/gallery/publish")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("title", "Wedding highlights"))))
                .andExpect(status().isOk())
                .andReturn();
        return json(result);
    }

    @Test
    @DisplayName("publishing returns a shareable slug and a PIN")
    void publishGeneratesLinkAndPin() throws Exception {
        Long eventId = createEvent(adminToken, "Wedding");
        Long photoId = uploadPhoto(eventId);
        select(eventId, List.of(photoId));

        JsonNode published = publish(eventId);
        assertNotNull(published.get("slug").asText());
        assertEquals(6, published.get("pin").asText().length());
        assertEquals(1, published.get("photoCount").asInt());
    }

    @Test
    @DisplayName("a gallery with no selected photos cannot be published")
    void emptySelectionCannotPublish() throws Exception {
        Long eventId = createEvent(adminToken, "Empty");
        mockMvc.perform(get("/api/events/" + eventId + "/gallery")
                .header("Authorization", bearer(adminToken)));

        mockMvc.perform(post("/api/events/" + eventId + "/gallery/publish")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of())))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("the correct PIN unlocks the gallery and the wrong one does not")
    void pinGatesAccess() throws Exception {
        Long eventId = createEvent(adminToken, "Wedding");
        Long photoId = uploadPhoto(eventId);
        select(eventId, List.of(photoId));
        JsonNode published = publish(eventId);
        String slug = published.get("slug").asText();
        String pin = published.get("pin").asText();

        mockMvc.perform(post("/api/public/galleries/" + slug + "/access")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("pin", "000000")))
                        .header("X-Forwarded-For", "203.0.113.10"))
                .andExpect(status().isForbidden());

        MvcResult unlocked = mockMvc.perform(post("/api/public/galleries/" + slug + "/access")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("pin", pin)))
                        .header("X-Forwarded-For", "203.0.113.11"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gallery.photoCount").value(1))
                .andReturn();

        String galleryToken = json(unlocked).get("accessToken").asText();

        mockMvc.perform(get("/api/public/galleries/" + slug + "/photos")
                        .header("X-Gallery-Token", galleryToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.photos.length()").value(1));
    }

    @Test
    @DisplayName("photos stay locked without a gallery token")
    void photosNeedAToken() throws Exception {
        Long eventId = createEvent(adminToken, "Wedding");
        Long photoId = uploadPhoto(eventId);
        select(eventId, List.of(photoId));
        String slug = publish(eventId).get("slug").asText();

        mockMvc.perform(get("/api/public/galleries/" + slug + "/photos"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("a token for one gallery does not open another")
    void galleryTokenIsScopedToItsSlug() throws Exception {
        Long firstEvent = createEvent(adminToken, "First");
        select(firstEvent, List.of(uploadPhoto(firstEvent)));
        JsonNode first = publish(firstEvent);

        Long secondEvent = createEvent(adminToken, "Second");
        select(secondEvent, List.of(uploadPhoto(secondEvent)));
        JsonNode second = publish(secondEvent);

        MvcResult unlocked = mockMvc.perform(
                        post("/api/public/galleries/" + first.get("slug").asText() + "/access")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                        Map.of("pin", first.get("pin").asText())))
                                .header("X-Forwarded-For", "203.0.113.20"))
                .andExpect(status().isOk())
                .andReturn();
        String firstToken = json(unlocked).get("accessToken").asText();

        mockMvc.perform(get("/api/public/galleries/" + second.get("slug").asText() + "/photos")
                        .header("X-Gallery-Token", firstToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("an unpublished gallery is invisible to customers")
    void unpublishedGalleryIsHidden() throws Exception {
        Long eventId = createEvent(adminToken, "Draft");
        select(eventId, List.of(uploadPhoto(eventId)));
        JsonNode published = publish(eventId);
        String slug = published.get("slug").asText();

        mockMvc.perform(post("/api/events/" + eventId + "/gallery/unpublish")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/public/galleries/" + slug))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("repeated wrong PINs lock the gallery for that caller")
    void pinBruteForceIsThrottled() throws Exception {
        Long eventId = createEvent(adminToken, "Wedding");
        select(eventId, List.of(uploadPhoto(eventId)));
        String slug = publish(eventId).get("slug").asText();

        for (int attempt = 0; attempt < 5; attempt++) {
            mockMvc.perform(post("/api/public/galleries/" + slug + "/access")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(Map.of("pin", "111111")))
                    .header("X-Forwarded-For", "198.51.100.5"));
        }

        mockMvc.perform(post("/api/public/galleries/" + slug + "/access")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("pin", "111111")))
                        .header("X-Forwarded-For", "198.51.100.5"))
                .andExpect(status().isTooManyRequests());
    }
}
