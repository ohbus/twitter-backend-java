---
title: Endpoints
description: Browse the current Moo version 1 endpoint groups and authorization requirements.
---

# Endpoints

`CSRF` means the `XSRF-TOKEN` cookie value must also be sent as the
`X-XSRF-TOKEN` header.

## Authentication

| Method | Path | Access | Purpose |
| --- | --- | --- | --- |
| GET | `/v1/csrf` | Public | Initialize a CSRF token and cookie |
| POST | `/v1/authenticate` | CSRF | Exchange credentials for JWT and refresh token |
| POST | `/v1/refresh` | CSRF | Rotate a refresh token and issue a JWT |
| POST | `/v1/logout` | JWT + CSRF | Revoke the current user refresh token |

## Users

| Method | Path | Access | Purpose |
| --- | --- | --- | --- |
| POST | `/v1/users` | CSRF | Register a user |
| PATCH | `/v1/users` | JWT + CSRF | Update the authenticated profile |
| GET | `/v1/users/{username-or-id}` | Public | Get a user; prefix usernames with `@` |
| PUT | `/v1/users/{userId}/follow` | JWT + CSRF | Follow a user |
| DELETE | `/v1/users/{userId}/follow` | JWT + CSRF | Unfollow a user |
| GET | `/v1/users/{userId}/followers` | Public | Page through followers |
| GET | `/v1/users/{userId}/followings` | Public | Page through followed users |

## Posts and discovery

| Method | Path | Access | Purpose |
| --- | --- | --- | --- |
| GET | `/v1/posts` | JWT | Page through posts |
| POST | `/v1/posts` | JWT + CSRF | Create a post, reply, or repost |
| GET | `/v1/posts/{postId}` | JWT | Get one post |
| DELETE | `/v1/posts/{postId}` | JWT + CSRF | Delete an owned post |
| PUT | `/v1/posts/{postId}/like` | JWT + CSRF | Like a post |
| DELETE | `/v1/posts/{postId}/like` | JWT + CSRF | Remove a like |
| GET | `/v1/posts/{postId}/replies` | JWT | Page through replies |
| GET | `/v1/hashtags` | JWT | List hashtags by recent activity |
| GET | `/v1/hashtags/{tag}/posts` | JWT | Page through tagged posts |
| GET | `/v1/search` | JWT | Search users and posts |
| GET | `/v1/search/posts` | JWT | Search posts |
| GET | `/v1/search/users` | JWT | Search users |

The [generated reference](reference.md) contains parameters and schemas derived
from the running Spring application.
