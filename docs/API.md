# API reference

Base URL: `http://localhost:8080` in development.

Two kinds of credential:

- **Access token** — issued at login, sent as `Authorization: Bearer <token>`.
  Used by admins and team members.
- **Gallery token** — issued when a customer enters the correct PIN, sent as
  `X-Gallery-Token: <token>`. Scoped to one gallery slug and short lived.

They are not interchangeable: the staff filter rejects gallery tokens, and the
public endpoint rejects access tokens.

---

## Auth

### `POST /api/auth/register`
Creates a studio lead (ADMIN). Team members are created by an admin instead.

```json
{ "name": "Priya Nair", "email": "priya@studio.com", "password": "Password@123" }
```

`201 Created`

```json
{
  "token": "eyJhbGciOi...",
  "expiresAt": "2026-09-17T04:12:00Z",
  "user": { "id": 1, "name": "Priya Nair", "email": "priya@studio.com", "role": "ADMIN" }
}
```

Errors: `409` email taken, `400` validation with `fieldErrors`.

### `POST /api/auth/login`
```json
{ "email": "priya@studio.com", "password": "Password@123" }
```
`200 OK` with the same body as register. `401` on a wrong password.

### `GET /api/auth/me`
Returns the current user. Used on reload to restore the session.

---

## Events

### `GET /api/events`
Every event the caller owns plus every event they are assigned to.

```json
[
  {
    "id": 1,
    "name": "Arjun & Priya Wedding",
    "description": "Two day celebration",
    "eventDate": "2026-09-09",
    "ownerName": "Priya Nair",
    "photoCount": 1250,
    "memberCount": 3,
    "canManage": true,
    "galleryPublished": true,
    "createdAt": "2026-09-01T06:00:00Z"
  }
]
```

`canManage` tells the UI whether to show curation controls. The server checks
ownership again on every write regardless.

### `POST /api/events` — admin only
```json
{ "name": "Arjun & Priya Wedding", "description": "optional", "eventDate": "2026-09-09" }
```
`201 Created` with the event.

### `GET /api/events/{eventId}`
`200` for the owner or an assigned member, `404` for anyone else.

### `GET /api/events/{eventId}/members`
```json
[{ "id": 2, "name": "Arjun Rao", "email": "arjun@studio.com",
   "role": "TEAM_MEMBER", "addedAt": "2026-09-02T09:00:00Z", "generatedPassword": null }]
```

### `POST /api/events/{eventId}/members` — owning admin
```json
{ "name": "Meera Iyer", "email": "meera@studio.com", "password": "optional" }
```
If no account exists for the email, one is created as TEAM_MEMBER. Omit
`password` and the API generates one and returns it in `generatedPassword` —
the only time it is ever visible. If the account already exists it is simply
assigned to the event.

`201 Created`. Errors: `409` already on this event, `403` not the owner.

### `DELETE /api/events/{eventId}/members/{userId}` — owning admin
`204 No Content`. Photos the member already uploaded stay with the event.

---

## Photos

### `GET /api/events/{eventId}/photos?page=0&size=60`
An admin gets every photo on the event. A team member gets only their own.

```json
{
  "items": [
    {
      "id": 41,
      "filename": "DSC_4821.jpg",
      "contentType": "image/jpeg",
      "fileSize": 5242880,
      "uploadedById": 2,
      "uploadedByName": "Arjun Rao",
      "createdAt": "2026-09-09T14:22:10Z",
      "url": "http://localhost:8080/api/files/events/1/9f2c....jpg?expires=1789...&signature=...",
      "selected": true
    }
  ],
  "page": 0, "size": 60, "totalItems": 1250, "totalPages": 21
}
```

`url` is signed and expires in 30 minutes by default. `selected` reflects whether
the photo is currently in the client gallery.

### `POST /api/events/{eventId}/photos`
`multipart/form-data`, repeat the field `files` for each photo. Up to 50 files,
25 MB each, JPEG/PNG/WebP/HEIC.

`200 OK` — note that a partial failure is still a success:

```json
{
  "uploaded": [ { "id": 41, "filename": "DSC_4821.jpg", "...": "..." } ],
  "failed": [ { "filename": "notes.pdf", "reason": "Only JPEG, PNG, WebP and HEIC images are accepted" } ]
}
```

Errors: `404` not on this event, `413` request exceeds the server limit.

### `DELETE /api/photos/{photoId}`
Allowed for the uploader or the owning admin. Removes the row, drops the photo
from the gallery selection, then deletes the object. `204 No Content`.

---

## Gallery — admin

### `GET /api/events/{eventId}/gallery`
Creates the draft on first call, then returns it.

```json
{
  "id": 7, "slug": "k3m9xq2p7t", "title": "Arjun & Priya Wedding",
  "published": true,
  "publishedAt": "2026-09-14T11:02:00Z",
  "expiresAt": null,
  "viewCount": 12,
  "selectedCount": 600,
  "shareUrl": "http://localhost:5173/g/k3m9xq2p7t",
  "selectedPhotoIds": [41, 42, 58]
}
```

### `PUT /api/events/{eventId}/gallery/selection`
Replaces the selection wholesale, which makes it idempotent.
```json
{ "photoIds": [41, 42, 58] }
```
`400` if any id belongs to a different event.

### `POST /api/events/{eventId}/gallery/publish`
```json
{ "title": "Wedding highlights", "pin": "482917", "expiresAt": "2026-12-31T23:59:59Z" }
```
All three are optional. Omit `pin` and a random six digit one is generated.

```json
{
  "slug": "k3m9xq2p7t",
  "shareUrl": "http://localhost:5173/g/k3m9xq2p7t",
  "pin": "482917",
  "title": "Wedding highlights",
  "photoCount": 600,
  "publishedAt": "2026-09-14T11:02:00Z",
  "expiresAt": null
}
```

**This is the only response that ever contains the PIN.** It is stored as a
BCrypt hash. Publishing again issues a new PIN and invalidates the old one.

Errors: `400` nothing selected or expiry in the past, `403` not the owning admin.

### `POST /api/events/{eventId}/gallery/unpublish`
Takes the link offline immediately. `200` with the gallery.

---

## Gallery — customer (no account)

### `GET /api/public/galleries/{slug}`
Just enough to render the unlock screen. Nothing behind the PIN is included.
```json
{ "title": "Wedding highlights", "eventName": "Arjun & Priya Wedding", "pinRequired": true }
```
`404` if the gallery is unpublished, expired, or the slug is wrong.

### `POST /api/public/galleries/{slug}/access`
```json
{ "pin": "482917" }
```
`200 OK`
```json
{
  "accessToken": "eyJhbGciOi...",
  "expiresInSeconds": 7200,
  "gallery": {
    "title": "Wedding highlights",
    "eventName": "Arjun & Priya Wedding",
    "eventDate": "2026-09-09",
    "photoCount": 600,
    "photos": [{ "id": 41, "filename": "DSC_4821.jpg", "url": "https://..." }]
  }
}
```

Errors: `403` wrong PIN, with attempts remaining in the message.
`429` after five wrong attempts from the same caller, locked for fifteen minutes.

### `GET /api/public/galleries/{slug}/photos`
Header: `X-Gallery-Token: <accessToken>`. Lets a refresh re-open the gallery
without asking for the PIN again. `403` without a valid token for **this** slug.

---

## Files (local storage only)

### `GET /api/files/{key}?expires={epochSeconds}&signature={hmac}`
Serves a photo. The signature is an HMAC-SHA256 over `key:expires` and is
verified in constant time. `403` if it is wrong or the link has expired.

When `STORAGE_TYPE=s3` this endpoint does not exist; `url` fields point at
presigned S3 URLs instead.

---

## Errors

Every failure uses the same envelope.

```json
{
  "timestamp": "2026-09-16T10:12:04Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Some fields need attention",
  "path": "/api/auth/register",
  "fieldErrors": { "password": "size must be between 8 and 72" }
}
```

| Status | When |
|--------|------|
| `400` | Validation failed, or the request does not make sense |
| `401` | No token, expired token, or wrong credentials |
| `403` | Signed in but not allowed, or a wrong PIN |
| `404` | Does not exist, or exists but is not yours |
| `409` | Duplicate email, or already on the event |
| `413` | Upload exceeds the server limit |
| `429` | Too many wrong PINs |
| `500` | Unexpected; logged server side, never leaked to the client |
