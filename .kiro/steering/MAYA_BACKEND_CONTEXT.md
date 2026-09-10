---
inclusion: auto
---

# MAYA Manage — Backend Project Context

## Overview
AI-powered creator growth platform. Connects to creators' social accounts via Phyllo, analyzes content performance, and generates personalized weekly content strategies using agentic LLM architecture. Spring Boot 3.4.3, Java 17, deployed on Google Cloud Run with Aiven PostgreSQL.

## Product Positioning
MAYA is an AI Creator Operating System — not an analytics dashboard. The core value prop: "Connect your creator accounts. MAYA learns your content and builds your next week of posts automatically." Content-first, analytics-light. The main flow: Connect → Sync → Generate Weekly Strategy → Review → Save to Calendar → Publish Consistently.

## Tech Stack
- Spring Boot 3.4.3 + Spring Security (JWT HS256)
- Spring Data JPA + Hibernate + PostgreSQL (local: localhost:5432/postgres, prod: Aiven)
- LangChain4j 0.35.0 (OpenAI GPT-4o-mini, streaming + non-streaming)
- Phyllo API (social data aggregation — staging: api.staging.getphyllo.com)
- Maven build, Docker + Cloud Run deployment
- JDK: Amazon Corretto 17 at C:\Users\deepa\.jdks\corretto-17.0.14

## Architecture Pattern
```
Client → Controllers → AI Orchestrator (LangChain4j AiService) → @Tool methods → Services → Repositories → PostgreSQL
```

Non-negotiable rules:
- Orchestrator NEVER touches DB directly — only through @Tool methods
- AnalyticsService = pure Java math, NEVER calls LLM
- New feature = new @Tool class registered in MayaAiConfig, nothing else changes
- Null preservation for Phyllo fields (null ≠ zero)

## Database Schema (Tables)

| Table | Purpose | Key relationships |
|-------|---------|-------------------|
| users | Maya auth (email, password, role) | Standalone |
| creators | One per social media account (Instagram/YouTube/etc) | Root of analytics data |
| posts | Content items with embedded PostMetrics (platform-agnostic) | FK → creators |
| comments | Comments on posts (denormalized creator_id for speed) | FK → posts, creators |
| hashtag_performance | Aggregated per creator+hashtag (upserted) | FK → creators |
| top_commenters | Aggregated per creator+username (upserted) | FK → creators |
| weekly_reports | Pre-computed weekly analytics snapshot (max 10 per creator) | FK → creators (+ top_post_id/worst_post_id → posts) |
| scheduled_posts | Content calendar drafts | FK → creators |
| user_social_accounts | Links Maya users ↔ Phyllo accounts ↔ creators | FK → users, creators |
| weekly_goals | Weekly posting targets per user per creator | Unique: user_id + creator_id + week_start |
| password_reset_tokens | Single-use hashed tokens for forgot-password flow | Standalone (email-keyed) |
| otp_verifications | Pending registration OTPs (hashed) | Standalone (email-keyed) |
| post_metrics_snapshots | Time-series (not populated yet) | FK → posts |
| creator_insights_snapshots | Time-series (not populated yet) | FK → creators |

### Platform-agnostic schema refactor (Phyllo-aligned vocabulary)
The `posts`, `comments`, and `creators` schemas were migrated from Instagram-specific names to Phyllo's field vocabulary so one schema serves all platforms. `caption` is the one deliberate exception (kept as a Maya-domain concept; mapped from Phyllo `description`/`title`).

- **Post** field renames: `instagram_id` → `phyllo_id`, `media_type` → `format`, `permalink` → `url`, `media_product_type` → `type`. New columns: `external_id`, `platform`, `title`, `duration`, `mentions`, `visibility`, `persistent_thumbnail_url`, `platform_profile_id`, `platform_profile_name`, `is_owned_by_platform_user`.
- **Comment** field renames: `instagram_id` → `phyllo_id`. New columns: `external_id`, `commenter_id`, `commenter_profile_url`, `commenter_display_name`, `content_url`, `content_published_at`.
- **Creator**: `instagram_id` → `phyllo_account_id`, new `platform` column.
- **ScheduledPost**: `published_instagram_id` → `published_external_id`.

## Key Entity Details

**PostMetrics (@Embeddable in posts table — Phyllo-aligned field names):**
- likeCount, commentCount (always non-null)
- saveCount, shareCount, repostCount, dislikeCount, reachOrganicCount, impressionOrganicCount, viewCount (Long), watchTimeInHours, avgWatchTimeInSec, clickCount, replayCount (NULLABLE — null means data not returned, NOT zero)
- engagementRate, saveRate, shareRate (computed on insert)
- Note: `viewCount` moved from Post into PostMetrics; old `plays` field removed (use viewCount). Getter renames cascade everywhere: getLikes→getLikeCount, getComments→getCommentCount, getSaves→getSaveCount, getShares→getShareCount, getReach→getReachOrganicCount, getImpressions→getImpressionOrganicCount.

**UserSocialAccount (the bridge):**
- user_id → users.id (Maya auth)
- creator_id → creators.id (analytics data)
- phyllo_user_id, phyllo_account_id (Phyllo identifiers)
- platform (INSTAGRAM, YOUTUBE, FACEBOOK, etc.)
- status (CONNECTED, DISCONNECTED)

**WeeklyGoal:**
- user_id, creator_id, week_start (unique constraint)
- target (1-30, default 5)

## Controllers & Endpoints

### Public (no auth):
- `GET /api/analytics/creators` — list demo creators (identified by username, not ID)
- `GET /api/analytics/dashboard/{creatorId}` — full 24-metric dashboard (computed on-demand)
- `GET /api/analytics/weekly-reports/{creatorId}` — historical reports (max 10)
- `POST /api/chat/stream` — AI chat with SSE streaming (body: {message, creatorId, sessionId})
- `GET /api/schedule/list?creatorId=X` — board/calendar entries (posts AND tasks; each row has `itemType`)
- `POST /api/schedule/create` — create a POST (default) or a TASK (`itemType: "TASK"`)
- `PUT /api/schedule/update/{id}` — edit/reschedule; also moves a task between columns via `taskStatus`
- `DELETE /api/schedule/delete/{id}` — remove item
- `PUT /api/schedule/approve/{id}` — approve a POST for publishing (posts only)
- `PUT /api/schedule/publish/{id}` — mark an APPROVED post published (posts only)
- `POST /api/strategy/generate` — generate 7-day content plan (rate limited: 10/hour)
- `POST /api/strategy/generate-and-save` — generate + auto-save to calendar (rate limited: 10/hour)

### Authenticated (Bearer JWT required):
- `POST /api/phyllo/connect` — initiate Phyllo Connect (returns sdkToken)
- `POST /api/phyllo/account-connected` — store connected account + trigger sync
- `GET /api/phyllo/accounts?userId=X` — list connected accounts (with profile data: followers, picture, etc.)
- `GET /api/phyllo/disconnected?userId=X` — list reconnectable accounts
- `PUT /api/phyllo/reconnect/{creatorId}` — instant reconnect (no SDK)
- `DELETE /api/phyllo/disconnect/{creatorId}` — soft disconnect (data preserved)
- `DELETE /api/phyllo/delete-account/{creatorId}` — PERMANENT hard delete (all data erased)
- `GET /api/phyllo/sync-status/{creatorId}` — poll sync progress
- `GET /api/goals/current?creatorId=X` — get current week's posting goal
- `POST /api/goals/set` — set/update weekly goal (body: {creatorId, target})
- `GET /api/posts/activity?creatorId=X` — posting activity (streak dates, thisWeekCount, totalPosts)
- `POST /api/content/**` — content generation (existing, deployed backend)

### Auth — Registration & Password (public):
- `POST /auth/send-otp` — start registration, email 6-digit OTP (rate limited: 3/10min)
- `POST /auth/verify-otp` — verify OTP → create user → return JWT (auto-login)
- `POST /auth/login` — email/password login → JWT
- `POST /auth/forgot-password` — email a reset link if account exists (rate limited: 3/10min; always responds "SENT" — no email-existence leak)
- `POST /auth/reset-password` — validate reset token + set new password (body: {token, newPassword})

### Access Control:
- Demo creators (identified by username: fitlife_by_meera, techwithriya, the.monkey.who.left.hc.verma, travelwithkartik) → always public regardless of ID
- Real creators (connected via Phyllo) → require JWT + ownership check via CreatorAccessService
- One social media account can only be CONNECTED to one Maya user at a time
- If User A disconnects, User B can then connect that same account

### Rate Limiting:
- AI Chat: 30 requests/minute per sessionId
- Strategy Generate: 10 requests/hour per creatorId
- Strategy Generate-and-Save: 10 requests/hour per creatorId (shared bucket with generate)
- Returns SSE `[ERROR]` for chat, HTTP 429 for strategy

## AI Chat System

**MayaAiService** (LangChain4j interface — no @AiService annotation, built manually in MayaAiConfig):
- Streaming via TokenStream → SSE
- System prompt includes: creatorId, currentDate, **platformContext**, clarification-before-action rules
- `chat(sessionId, userMessage, creatorId, currentDate, platformContext)` — platformContext is a short per-platform availability block built by `DashboardService.buildPlatformAiContext(creatorId)` and passed in by AgentChatController
- Platform awareness: if a user asks for a metric UNAVAILABLE on their platform (e.g. sentiment on Facebook, reach on YouTube), the AI explains it isn't available and offers an alternative — never fabricates a number
- @MemoryId sessionId for per-session isolation (frontend generates UUID)
- Chat memory: last 20 messages per session (in-memory, not persistent)
- System prompt rules: ask before destructive actions, never guess IDs, always verify with user

**Registered @Tool classes (in MayaAiConfig):**
1. AnalyticsTools — health score, engagement metrics, best/worst posts, hashtags, timing, fans, sentiment, topics, content strategy, profile
2. ScheduleTools — suggestSlots, createDraft, listScheduledPosts, updateDraft, deleteDraft
3. CommentTools — getUnansweredQuestions, getCommentContext, getHighEngagementComments, getCommentStats
4. TrendTools — detectContentGap, detectViralPatterns, suggestContentIdeas, analyzeHashtagStrategy
5. StrategyTools — generateWeeklyPlan (calls WeeklyStrategyService)

## AI Weekly Strategy Generator (HERO FEATURE)

**WeeklyStrategyService** — the core "wow" feature:
- Fetches last 30 posts for the creator
- Analyzes: top captions (style), last 7 posts (avoid repetition), viral patterns (high share), audience questions (from comments), content gaps (comment words vs caption words)
- Builds rich LLM prompt with all signals
- LLM generates 7-day plan starting from TOMORROW (not next Monday)
- Each day: content_pillar, post_idea, caption, hook, format, hashtags, best_time, cta, repurpose_note
- Parses JSON response into WeeklyPlanDTO
- Can auto-save all 7 days to calendar as PENDING drafts

**Content signals used for personalization:**
- Top 5 captions by ER (style reference)
- Last 7 posts (avoid repetition)
- Top 3 viral posts by share rate
- Top 5 audience questions from comments
- Content gaps (topics audience asks about but creator hasn't posted)
- Best format (IMAGE vs VIDEO by ER)
- Best posting day/hour
- Top hashtags
- Follower count / niche

## Data Flow

### Demo data (on app startup):
```
DataSeedService (Order 1) → loads 4 Phyllo JSON files → seeds creators/posts/comments
AnalyticsProcessingService (Order 2) → computes hashtag_performance, top_commenters, weekly_reports
Both skip if data already exists (quick exit check)
```

### Real user flow:
```
User connects via Phyllo SDK → POST /api/phyllo/account-connected
  → Creates Creator + UserSocialAccount link
  → PhylloSyncService.syncAccount() runs @Async:
    → syncProfile() (GET /v1/profiles?account_id=X — reads reputation.follower_count, falls back to reputation.subscriber_count for YouTube/Twitch/LinkedIn)
    → syncPosts() (GET /v1/social/contents?account_id=X&limit=100)
    → If empty → requestHistoricData (POST /v1/social/contents/fetch-historic) → wait 5 min → retry → schedule 30-min retry if still empty (marks this the "slow path")
    → syncComments() per post (GET /v1/social/comments?account_id=X&content_id=Y — both required)
    → processCreatorAnalytics() → generates hashtag_performance + top_commenters + weekly_report
    → Sync-complete email: sent ONLY on the slow (historic) path or the 30-min retry completion — fast syncs (~2 min, user still on page) send nothing. Recipient resolved via UserSocialAccount.userId → users.email (NOT creator.email, which Phyllo rarely populates).
```

### Nightly sync (NightlySyncJob — @Scheduled 3am, or Cloud Scheduler trigger for prod):
```
For each CONNECTED non-demo account:
  → syncProfile (1 API call)
  → clearPostReferencesByCreatorId (nulls weekly_reports.top_post_id/worst_post_id BEFORE deleting posts — prevents FK constraint violation)
  → DELETE old posts + comments → batch INSERT fresh
  → syncRecentComments (last 15 posts, 15 API calls)
  → processCreatorAnalytics() (hashtags + commenters + weekly report)
  → 200ms delay between API calls (Phyllo rate limit: 10 req/sec)
  → Error isolation per user (one failure doesn't stop others)
  → NOTE: this delete-then-reinsert flow must clear weekly_reports FK refs first (top_post_id/worst_post_id). Same guard applied in DataSeedService re-seed path.
```

### Weekly report lifecycle:
- Generated on: first connection, reconnect, nightly job (if none for current week)
- Max 10 per creator per social account (oldest trimmed automatically via enforceWeeklyReportLimit)
- Unique constraint: (creator_id, week_start_date)
- NOT used by dashboard endpoint (dashboard computes on-demand from raw data)
- Exposed via: GET /api/analytics/weekly-reports/{creatorId}

### Account management:
- Disconnect (soft): sets status=DISCONNECTED, creator.isActive=false, data preserved
- Reconnect: re-activates instantly (no Phyllo SDK), triggers fresh sync
- Delete (hard): permanently erases all data (posts, comments, analytics, reports, scheduled posts, creator entity)
- One social account → one Maya user at a time (409 if another user tries to connect)
- If disconnected by User A → User B can connect it (transfer ownership)

## Phyllo API Integration

**Credentials:** Basic auth (base64 of client_id:client_secret) — stored as env vars
- Base URL: ${PHYLLO_BASE_URL}
- Client ID: ${PHYLLO_CLIENT_ID}
- Client Secret: ${PHYLLO_CLIENT_SECRET}
- Environment: ${PHYLLO_ENVIRONMENT}

**Endpoints used:**
- POST /v1/users — create Phyllo user (handles "user_exists_with_external_id" by fetching existing)
- POST /v1/sdk-tokens — generate frontend SDK token
- GET /v1/accounts/{id} — account details after connection
- GET /v1/profiles?account_id=X — profile (reputation.follower_count, reputation.following_count, reputation.content_count)
- GET /v1/social/contents?account_id=X&limit=100 — posts
- POST /v1/social/contents/fetch-historic — request data older than 90 days (body: {account_id, from_date})
- GET /v1/social/comments?account_id=X&content_id=Y&limit=100 — comments (BOTH account_id AND content_id required)

**Key Phyllo field mappings (post-refactor — entity names now mirror Phyllo):**
- post.description (fallback post.title) → caption; post.title → title
- post.external_id → externalId; creator.platform → post.platform
- post.format → format (IMAGE/VIDEO/AUDIO/TEXT); post.type → type (FEED/REELS/etc)
- post.url → url; post.hashtags[] → comma-separated string; post.mentions[] → comma-separated string
- engagement.like_count → likeCount, comment_count → commentCount
- engagement.reach_organic_count → reachOrganicCount (nullable), impression_organic_count → impressionOrganicCount
- engagement.save_count → saveCount, share_count → shareCount, repost_count → repostCount, dislike_count → dislikeCount
- engagement.view_count → viewCount (Long), watch_time_in_hours → watchTimeInHours, avg_watch_time_in_sec → avgWatchTimeInSec, click_count → clickCount
- comment.external_id/commenter_id/commenter_profile_url/commenter_display_name + content.url/published_at
- profile: reputation.follower_count (fallback reputation.subscriber_count) → followerCount, following_count, content_count

## Security Config (WebSecurityConfig)

- CORS: localhost:5173, mayamanage.com, Firebase hosting URLs
- Public: /auth/login, /auth/registerUser, /api/analytics/**, /api/chat/**, /api/schedule/**, /api/strategy/**, /contact/**
- Authenticated: /auth/**, /api/content/**, /api/phyllo/**, /api/goals/**, /api/posts/**
- JWT decoder: HS256 with base64-encoded secret from env var
- Rate limiting: in-memory per session/creator (RateLimiterService)

## Multi-Platform Design

One Maya user can connect multiple social accounts:
- Each platform account = separate Creator entity = separate creatorId
- All analytics, chat, schedule, strategy, goals scoped by creatorId
- Frontend switches between accounts via account selector
- Disconnect/reconnect/delete per creatorId (independent operations)
- One social account can only be actively connected to ONE Maya user
- /api/phyllo/accounts returns full profile data (followers, picture, niche, verified status)

## Weekly Board (Jira-style) — Posts + Tasks in one table

The `scheduled_posts` table backs a weekly board that holds two item kinds, distinguished by an `itemType` enum (single-table inheritance — chosen over a separate table so the board is one query / one update endpoint / one response shape):

- **POST** (default): a content draft. Uses the `approvalStatus` lifecycle (PENDING → APPROVED → PUBLISHED / REJECTED / FAILED). Has caption, hashtags, mediaType, mediaUrl.
- **TASK**: a to-do around content. Uses a separate `taskStatus` lifecycle (TODO → IN_PROGRESS → DONE). `caption` doubles as the task title; mediaType is stored as "NONE"; hashtags/mediaUrl are null.

Key modeling decisions:
- `itemType` and `taskStatus` are separate columns from `approvalStatus`, so the post-lifecycle and task-lifecycle enums never collide (POST rows have null taskStatus; TASK rows leave approvalStatus unused).
- `scheduledFor` stays REQUIRED for both — every board item sits on a day.
- `mediaType` stays NOT NULL (tasks carry "NONE") — chosen to avoid a manual DB constraint change; `ddl-auto=update` auto-adds the new `item_type`/`task_status` columns on startup.
- Existing rows default to `itemType = POST` (backward compatible). AI ScheduleTools + StrategyController create posts via `new ScheduledPost()` + setters, so they keep working unchanged (itemType defaults to POST).
- `toResponse` uses LinkedHashMap (not Map.of) because several fields can be null (taskStatus for posts, etc.).

Endpoints (same for both kinds; type-aware internally):
- create: POST omits itemType (→POST) or sends `itemType:"TASK"` + optional `taskStatus`
- update: reschedule (`scheduledFor`) works for both; `taskStatus` moves a task between columns
- approve/publish: posts only

## Platform-Aware Analytics (Instagram / Facebook / YouTube)

Analytics are a HYBRID: a shared core (reused math) + a per-platform hero block. Focus platforms are Instagram, Facebook, YouTube; everything else falls back to Instagram-default behavior. Data availability drives everything (from Phyllo's engagement schema).

**`Platform` enum** (`com.MAYA.MAYA.Enums.Platform`): INSTAGRAM, FACEBOOK, YOUTUBE, OTHER + `normalize(String)` maps raw Phyllo/creator platform strings (case-insensitive, handles IG Direct/Lite, Facebook Commerce, etc.) to canonical values. Unknown → OTHER.

**Same DTO, different fills.** The dashboard response shape is unchanged; only *which* numbers fill and *how* they're computed varies:
- Engagement denominator per platform: Instagram/Facebook = reach; YouTube = views (no reach exists on YT).
- `DashboardResponseDTO` gained 3 additive fields: `platform` (String), `platformInsights` (List<PlatformInsightCardDTO>), `unavailableMetrics` (List<UnavailableMetricDTO>).
  - `PlatformInsightCardDTO { key, label, value (nullable), unit, delta (always null for now), description }` — a generic, self-describing card list; frontend renders it blindly. Adding a new platform = backend returns a different list, no DTO/frontend change.
  - `UnavailableMetricDTO { key, label, reason }` — what the platform can't provide; drives UI hide + feeds the AI system message. `key` matches `rateCards[].metricName` exactly for cross-referencing.

**Platform hero cards (in platformInsights):**
- Instagram → empty (its hero metrics — save rate, share rate, reach efficiency, play-through — already live in core `rateCards`)
- Facebook → `fb_reach_efficiency` (reach/impressions), `fb_watch_time` (hours), `fb_view_rate` (views/reach), `fb_click_signal` (clicks)
- YouTube → `yt_view_engagement` ((likes+comments)/views), `yt_like_to_view`, `yt_approval_rate` (likes/(likes+dislikes) — YT-only), `yt_views_per_sub`

**Core sections nulled per platform:** Facebook has no comment bodies via Phyllo, so `sentimentBreakdown`, `questionsVsStatements`, `questionsInsight`, `mostLikedComments`, `topCommenters`, `commonWords` come back null for Facebook. YouTube keeps all comment sections (it has comments). `rateCards` is NOT filtered — unsupported metrics return `currentValue: null` and their keys appear in `unavailableMetrics`.

**Platform-aware health score** (`AccountHealthService`): weights are chosen per platform and re-normalized to 100% so a platform is never penalized for a component it can't provide. `componentScores` map only contains keys relevant to the platform (frontend must iterate keys, not assume all 5):
- Instagram/Other: engagement .30, consistency .20, reach_distribution .20, sentiment .15, content_value .15
- Facebook: engagement .40, consistency .25, reach_distribution .35 (no sentiment/saves)
- YouTube: engagement .55, consistency .30, sentiment .15 (no reach/saves)

## Environment Variables (for Cloud Run deployment)

```
OPENAI_API_KEY=sk-proj-...
JWT_SECRET=base64-encoded-secret
AIVEN_DB_PASSWORD=your_aiven_password
PHYLLO_BASE_URL=https://api.staging.getphyllo.com
PHYLLO_CLIENT_ID=024587e1-c1df-4493-b195-ef75eee887c8
PHYLLO_CLIENT_SECRET=8e05c8bf-6aae-4885-ac5b-eaee4164da99
PHYLLO_ENVIRONMENT=staging
FRONTEND_BASE_URL=https://mayamanage.com   # used to build password reset links (defaults to this if unset)
```

## What's Built (recent additions)
- OTP-based registration + email verification (send-otp / verify-otp)
- Forgot password / reset password flow (token-based reset link — /auth/forgot-password + /auth/reset-password, 30-min single-use hashed tokens)
- Platform-aware analytics + health score for Instagram/Facebook/YouTube
- Platform-agnostic Phyllo-aligned entity schema

## What's NOT Built Yet
- Cross-platform content repurposer (turn 1 post into IG/YT/TikTok variants)
- Cloud Scheduler HTTP trigger endpoint (for nightly sync on Cloud Run cold start) — currently use a public GET (e.g. /api/analytics/creators) only to wake the instance
- Post publishing via Phyllo Publish API
- Persistent chat history (DB-stored)
- Proactive AI (auto-suggestions, alerts, weekly briefs)
- Payment/subscription system
- Account lockout (brute force protection on login)
- Input validation on chat message length
- Own social-connection SDK (planned Phyllo replacement — same DB, swap the 4 Phyllo-coupled files: PhylloService, PhylloSyncService, NightlySyncJob, PhylloController)
- Queue/worker or webhook-based sync (current sync uses blocking @Async threads with sleeps — fragile on Cloud Run scale-down; deferred to SDK migration)

## File Structure (key files)

```
src/main/java/com/MAYA/MAYA/
├── Config/
│   ├── MayaAiConfig.java          — wires LangChain4j orchestrator + all 5 tool classes
│   └── langChainConfig.java       — embedding store, chat listener
├── Controller/
│   ├── AnalyticsDashboardController.java — dashboard + weekly reports API
│   ├── AgentChatController.java    — SSE streaming chat (rate limited: 30/min)
│   ├── ScheduleController.java     — calendar CRUD
│   ├── StrategyController.java     — weekly plan generation (rate limited: 10/hour)
│   ├── PhylloController.java       — connect/disconnect/reconnect/delete/sync-status
│   ├── WeeklyGoalController.java   — get/set weekly posting goals
│   ├── PostActivityController.java — streak + activity data from real posts
│   └── userController.java         — auth: login, OTP register (send-otp/verify-otp), forgot/reset password (@RequestMapping /auth)
├── Service/
│   ├── ai/
│   │   ├── MayaAiService.java      — LangChain4j interface (orchestrator)
│   │   ├── AnalyticsTools.java     — @Tool methods for data queries
│   │   ├── ScheduleTools.java      — @Tool methods for calendar (CRUD)
│   │   ├── CommentTools.java       — @Tool methods for comments
│   │   ├── TrendTools.java         — @Tool methods for trends/gaps
│   │   └── StrategyTools.java      — @Tool method for weekly plan generation
│   ├── analytics/
│   │   ├── AnalyticsService.java   — time-series metrics (pure Java) + platform-specific FB/YT hero metric methods (view engagement, like-to-view, approval rate, views/sub, reach efficiency, watch time, view rate, clicks)
│   │   ├── SnapshotAnalyticsService.java — 8 snapshot metrics
│   │   ├── AccountHealthService.java — platform-aware composite health score 0-100 (per-platform weights, re-normalized)
│   │   ├── DashboardService.java   — assembles full response + platformInsights + unavailableMetrics + buildPlatformAiContext() for the chat system message
│   │   └── AnalyticsProcessingService.java — computes & stores derived tables + enforces 10-report limit
│   ├── strategy/
│   │   └── WeeklyStrategyService.java — THE HERO: analyzes posts → builds LLM prompt → generates 7-day plan
│   ├── phyllo/  (the ONLY Phyllo-coupled package — swap target for own SDK)
│   │   ├── PhylloService.java      — Phyllo API communication (handles user_exists, historic fetch)
│   │   ├── PhylloSyncService.java  — maps Phyllo JSON → Maya entities, sets platform, slow-path email, subscriber_count fallback, triggers analytics
│   │   └── NightlySyncJob.java     — @Scheduled nightly refresh; clears weekly_reports post-FK refs before delete
│   ├── instagram/
│   │   ├── DataSeedService.java    — seeds 4 demo creators on startup (skips if exists)
│   │   └── DummyGraphApiService.java — loads demo JSON files
│   ├── CreatorAccessService.java   — demo vs real access control (by username, not ID)
│   ├── OtpService.java             — registration OTP generate/verify (hashed, rate limited)
│   ├── PasswordResetService.java   — forgot-password: token generate/email + reset (hashed, single-use)
│   ├── EmailService.java           — OTP, sync-complete, and password-reset emails (ZeptoMail SMTP)
│   └── RateLimiterService.java     — in-memory rate limiter (per session/creator)
├── Enums/
│   └── Platform.java              — canonical platform values + normalize() (used by sync + analytics)
├── Entity/
│   ├── user.java, Role.java       — Maya auth
│   ├── UserSocialAccount.java     — linking table
│   ├── WeeklyGoal.java            — weekly posting targets
│   ├── OtpVerification.java       — pending registration OTPs (hashed)
│   ├── PasswordResetToken.java    — single-use hashed reset tokens (forgot-password)
│   └── instagram/
│       ├── Creator.java, Post.java, PostMetrics.java, Comment.java (Phyllo-aligned field names)
│       ├── HashtagPerformance.java, TopCommenter.java, WeeklyReport.java
│       └── ScheduledPost.java (Jira-style board item: POST or TASK via itemType enum; separate taskStatus for tasks)
├── Repository/
│   ├── userRepository.java, UserSocialAccountRepository.java, WeeklyGoalRepository.java
│   ├── OtpVerificationRepository.java, PasswordResetTokenRepository.java
│   └── instagram/ (all JPA repos — each has deleteByCreatorId for hard-delete)
├── DTO/
│   ├── analytics/ (DashboardResponseDTO with platform/platformInsights/unavailableMetrics + inner classes, all metric DTOs)
│   └── strategy/ (WeeklyPlanDTO with DayPlanDTO)
├── Security/ (WebSecurityConfig, jwtTokenProvider, CustomUserDetailsService)
└── Exception/ (CreatorNotFoundException, GlobalExceptionHandler, ApiErrorResponse)

src/main/resources/
├── application.properties (all secrets as ${ENV_VARS}, DB config, LangChain4j, prompts)
├── profile_data/ (4 Phyllo JSON files for demo posts)
└── profiles_commentList/ (4 Phyllo JSON files for demo comments)
```

## Running Locally
- Set JAVA_HOME to corretto-17 or run from IntelliJ (has its own JDK)
- Local PostgreSQL: localhost:5432/postgres (user: postgres, pass: 12A12b)
- Set env vars in IDE run config: OPENAI_API_KEY, JWT_SECRET, PHYLLO_* (or hardcode temporarily for dev)
- On first run: seeds demo data + computes analytics (~30-60 sec)
- On subsequent runs: skips seeding (quick exit if 4+ creators and comments exist)
- Frontend runs on localhost:5173 (React 19 + Vite 6 + TailwindCSS)

## Deployment (Cloud Run)
- Docker build from existing Dockerfile + cloudbuild.yaml
- Set all env vars in Cloud Run console
- ddl-auto=update (never create in prod)
- @Scheduled nightly job won't fire on Cloud Run (cold start kills it) — needs Cloud Scheduler HTTP trigger (not built yet)
- Min instances: 0 (scales to zero, saves cost) — means cold starts on first request
