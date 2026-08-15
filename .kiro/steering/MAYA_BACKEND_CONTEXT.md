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
| posts | Instagram posts with embedded PostMetrics | FK → creators |
| comments | Comments on posts (denormalized creator_id for speed) | FK → posts, creators |
| hashtag_performance | Aggregated per creator+hashtag (upserted) | FK → creators |
| top_commenters | Aggregated per creator+username (upserted) | FK → creators |
| weekly_reports | Pre-computed weekly analytics snapshot (max 10 per creator) | FK → creators |
| scheduled_posts | Content calendar drafts | FK → creators |
| user_social_accounts | Links Maya users ↔ Phyllo accounts ↔ creators | FK → users, creators |
| weekly_goals | Weekly posting targets per user per creator | Unique: user_id + creator_id + week_start |
| post_metrics_snapshots | Time-series (not populated yet) | FK → posts |
| creator_insights_snapshots | Time-series (not populated yet) | FK → creators |

## Key Entity Details

**PostMetrics (@Embeddable in posts table):**
- likes, comments (always non-null)
- saves, shares, reach, impressions, plays (NULLABLE — null means data not returned from Phyllo, NOT zero)
- engagementRate, saveRate, shareRate (computed on insert)

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
- `GET /api/schedule/list?creatorId=X` — calendar entries
- `POST /api/schedule/create` — create draft
- `PUT /api/schedule/update/{id}` — edit draft
- `DELETE /api/schedule/delete/{id}` — remove draft
- `PUT /api/schedule/approve/{id}` — approve for publishing
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
- System prompt includes: creatorId, currentDate, clarification-before-action rules
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
    → syncProfile() (GET /v1/profiles?account_id=X — reads reputation.follower_count)
    → syncPosts() (GET /v1/social/contents?account_id=X&limit=100)
    → If empty → requestHistoricData (POST /v1/social/contents/fetch-historic) → retry up to 90sec
    → syncComments() per post (GET /v1/social/comments?account_id=X&content_id=Y — both required)
    → processCreatorAnalytics() → generates hashtag_performance + top_commenters + weekly_report
```

### Nightly sync (NightlySyncJob — @Scheduled 3am, or Cloud Scheduler trigger for prod):
```
For each CONNECTED non-demo account:
  → syncProfile (1 API call)
  → syncPostsWithMetricUpdate (1 API call — INSERT new + UPDATE existing metrics)
  → syncRecentComments (last 15 posts, 15 API calls)
  → processCreatorAnalytics() (hashtags + commenters + weekly report)
  → 200ms delay between API calls (Phyllo rate limit: 10 req/sec)
  → Error isolation per user (one failure doesn't stop others)
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

**Key Phyllo field mappings:**
- post.title → caption
- post.format → mediaType (IMAGE/VIDEO)
- post.type → mediaProductType (FEED/REELS)
- post.hashtags[] → comma-separated string
- engagement.reach_organic_count → reach (79% available, nullable)
- engagement.save_count → saves (63% available, nullable)
- profile: reputation.follower_count, reputation.following_count, reputation.content_count

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

## Environment Variables (for Cloud Run deployment)

```
OPENAI_API_KEY=sk-proj-...
JWT_SECRET=base64-encoded-secret
AIVEN_DB_PASSWORD=your_aiven_password
PHYLLO_BASE_URL=https://api.staging.getphyllo.com
PHYLLO_CLIENT_ID=024587e1-c1df-4493-b195-ef75eee887c8
PHYLLO_CLIENT_SECRET=8e05c8bf-6aae-4885-ac5b-eaee4164da99
PHYLLO_ENVIRONMENT=staging
```

## What's NOT Built Yet
- OTP-based registration + email verification
- Forgot password / reset password flow
- Cross-platform content repurposer (turn 1 post into IG/YT/TikTok variants)
- Cloud Scheduler HTTP trigger endpoint (for nightly sync on Cloud Run cold start)
- Post publishing via Phyllo Publish API
- Persistent chat history (DB-stored)
- Proactive AI (auto-suggestions, alerts, weekly briefs)
- Payment/subscription system
- Account lockout (brute force protection on login)
- Input validation on chat message length

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
│   └── userController.java         — auth (login/register)
├── Service/
│   ├── ai/
│   │   ├── MayaAiService.java      — LangChain4j interface (orchestrator)
│   │   ├── AnalyticsTools.java     — @Tool methods for data queries
│   │   ├── ScheduleTools.java      — @Tool methods for calendar (CRUD)
│   │   ├── CommentTools.java       — @Tool methods for comments
│   │   ├── TrendTools.java         — @Tool methods for trends/gaps
│   │   └── StrategyTools.java      — @Tool method for weekly plan generation
│   ├── analytics/
│   │   ├── AnalyticsService.java   — 16 time-series metrics (pure Java)
│   │   ├── SnapshotAnalyticsService.java — 8 snapshot metrics
│   │   ├── AccountHealthService.java — composite health score 0-100
│   │   ├── DashboardService.java   — assembles full 24-metric response
│   │   └── AnalyticsProcessingService.java — computes & stores derived tables + enforces 10-report limit
│   ├── strategy/
│   │   └── WeeklyStrategyService.java — THE HERO: analyzes posts → builds LLM prompt → generates 7-day plan
│   ├── phyllo/
│   │   ├── PhylloService.java      — Phyllo API communication (handles user_exists, historic fetch)
│   │   ├── PhylloSyncService.java  — maps Phyllo JSON → Maya entities + triggers analytics processing
│   │   └── NightlySyncJob.java     — @Scheduled nightly data refresh + metric updates
│   ├── instagram/
│   │   ├── DataSeedService.java    — seeds 4 demo creators on startup (skips if exists)
│   │   └── DummyGraphApiService.java — loads demo JSON files
│   ├── CreatorAccessService.java   — demo vs real access control (by username, not ID)
│   └── RateLimiterService.java     — in-memory rate limiter (per session/creator)
├── Entity/
│   ├── user.java, Role.java       — Maya auth
│   ├── UserSocialAccount.java     — linking table
│   ├── WeeklyGoal.java            — weekly posting targets
│   └── instagram/
│       ├── Creator.java, Post.java, PostMetrics.java, Comment.java
│       ├── HashtagPerformance.java, TopCommenter.java, WeeklyReport.java
│       └── ScheduledPost.java
├── Repository/
│   ├── userRepository.java, UserSocialAccountRepository.java, WeeklyGoalRepository.java
│   └── instagram/ (all JPA repos — each has deleteByCreatorId for hard-delete)
├── DTO/
│   ├── analytics/ (DashboardResponseDTO with inner classes, all metric DTOs)
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
