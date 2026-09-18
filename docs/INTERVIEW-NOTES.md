# Notes for the walkthrough

The brief says you may be asked to explain your architecture or make a small
change during the evaluation. This file is the map: what each file does, why
each decision was made, and where to click when someone asks for a change.

Read this once end to end, then run the app and follow a photo from upload to
client gallery with the code open beside you. That single trace explains most of
the system.

---

## 1. The one-paragraph summary

A Spring Boot API holds all the rules; React is only a view over it. Users,
events, photos and galleries live in MySQL, but image **files** live in object
storage behind a `StorageService` interface — MySQL only ever stores the key.
Staff authenticate with a JWT; customers never get an account at all, they trade
a slug plus a PIN for a second, narrower kind of JWT that opens exactly one
gallery. Every access rule is enforced in the service layer, so the UI hiding a
button is a convenience, not the security boundary.

---

## 2. Where everything lives

### Backend — `backend/src/main/java/com/trizen/photoshare/`

| Package | Files | Job |
|---------|-------|-----|
| `entity` | `User`, `Event`, `EventMember`, `Photo`, `Gallery`, `Role` | JPA entities, one per table |
| `repository` | five Spring Data interfaces | Queries. The interesting one is `EventRepository.findVisibleTo` |
| `dto` | `AuthDtos`, `EventDtos`, `PhotoDtos`, `GalleryDtos` | Java records for request and response bodies |
| `service` | `AuthService`, `EventService`, `PhotoService`, `GalleryService`, `PinAttemptService` | All business logic and all access checks |
| `service.storage` | `StorageService`, `LocalStorageService`, `S3StorageService`, `UrlSigner` | The file abstraction |
| `security` | `JwtService`, `JwtAuthenticationFilter`, `AppUserPrincipal`, `AppUserDetailsService`, `RestAuthEntryPoint` | Authentication |
| `config` | `SecurityConfig`, `AppProperties`, `DataSeeder` | Wiring and configuration |
| `controller` | `Auth`, `Event`, `Photo`, `Gallery`, `PublicGallery`, `File` | HTTP surface, deliberately thin |
| `exception` | `ApiException` subclasses, `GlobalExceptionHandler`, `ApiError` | One error shape for the whole API |

### Frontend — `frontend/src/`

| File | Job |
|------|-----|
| `api/client.js` | Axios instance, attaches the token, normalises error messages |
| `auth/AuthContext.jsx` | Who is signed in; restores the session on reload via `/api/auth/me` |
| `components/ProtectedRoute.jsx` | Redirects to login when there is no user |
| `components/Uploader.jsx` | Drag and drop, progress, shows per-file failures |
| `components/PhotoSheet.jsx` | The contact sheet grid, doubles as the selection UI |
| `components/GalleryPanel.jsx` | Curate, publish, show link and PIN, unpublish |
| `components/TeamPanel.jsx` | Add and remove photographers |
| `pages/EventPage.jsx` | Tabs: Photos, Client gallery, Team |
| `pages/PublicGallery.jsx` | The whole customer experience: PIN screen, then grid |

---

## 3. Trace one photo end to end

This is the most useful thing to be able to narrate.

**Upload.** `Uploader.jsx` builds a `FormData` with every file under the field
name `files` and POSTs to `/api/events/{id}/photos`. `PhotoController` hands it
to `PhotoService.upload`, which first calls
`eventService.requireReadAccess(eventId, principal)` — that single line is what
stops a photographer uploading into someone else's wedding. Then it loops the
files: validate content type and size, build a key
`events/{eventId}/{uuid}.jpg`, write the bytes through `StorageService`, save a
`Photo` row with the key. A file that fails validation goes into the `failed`
list and the loop continues.

**Listing.** `PhotoService.list` picks one of two repository queries depending on
`eventService.canManage(event, principal)`: admins get `findByEvent`, team
members get `findByEventAndUploader`. That is the whole "members only see their
own photos" rule. Each DTO gets a **signed URL** from `StorageService.presignedUrl`,
not the raw path.

**Curation.** The admin marks frames in `PhotoSheet` (selection lives in React
state) and clicks Save, which PUTs the full id list to `/gallery/selection`.
`GalleryService.updateSelection` re-fetches those ids **scoped to the event**, so
an id from another event fails with a 400, then replaces the `gallery_photos`
rows.

**Publish.** `GalleryService.publish` refuses an empty selection, generates a
random 10-character slug and a 6-digit PIN, stores `BCrypt(pin)`, sets
`published = true`, and returns the plain PIN exactly once.

**Customer.** They open `/g/{slug}`. `PublicGallery.jsx` calls the teaser
endpoint for the title, shows the PIN field, and POSTs the PIN.
`GalleryService.verifyPin` checks the lockout, compares with BCrypt, and on
success issues a gallery JWT whose subject is the slug. That token goes in
session storage and is sent as `X-Gallery-Token` on refresh.

---

## 4. Decisions you should be ready to defend

**Why store files outside the database.** Image bytes in MySQL bloat the table,
slow every backup, and force every byte through the application. Storing a key
keeps rows tiny and lets the browser fetch images straight from S3. It also makes
the storage swap a config change: `app.storage.type=local|s3`.

**Why signed URLs instead of streaming through the API.** If the API served every
image, it would sit in the data path for a 1,250-photo gallery. A signed URL has
an expiry and an HMAC, so it cannot be forged or shared forever, and it mirrors
exactly what S3 presigning does — which is why swapping the implementation
changes nothing for the frontend.

**Why two JWT types.** A customer token and a staff token look identical on the
wire. Without a `typ` claim, a gallery token would be a valid `Authorization`
header. `JwtAuthenticationFilter` only accepts `typ=access`; `viewWithToken` only
accepts `typ=gallery` **and** a subject matching the requested slug. There are
tests for both.

**Why 404 instead of 403 for a foreign event.** A 403 confirms the event exists.
Returning 404 means ids leak nothing.

**Why `event_members` is a table and not a column.** A photographer is on some
events and not others. The relationship belongs between the two entities, and it
carries its own `added_at`.

**Why BCrypt for a 6-digit PIN.** Only six digits means a million possibilities,
which is brute-forceable in seconds if unthrottled. BCrypt makes each guess slow,
and `PinAttemptService` locks the gallery after five wrong attempts from a
caller. The two together are what make a short PIN acceptable.

**Why access checks live in services, not controllers.** Several controllers
touch the same event; putting the rule in `requireReadAccess` /
`requireManageAccess` means it exists once. `@PreAuthorize("hasRole('ADMIN')")`
on `GalleryController` is a coarse first gate; ownership is still checked inside.

**Why the selection is replaced wholesale.** `PUT` with the complete id list is
idempotent. Sending individual add/remove calls while someone works through a
thousand frames invites drift between the UI and the database.

**Why a partial upload is a 200.** Dropping 200 files where one is a PDF should
land 199 photos and name the one that failed. That is what `uploaded` and
`failed` are for.

---

## 5. Likely questions, short answers

**"Walk me through your architecture."** Use section 1, then draw the diagram
from the README: React → Spring Boot (filter → controller → service) → MySQL for
metadata, object storage for files, signed URLs back to the browser.

**"How do you stop a team member publishing a gallery?"** Three layers: the UI
hides the tab, `@PreAuthorize("hasRole('ADMIN')")` rejects the role, and
`requireManageAccess` rejects anyone who is not the owning admin.
`EventAccessControlTest.memberCannotPublish` proves it.

**"How would this scale to a hundred thousand photos?"** Photos are already
paginated. The next three moves: generate thumbnails on upload so grids stop
loading full-size images; upload directly from the browser to S3 with presigned
PUTs so the app server leaves the data path; move PIN throttling to Redis so it
works across nodes. The API is stateless, so it scales horizontally as is.

**"Why MySQL and not MongoDB?"** The data is relational — users own events, events
have members and photos, galleries reference photos. Foreign keys and a join
table express that exactly, and the queries are joins, not document reads.

**"What is your N+1 risk?"** Listing photos would trigger a query per uploader,
so `PhotoRepository` uses `join fetch p.uploadedBy`. `findBySlugWithPhotos` does
the same for the gallery. `open-in-view` is disabled so lazy loading cannot leak
outside a transaction and hide the problem.

**"What would you do differently with more time?"** The Known Limitations list at
the end of the README, in that order: thumbnails first, then Flyway migrations,
then httpOnly cookies for the token.

**"Where are your tests?"** `backend/src/test`. Three suites over the real HTTP
layer against H2, concentrated on the access rules, because that is where a bug
would actually hurt.

---

## 6. If they ask for a small change live

Rehearse two or three of these before the call.

| Change | Where |
|--------|-------|
| PIN length 6 → 8 | `application.yml` → `app.gallery.pin-length`. Validation already allows 4–8 |
| Allow a new image type | `app.upload.allowed-content-types` |
| Photo URLs expire faster | `app.storage.signed-url-minutes` |
| Lock after 3 wrong PINs | `app.gallery.max-pin-attempts` |
| Add a field to the event | `Event` entity → `EventDtos.EventDto` and `CreateEventRequest` → `EventService.create`/`toDto` → `EventsPage.jsx` form |
| Let members see all photos on their event | `PhotoService.list` — pick `findByEvent` unconditionally |
| Add "download all" | New endpoint on `GalleryController`, stream a zip built from `gallery.getPhotos()` via `StorageService` |
| Show who marked a photo | Add a column to `gallery_photos` — the `@ManyToMany` becomes its own entity |

The pattern for almost any change: **entity → DTO → service → controller →
frontend page**. Say that out loud; it shows you know the shape of your own code.

---

## 7. Run it before the call

```bash
docker compose up --build
# then: http://localhost:5173
```

Sign in as `admin@trizen-ai.com` / `Admin@12345`, upload a handful of photos,
mark a few, publish, copy the link and PIN, and open it in a private window.
That is the demo. It takes ninety seconds and it answers most questions before
they are asked.

Also run `cd backend && mvn test` once so you have seen the suite pass and can
say what it covers.
