---
title: Data model
description: Explore Moo users, posts, follows, likes, hashtags, and refresh tokens.
---

# Data model

```mermaid
erDiagram
    USERS ||--o{ POSTS : authors
    USERS ||--o{ FOLLOWS : follows
    USERS ||--o{ FOLLOWS : is_followed
    USERS ||--o{ LIKES : gives
    POSTS ||--o{ LIKES : receives
    POSTS ||--o{ HASHTAG_POSTS : tagged
    HASHTAGS ||--o{ HASHTAG_POSTS : labels
    USERS ||--o| REFRESH_TOKENS : owns
    POSTS o|--o{ POSTS : replies_or_reposts
```

## Main records

- `Users` stores identity, profile, password hash, counters, and verification.
- `Posts` stores up to 240 characters, media URLs, counters, author, reply and
  original-post references, hashtags, and mentions.
- `Follow` records a unique follower/following pair.
- `Likes` enforces one like per user and post. `HashtagPosts` indexes its
  hashtag and post relationships but does not enforce unique pairs.
- `RefreshToken` stores one rotating refresh token per user.

Entities stay internal. Controllers expose the records in `model`, preventing
persistence details and password hashes from leaking into API responses.
