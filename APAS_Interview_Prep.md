# APAS Interview Prep — 10 Questions, Grounded in Your Actual Code

> All answers are traced directly to [ProjectService.java](file:///d:/downlodes/JON%20-%20Copy/pas/src/main/java/com/project/pas/service/ProjectService.java), [SecurityConfig.java](file:///d:/downlodes/JON%20-%20Copy/pas/src/main/java/com/project/pas/config/SecurityConfig.java), [Project.java](file:///d:/downlodes/JON%20-%20Copy/pas/src/main/java/com/project/pas/model/Project.java), [ApprovalHistory.java](file:///d:/downlodes/JON%20-%20Copy/pas/src/main/java/com/project/pas/model/ApprovalHistory.java), [FacultyController.java](file:///d:/downlodes/JON%20-%20Copy/pas/src/main/java/com/project/pas/controller/FacultyController.java), [GuideAssignment.java](file:///d:/downlodes/JON%20-%20Copy/pas/src/main/java/com/project/pas/model/GuideAssignment.java), and [ProjectStatus.java](file:///d:/downlodes/JON%20-%20Copy/pas/src/main/java/com/project/pas/model/ProjectStatus.java) in your repo.

---

## Q1 — Draw the architecture. Name every layer and what lives in it.

```
┌──────────────────────────────────────────────────────┐
│  BROWSER  (Chrome, Postman)                          │
│  HTML forms + Bootstrap 5 UI rendered by Thymeleaf   │
└──────────────────────┬───────────────────────────────┘
                       │  HTTP (GET / POST)
                       ▼
┌──────────────────────────────────────────────────────┐
│  PRESENTATION LAYER  — Spring MVC Controllers        │
│  StudentController  FacultyController  HodController │
│  AdminController    AuthController    ProfileController│
│  • Maps URL → method  • Reads @PathVariable/@Param   │
│  • Calls Service      • Populates Thymeleaf Model     │
│  • Returns view name  • Writes flash attributes       │
└──────────────────────┬───────────────────────────────┘
                       │  method calls (Spring beans)
                       ▼
┌──────────────────────────────────────────────────────┐
│  SECURITY LAYER  (cuts across all layers)            │
│  SecurityConfig → SecurityFilterChain                │
│  • URL-level: /faculty/** → ROLE_FACULTY only        │
│  • DaoAuthenticationProvider + BCrypt                │
│  • Session-based; CSRF enabled on web forms          │
└──────────────────────┬───────────────────────────────┘
                       │
                       ▼
┌──────────────────────────────────────────────────────┐
│  SERVICE LAYER  — Business Logic & State Machine     │
│  ProjectService       GuideSelectionService          │
│  ProjectTopicService  EmailService                   │
│  FileStorageService   PasswordResetService           │
│  UserService          BranchService                  │
│  • ALL status transitions happen here                │
│  • ALL branch + owner checks happen here             │
│  • @Transactional wraps every mutation               │
└──────────────────────┬───────────────────────────────┘
                       │  Spring Data JPA interfaces
                       ▼
┌──────────────────────────────────────────────────────┐
│  REPOSITORY LAYER  — Spring Data JPA                 │
│  ProjectRepository    ApprovalHistoryRepository      │
│  UserRepository       GuideAssignmentRepository      │
│  ProjectTeamMemberRepository  ProjectTopicRepository │
│  SearchSpecifications (JPA Criteria API)             │
│  • Derived queries: findByBranchAndStatusIn(...)     │
│  • Specifications for pageable search/sort           │
└──────────────────────┬───────────────────────────────┘
                       │  Hibernate ORM / JDBC
                       ▼
┌──────────────────────────────────────────────────────┐
│  PERSISTENCE LAYER                                   │
│  MySQL  (via Hibernate / Spring Data JPA)            │
│  Tables: users, projects, approval_history,          │
│  project_team_members, project_topics,               │
│  guide_assignments, guide_assignment_history,        │
│  guide_selection_forms, branches,                    │
│  password_reset_tokens                               │
└──────────────────────────────────────────────────────┘
```

**Cross-cutting concerns:**
- `GuideSelectionScheduler` — `@Scheduled` cron job at the Service layer that runs `autoAssignAll()` when forms expire
- `GlobalControllerAdvice` — exception handling across all controllers, surfaces service exceptions as Thymeleaf flash messages
- `DataInitializer` — seeds initial data on startup
- `FileStorageService` — disk I/O via `java.nio`, files stored under `/uploads/{type}/{projectId}/`

---

## Q2 — Draw the schema. How many tables, what are the foreign keys, what's in the approvals table?

### 10 Tables

| Table | PK | Notable FKs |
|---|---|---|
| `users` | [id](file:///d:/downlodes/JON%20-%20Copy/pas/src/main/java/com/project/pas/model/Project.java#371-374) | `branch_id → branches.id` |
| `branches` | [id](file:///d:/downlodes/JON%20-%20Copy/pas/src/main/java/com/project/pas/model/Project.java#371-374) | — |
| `projects` | [id](file:///d:/downlodes/JON%20-%20Copy/pas/src/main/java/com/project/pas/model/Project.java#371-374) | `student_id → users.id`, `branch_id → branches.id`, `topic_id → project_topics.id`, `faculty_guide_id → users.id` |
| `project_topics` | [id](file:///d:/downlodes/JON%20-%20Copy/pas/src/main/java/com/project/pas/model/Project.java#371-374) | `branch_id → branches.id`, `created_by → users.id` |
| `project_team_members` | [id](file:///d:/downlodes/JON%20-%20Copy/pas/src/main/java/com/project/pas/model/Project.java#371-374) | `project_id → projects.id`, `member_id → users.id`; **UNIQUE(project_id, member_id)** |
| `approval_history` | [id](file:///d:/downlodes/JON%20-%20Copy/pas/src/main/java/com/project/pas/model/Project.java#371-374) | `project_id → projects.id`, `performed_by → users.id` |
| `guide_assignments` | [id](file:///d:/downlodes/JON%20-%20Copy/pas/src/main/java/com/project/pas/model/Project.java#371-374) | `student_id → users.id` **(UNIQUE)**, `faculty_id → users.id`, `branch_id → branches.id`, `selection_form_id → guide_selection_forms.id` |
| `guide_assignment_history` | [id](file:///d:/downlodes/JON%20-%20Copy/pas/src/main/java/com/project/pas/model/Project.java#371-374) | `student_id`, `faculty_id`, `branch_id` (all → users/branches) |
| `guide_selection_forms` | [id](file:///d:/downlodes/JON%20-%20Copy/pas/src/main/java/com/project/pas/model/Project.java#371-374) | `branch_id → branches.id`, `created_by → users.id` |
| `password_reset_tokens` | [id](file:///d:/downlodes/JON%20-%20Copy/pas/src/main/java/com/project/pas/model/Project.java#371-374) | `user_id → users.id` |

### The `approval_history` table — full column inventory

```java
// From ApprovalHistory.java
id                       BIGINT PK
project_id               FK → projects.id  (NOT NULL)
performed_by             FK → users.id     (NOT NULL)
user_role                VARCHAR (enum: STUDENT/FACULTY/HOD)
action                   VARCHAR  e.g. "Approved project proposal"
previous_status          VARCHAR (enum ProjectStatus, nullable)
new_status               VARCHAR (enum ProjectStatus, NOT NULL)
comments                 TEXT (optional free-text)
rejection_reason         TEXT (mandatory on rejections)
previous_requested_team_size   INT (nullable)
new_requested_team_size        INT (nullable)
previous_min_team_size         INT (nullable)
new_min_team_size              INT (nullable)
previous_max_team_size         INT (nullable)
new_max_team_size              INT (nullable)
timestamp                DATETIME NOT NULL (set by @PrePersist)
```

> **Key design decision:** history rows are append-only. A project's full audit trail — every approve, reject, resubmit, team-size change — lives here. The `previous_status → new_status` pair means you can rebuild the entire timeline just from this table.

---

## Q3 — How is a project's state modelled? What happens if a Guide rejects and the student resubmits?

### Both: a status column AND a history table

```java
// Project.java — line 47
private ProjectStatus status = ProjectStatus.DRAFT;

// Project.java — line 116
private ProjectStatus rejectedAtStage;  // ← the sentinel that routes resubmissions
```

**Full state machine (from [ProjectStatus.java](file:///d:/downlodes/JON%20-%20Copy/pas/src/main/java/com/project/pas/model/ProjectStatus.java)):**
```
DRAFT
  ↓  (own idea submitted)
PROJECT_IDEA_PENDING_FACULTY
  ↓ (faculty approves idea)       ↓ (faculty rejects)
PROJECT_IDEA_APPROVED          FACULTY_REJECTED
  ↓                               ↑ (student edits + resubmits)
TOPIC_SELECTED ─────────────────→ PROPOSAL_PENDING_FACULTY
  ↓ (student submits proposal docs)
PROPOSAL_PENDING_FACULTY
  ↓ (faculty approves)
PROPOSAL_APPROVED
  ↓ (student clicks "Start Working")
STUDENT_WORKING
  ↓ (student uploads final files)
PENDING_FACULTY_REVIEW
  ↓ (faculty approves)           ↓ (faculty rejects)
FACULTY_APPROVED → PENDING_HOD_REVIEW   HOD_REJECTED
  ↓ (HOD approves)               ↑ (student resubmits)
COMPLETED                   STUDENT_RESUBMISSION (transient)
```

### Rejection → Resubmission flow (actual code, [ProjectService.java](file:///d:/downlodes/JON%20-%20Copy/pas/src/main/java/com/project/pas/service/ProjectService.java) lines ~599–830)

**Step 1 — Faculty rejects proposal:**
```java
// rejectProposal() - ProjectService.java
project.setStatus(ProjectStatus.FACULTY_REJECTED);
project.setRejectedAtStage(ProjectStatus.PROPOSAL_PENDING_FACULTY); // ← sentinel saved
projectRepository.save(project);
recordHistory(...);  // ← immutable audit row written
```

**Step 2 — Student sees the rejection + reason** (from the `approval_history` table's `rejection_reason` column).

**Step 3 — Student fixes proposal and hits resubmit:**
```java
// resubmit() - ProjectService.java
// First lands in a transient state
project.setStatus(ProjectStatus.STUDENT_RESUBMISSION);
projectRepository.save(project);
recordHistory(... STUDENT_RESUBMISSION ...);

// Then routes back using the sentinel
if (project.getRejectedAtStage() == ProjectStatus.PROPOSAL_PENDING_FACULTY) {
    targetStatus = ProjectStatus.PROPOSAL_PENDING_FACULTY;
} else if (...PENDING_HOD_REVIEW) {
    targetStatus = ProjectStatus.PENDING_HOD_REVIEW;
} ...
project.setStatus(targetStatus);
projectRepository.save(project);
recordHistory(... targetStatus ...);
```

**Critical constraint enforced during resubmit:**
> Core fields (`title`, `description`, `category`, `techStack`) are intentionally NOT touched in [resubmit()](file:///d:/downlodes/JON%20-%20Copy/pas/src/main/java/com/project/pas/service/ProjectService.java#751-811). The code comment says: *"They are read-only during resubmission and must not be changed."* This is enforced by simply not calling any setters for those fields.

**History table role vs status column:**
- `status` = the *current live state* (one row, latest value)
- `approval_history` = the *full immutable audit trail* (N rows, one per event)
- [isProjectDetailsLocked()](file:///d:/downlodes/JON%20-%20Copy/pas/src/main/java/com/project/pas/service/ProjectService.java#82-95) queries history to find if `PROPOSAL_APPROVED` ever appeared — because status alone can't tell you that once the project has moved forward.

---

## Q4 — How do you stop a Student from calling the approve endpoint directly in Postman?

### The three-layer defence (walk through each)

**Layer 1 — URL-level role enforcement (SecurityConfig.java, line 55)**
```java
.requestMatchers("/student/**").hasRole("STUDENT")
.requestMatchers("/faculty/**").hasRole("FACULTY")
.requestMatchers("/hod/**").hasRole("HOD")
```
A student hitting `POST /faculty/projects/7/approve-proposal` gets **403 Forbidden** immediately — Spring Security rejects the request before the controller even runs.

**Layer 2 — Owner/Guide enforcement in the Controller (FacultyController.java, lines 117-122)**
```java
if (project.getFacultyGuide() == null ||
    !project.getFacultyGuide().getId().equals(faculty.getId())) {
    redirect.addFlashAttribute("error",
        "Access denied: you are not the assigned guide for this project");
    return "redirect:/faculty/projects";
}
```
This stops **Faculty A** from approving **Faculty B's** student even though both have `ROLE_FACULTY`.

**Layer 3 — Service-layer validation (ProjectService.java → [validateAssignedGuide](file:///d:/downlodes/JON%20-%20Copy/pas/src/main/java/com/project/pas/service/ProjectService.java#947-959))**
```java
private void validateAssignedGuide(Project project, User faculty) {
    if (project.getFacultyGuide() == null ||
        !project.getFacultyGuide().getId().equals(faculty.getId())) {
        throw new IllegalStateException("Access denied: you are not the faculty guide");
    }
}
```
Every approve/reject method calls this before doing anything:
```java
public void approveProposal(Project project, User faculty, String comments) {
    validateFaculty(faculty);          // role check
    validateBranchMatch(faculty, ...); // branch check
    validateAssignedGuide(project, faculty); // ← HERE
    validateStatus(project, ProjectStatus.PROPOSAL_PENDING_FACULTY); // state check
    ...
}
```

**CSRF protection:** All POST forms carry the Spring Security CSRF token. A raw Postman call from a student's session still can't forge a faculty POST because the session identity (read via `Authentication auth`) will resolve to the student's [User](file:///d:/downlodes/JON%20-%20Copy/pas/src/main/java/com/project/pas/model/User.java#10-164), which then fails [validateFaculty()](file:///d:/downlodes/JON%20-%20Copy/pas/src/main/java/com/project/pas/service/ProjectService.java#908-913).

**Summary:** Student → correct role fails at Layer 1. Faculty from wrong branch fails at Layer 2. Correct faculty but not the assigned guide fails at Layer 3. Wrong project status fails at Layer 4 ([validateStatus](file:///d:/downlodes/JON%20-%20Copy/pas/src/main/java/com/project/pas/service/ProjectService.java#939-946)).

---

## Q5 — How do you know which Branch Admin (HOD) a project belongs to?

> There is no "Branch Admin" as a separate role. The equivalent role is **HOD**. The ownership chain is:

```
Project.branch  ──FK──►  branches.id
User(HOD).branch ──FK──► branches.id
```

**In the [Project](file:///d:/downlodes/JON%20-%20Copy/pas/src/main/java/com/project/pas/model/Project.java#11-407) entity ([Project.java](file:///d:/downlodes/JON%20-%20Copy/pas/src/main/java/com/project/pas/model/Project.java), line 41):**
```java
@ManyToOne(fetch = FetchType.EAGER)
@JoinColumn(name = "branch_id", nullable = false)
private Branch branch;
```
This `branch_id` is set at project creation time to `student.getBranch()`:
```java
// ProjectService.submitOwnIdea()
project.setBranch(student.getBranch());
```

**HOD queries use their own branch identity:**
```java
// HodController (typical pattern)
User hod = getCurrentUser(auth);
// The service then filters by hod.getBranch()
projectService.getProjectsByBranchAndStatuses(hod.getBranch(), ...)
```

**HOD approve enforces branch match at service layer:**
```java
public void hodApprove(Project project, User hod, String comments) {
    validateHod(hod);
    validateBranchMatch(hod, project.getBranch()); // ← prevents cross-branch HOD action
    ...
}
```

So the chain is: `project.branch == hod.branch`. A HOD can only see and act on projects where `project.branch_id == their own branch_id`. This check happens in the **service layer**, not just in the UI.

---

## Q6 — Two Branch Admins (HODs) act on the same project at once. What happens?

### What the code does today (no explicit optimistic lock)

The [Project](file:///d:/downlodes/JON%20-%20Copy/pas/src/main/java/com/project/pas/model/Project.java#11-407) entity has no `@Version` field, so there is **no optimistic locking**. Here is the race scenario:

```
T=0: HOD-A reads project (status = PENDING_HOD_REVIEW)
T=0: HOD-B reads project (status = PENDING_HOD_REVIEW)
T=1: HOD-A calls hodApprove() → validateStatus passes → sets COMPLETED → save()
T=2: HOD-B calls hodApprove() → validateStatus still passes (stale read) → sets COMPLETED → save()
Result: Two COMPLETED history rows. Project is "double-approved."
```

### Why it rarely matters in practice

1. There is only **one HOD per branch**. Two HODs from different branches can't act on the same project because [validateBranchMatch](file:///d:/downlodes/JON%20-%20Copy/pas/src/main/java/com/project/pas/service/ProjectService.java#926-931) blocks cross-branch actions.
2. Both actions here result in `COMPLETED` — so the duplicate outcome is harmless but wasteful.
3. If one approves and one rejects simultaneously, the last `save()` wins — this is a real bug.

### What you should say in the interview

> "We don't have optimistic locking implemented. The correct fix would be to add `@Version private Long version;` to the [Project](file:///d:/downlodes/JON%20-%20Copy/pas/src/main/java/com/project/pas/model/Project.java#11-407) entity. Spring Data JPA would then automatically detect stale reads and throw an `OptimisticLockException`, which the controller could catch and tell the second HOD 'this project was already actioned.' It's a known gap."

### The fix (30 seconds to code)
```java
// Project.java
@Version
private Long version;
```

That single annotation makes Hibernate add `WHERE version = ?` to every UPDATE, preventing silent overwrites.

---

## Q7 — How do you authenticate users? Session, JWT, or Spring Security defaults?

### Session-based authentication via Spring Security's form login

**[SecurityConfig.java](file:///d:/downlodes/JON%20-%20Copy/pas/src/main/java/com/project/pas/config/SecurityConfig.java) tells the whole story:**
```java
.formLogin(form -> form
    .loginPage("/login")
    .successHandler(roleBasedSuccessHandler()) // redirects by role
    .failureUrl("/login?error=true")
    .permitAll())
```

**The full authentication pipeline:**

1. **User POSTs** `email` + [password](file:///d:/downlodes/JON%20-%20Copy/pas/src/main/java/com/project/pas/config/SecurityConfig.java#32-36) to `/login`
2. **`DaoAuthenticationProvider`** calls `CustomUserDetailsService.loadUserByUsername(email)`
3. `CustomUserDetailsService` calls `UserRepository.findByEmail(email)` → returns a [User](file:///d:/downlodes/JON%20-%20Copy/pas/src/main/java/com/project/pas/model/User.java#10-164) entity
4. It maps `User.role` to a Spring `GrantedAuthority` (e.g., `ROLE_FACULTY`)
5. Spring Security compares the submitted password against the stored BCrypt hash
6. On success, a **server-side HTTP session** is created; a `JSESSIONID` cookie is sent to the browser
7. On subsequent requests, Spring Security reads the session, reconstructs the [Authentication](file:///d:/downlodes/JON%20-%20Copy/pas/src/main/java/com/project/pas/config/SecurityConfig.java#77-97) object, and populates the `SecurityContext`
8. Controllers access the logged-in user via `@AuthenticationPrincipal` or `auth.getName()` (the email)

**Password encoding:**
```java
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();  // default strength 10
}
```

**No JWT anywhere.** This is a server-rendered Thymeleaf monolith, so session cookies are the natural fit. JWT would be needed only if you were building a separate React/mobile frontend.

**[roleBasedSuccessHandler()](file:///d:/downlodes/JON%20-%20Copy/pas/src/main/java/com/project/pas/config/SecurityConfig.java#74-99)** post-login redirects ([SecurityConfig.java](file:///d:/downlodes/JON%20-%20Copy/pas/src/main/java/com/project/pas/config/SecurityConfig.java), lines 74–97):
- `ROLE_ADMIN` → `/admin/dashboard`
- `ROLE_STUDENT` → `/student/dashboard`
- `ROLE_FACULTY` → `/faculty/dashboard`
- `ROLE_HOD` → `/hod/dashboard`

---

## Q8 — What did you test with Postman, and what did you never test?

### Honest answer (what the code tells us)

**Tested via Postman (likely):**
- Login endpoint — form POST to `/login` with email/password
- Student `POST /student/project/custom` — submitting own idea
- Student `POST /student/project/{id}/proposal` — submitting proposal fields
- Faculty `POST /faculty/projects/{id}/approve-proposal` — approval flow
- Faculty `POST /faculty/projects/{id}/reject-proposal` — rejection with reason
- HOD `POST /hod/projects/{id}/approve` — final approval
- File download `GET /faculty/projects/{id}/download/{type}` — binary response
- CSV upload `POST /admin/users/upload` — multipart form

**Almost certainly NOT tested with Postman:**
- **Concurrent requests** — two users hitting the same endpoint simultaneously (no load testing)
- **CSRF token bypass** — Postman tests likely ignored CSRF or hit the `/api/**` path which has CSRF disabled (`csrf.ignoringRequestMatchers("/api/**")`)
- **Token expiry** on password reset — the 24-hour expiry edge case
- **Auto-assignment cron job** — `GuideSelectionScheduler` — can't easily trigger from Postman
- **Pagination boundaries** — page=0 with size=0, or requesting page 99 of a 10-row set
- **Branch isolation violation attempts** — e.g., HOD A trying to approve HOD B's branch project via crafted URL
- **Team member race condition** — two students adding the same third student simultaneously
- **File size limits** — uploading a 500MB zip
- **Empty file uploads** — POSTing with no file attached

> **Say this:** "I tested happy-path flows manually with Postman. I did not run any automated integration tests, load tests, or adversarial security tests. The CSRF bypass via `/api/**` is a gap — I added that exclusion for the email test endpoint but it means any POST to `/api/` skips CSRF protection."

---

## Q9 — If 5,000 students submit in the same week, what breaks first?

### In order of failure probability:

**1. File storage (almost immediate)**
```java
// FileStorageService — stores to local disk
uploads/proposals/{id}/presentation.pptx
uploads/final/{id}/report.pdf
```
- 5,000 PPT submissions = 5,000 × ~10MB = **50GB on the application server's local disk**
- No streaming, no S3, no CDN
- Server runs out of disk space; file writes start failing
- **Fix:** Migrate to AWS S3 / Azure Blob Storage

**2. EAGER loading causing N+1 queries (degrades fast under load)**
```java
// Project.java
@ManyToOne(fetch = FetchType.EAGER)  // student
@ManyToOne(fetch = FetchType.EAGER)  // branch
@ManyToOne(fetch = FetchType.EAGER)  // topic
@ManyToOne(fetch = FetchType.EAGER)  // facultyGuide
```
Every page load that lists 10 projects fires 40+ SQL queries. With 5,000 projects, the HOD dashboard listing `PENDING_HOD_REVIEW` projects will be extremely slow.
- **Fix:** Switch to `LAZY` + use `@EntityGraph` or HQL `JOIN FETCH` on specific queries

**3. Auto-assignment scheduler (`GuideSelectionScheduler`)**
```java
// GuideSelectionService.autoAssignAll()
// Iterates all unassigned students → assigns round-robin
// All inside a single @Transactional
```
With 5,000 students in one branch, this single transaction holds a table lock for potentially minutes. Other requests time out.
- **Fix:** Batch the assignment in chunks (page through students, commit each batch)

**4. Search/filter queries without indexes**
```java
projectRepository.findByBranchAndStatusIn(branch, statuses)
```
Without a composite index on [(branch_id, status)](file:///d:/downlodes/JON%20-%20Copy/pas/src/main/java/com/project/pas/model/User.java#10-164), this is a full table scan across 5,000 rows every time the HOD dashboard loads.
- **Fix:** `ALTER TABLE projects ADD INDEX idx_branch_status (branch_id, status);`

**5. Embedded Tomcat thread pool exhaustion**
- Default Spring Boot embedded Tomcat: 200 threads
- If 200 students all submit simultaneously and file uploads take 30s each, the thread pool saturates
- **Fix:** Async file processing; move uploads to a background job queue (e.g., Spring `@Async` + a work queue)

---

## Q10 — What would you rewrite if you started again on Monday?

### Honest technical retrospective:

**1. Replace local disk storage with object storage first**
Every file operation is brittle. S3 + pre-signed URLs means files never touch the app server. This is the highest-leverage change.

**2. Add `@Version` optimistic locking to [Project](file:///d:/downlodes/JON%20-%20Copy/pas/src/main/java/com/project/pas/model/Project.java#11-407)**
One annotation. Prevents silent overwrites in concurrent approval scenarios. Should have been there from day one.

**3. Switch all FetchType to LAZY + use `@EntityGraph` per query**
The blanket `EAGER` loading on [Project](file:///d:/downlodes/JON%20-%20Copy/pas/src/main/java/com/project/pas/model/Project.java#11-407) (4 eager associations) is a performance time bomb. For list views, you only need `student.fullName` and `branch.code`, not entire faculty objects.

**4. Add proper bean validation + a `@ControllerAdvice` for validation errors**
Currently validation is done with `if` statements inside services throwing `IllegalStateException`. The proper Spring approach is `@Valid` on controller params + `@ExceptionHandler(MethodArgumentNotValidException)` in `GlobalControllerAdvice`. This would halve the boilerplate in [ProjectService](file:///d:/downlodes/JON%20-%20Copy/pas/src/main/java/com/project/pas/service/ProjectService.java#29-983).

**5. Write integration tests (at least for the state machine)**
There are zero automated tests. A `@SpringBootTest` test for the proposal → reject → resubmit → approve path would have caught the `rejectedAtStage` routing bug earlier. At minimum: test that a student can't skip stages.

**6. Separate the [resubmit()](file:///d:/downlodes/JON%20-%20Copy/pas/src/main/java/com/project/pas/service/ProjectService.java#751-811) method by stage**
[resubmit()](file:///d:/downlodes/JON%20-%20Copy/pas/src/main/java/com/project/pas/service/ProjectService.java#751-811) handles both Stage 1 and Stage 2 rejections with `if/else` on `rejectedAtStage`. This is confusing. Better: `resubmitProposal()` and `resubmitFinalProject()` as distinct service methods with clear, stage-specific validation.

**7. Rate-limit the forgot-password endpoint**
As noted in the project docs, `/forgot-password` has no rate limiting. `Bucket4j` with a `@RateLimiter` annotation is a 15-minute addition.

---

## Quick Reference Cheat Sheet

| Question | Answer in one line |
|---|---|
| Architecture | Spring MVC monolith: Browser → Controller → Service → Repository → MySQL |
| Tables | 10 tables; core join: projects has 4 FKs (student, branch, topic, facultyGuide) |
| State modelling | Status column (current state) + approval_history (full audit) + rejectedAtStage (routing sentinel) |
| Role enforcement | 3 layers: URL rule → controller ID check → service validateAssignedGuide() |
| Branch ownership | project.branch_id = student.branch_id at creation; HOD queries filter by their own branch |
| Concurrent HODs | Last write wins — real race condition; fix = @Version on Project entity |
| Authentication | Spring Security form login → BCrypt → server-side HTTP session (no JWT) |
| Postman gaps | No concurrent, no CSRF bypass tests, no auto-assign trigger, no load tests |
| Scale bottleneck | Local file storage breaks first, then N+1 EAGER queries, then single @Transactional auto-assign |
| Would rewrite | S3 for files, LAZY loading, @Version, integration tests, split resubmit by stage |
