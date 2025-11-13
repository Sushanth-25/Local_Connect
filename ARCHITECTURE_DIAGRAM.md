# Architecture Diagram: Comments & Aggregate Counters

## System Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                          UI Layer                            │
│                     (Jetpack Compose)                        │
│                                                              │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐     │
│  │  Post Card   │  │ Post Detail  │  │  Comments    │     │
│  │              │  │              │  │  Section     │     │
│  │  ❤️ 125      │  │  💬 Add      │  │              │     │
│  │  💬 45       │  │  Comment     │  │  [List of    │     │
│  │  👁️ 1234     │  │              │  │  comments]   │     │
│  └──────────────┘  └──────────────┘  └──────────────┘     │
│         │                  │                  │             │
│         └──────────────────┼──────────────────┘             │
└─────────────────────────────┼──────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                      ViewModel Layer                         │
│                   (State Management)                         │
│                                                              │
│  ┌────────────────────────────────────────────────────┐    │
│  │           CommentsViewModel                        │    │
│  │                                                     │    │
│  │  StateFlow<List<Comment>> comments                 │    │
│  │  StateFlow<PostStats?> postStats                   │    │
│  │                                                     │    │
│  │  fun observeComments(postId)                       │    │
│  │  fun addComment(...)                               │    │
│  │  fun likePost(postId)                              │    │
│  │  fun trackPostView(postId)                         │    │
│  └────────────────────────────────────────────────────┘    │
│         │                               │                   │
└─────────┼───────────────────────────────┼───────────────────┘
          │                               │
          ▼                               ▼
┌──────────────────────────┐   ┌──────────────────────────┐
│   CommentRepository      │   │ FirebasePostRepository    │
│                          │   │                           │
│  • addComment()          │   │  • likePost()             │
│  • getComments()         │   │  • unlikePost()           │
│  • observeComments()     │   │  • incrementViews()       │
│  • deleteComment()       │   │  • getPostStats()         │
│  • likeComment()         │   │                           │
│  • getReplies()          │   │  Returns: PostStats       │
│                          │   │  - likes: Int             │
│  🔒 Uses Transactions    │   │  - comments: Int          │
│  ⚡ Real-time Flow       │   │  - views: Int             │
│                          │   │  - upvotes: Int           │
└──────────┬───────────────┘   └──────────┬────────────────┘
           │                              │
           └──────────┬───────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────────┐
│                  Firestore Database                          │
│                                                              │
│  posts/                                                      │
│  └── {postId}/                                              │
│      ├── likes: 125        ← Aggregate Counter              │
│      ├── comments: 45      ← Aggregate Counter              │
│      ├── views: 1234       ← Aggregate Counter              │
│      ├── upvotes: 89       ← Aggregate Counter              │
│      ├── userId: "..."                                      │
│      ├── caption: "..."                                     │
│      ├── timestamp: 123456                                  │
│      └── comments/         ← Subcollection                  │
│          ├── {commentId1}/                                  │
│          │   ├── commentId: "abc123"                        │
│          │   ├── postId: "post123"                          │
│          │   ├── userId: "user456"                          │
│          │   ├── userName: "John Doe"                       │
│          │   ├── text: "Great post!"                        │
│          │   ├── timestamp: 1699876543210                   │
│          │   ├── likes: 5                                   │
│          │   └── parentCommentId: null                      │
│          └── {commentId2}/                                  │
│              ├── ... (same structure)                       │
│              └── parentCommentId: "commentId1"  ← Reply     │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

## Data Flow Diagrams

### 1. Adding a Comment

```
User taps "Post Comment"
         │
         ▼
┌─────────────────────┐
│ CommentsViewModel   │
│ .addComment()       │
└──────────┬──────────┘
           │
           ▼
┌─────────────────────┐
│ CommentRepository   │
│ .addComment()       │
└──────────┬──────────┘
           │
           ▼
┌─────────────────────────────────────────┐
│  Firestore Transaction (Atomic)         │
│                                          │
│  1. Add comment to subcollection        │
│     posts/{postId}/comments/{commentId} │
│                                          │
│  2. Increment counter on post           │
│     posts/{postId}.comments += 1        │
│                                          │
│  3. Update timestamp                    │
│     posts/{postId}.updatedAt = now()    │
└──────────┬──────────────────────────────┘
           │
           ▼
┌─────────────────────┐
│ Real-time Listener  │
│ observeComments()   │
│ receives update     │
└──────────┬──────────┘
           │
           ▼
┌─────────────────────┐
│ UI automatically    │
│ updates with new    │
│ comment             │
└─────────────────────┘
```

### 2. Liking a Post

```
User taps "Like" button
         │
         ▼
┌─────────────────────┐
│ CommentsViewModel   │
│ .likePost()         │
└──────────┬──────────┘
           │
           ▼
┌─────────────────────┐
│ PostRepository      │
│ .likePost()         │
└──────────┬──────────┘
           │
           ▼
┌─────────────────────────────────────────┐
│  Firestore Transaction (Atomic)         │
│                                          │
│  1. Increment likes counter             │
│     posts/{postId}.likes += 1           │
│                                          │
│  2. Update timestamp                    │
│     posts/{postId}.updatedAt = now()    │
└──────────┬──────────────────────────────┘
           │
           ▼
┌─────────────────────┐
│ ViewModel reloads   │
│ stats to show       │
│ updated count       │
└─────────────────────┘
```

### 3. Real-time Comment Updates

```
┌─────────────────────┐
│ Firestore           │
│ Snapshot Listener   │
└──────────┬──────────┘
           │ (whenever comments change)
           ▼
┌─────────────────────┐
│ CommentRepository   │
│ .observeComments()  │
│ Flow emits new data │
└──────────┬──────────┘
           │
           ▼
┌─────────────────────┐
│ CommentsViewModel   │
│ StateFlow updates   │
└──────────┬──────────┘
           │
           ▼
┌─────────────────────┐
│ Compose UI          │
│ recomposes with     │
│ new comments        │
└─────────────────────┘
```

## Key Concepts

### Aggregate Counters
```
┌──────────────────────────────────────────────┐
│  Why Aggregate Counters?                     │
│                                              │
│  ❌ WITHOUT Counters:                        │
│  To show comment count, must query           │
│  entire subcollection:                       │
│  • Expensive (reads all documents)           │
│  • Slow (network latency)                    │
│  • Costs more (Firestore billing)            │
│                                              │
│  ✅ WITH Counters:                           │
│  Counter stored on post document:            │
│  • Fast (single document read)               │
│  • Cheap (1 read vs many)                    │
│  • Instant (O(1) access)                     │
│                                              │
│  posts/{postId}/comments: 45 ← Single field! │
└──────────────────────────────────────────────┘
```

### Atomic Transactions
```
┌──────────────────────────────────────────────┐
│  Why Transactions?                           │
│                                              │
│  ❌ WITHOUT Transactions:                    │
│  1. Add comment                              │
│  2. ❗ App crashes                           │
│  3. Counter not incremented                  │
│  Result: Count is wrong! 😱                  │
│                                              │
│  ✅ WITH Transactions:                       │
│  1. Start transaction                        │
│  2. Add comment                              │
│  3. Increment counter                        │
│  4. ❗ App crashes before commit             │
│  5. Transaction rolls back                   │
│  Result: Everything consistent! ✅           │
│                                              │
│  Firestore ensures ALL or NOTHING           │
└──────────────────────────────────────────────┘
```

### Subcollections vs Arrays
```
┌──────────────────────────────────────────────┐
│  Why Subcollections for Comments?           │
│                                              │
│  ❌ Array in Post Document:                  │
│  posts/{postId}                              │
│    └── comments: [                           │
│          {text: "...", user: "..."},         │
│          {text: "...", user: "..."},         │
│          ... 1000 more comments              │
│        ]                                     │
│                                              │
│  Problems:                                   │
│  • Document size limit (1MB)                 │
│  • Must load ALL comments always            │
│  • Can't query/filter comments              │
│  • Can't paginate                           │
│  • No real-time for individual comments     │
│                                              │
│  ✅ Subcollection:                           │
│  posts/{postId}/comments/{commentId}         │
│                                              │
│  Benefits:                                   │
│  • Unlimited comments                        │
│  • Query & filter                           │
│  • Pagination support                       │
│  • Real-time updates                        │
│  • Independent document operations          │
│  • Security rules per comment               │
└──────────────────────────────────────────────┘
```

## Performance Characteristics

```
Operation               | Reads | Writes | Latency
────────────────────────|────---|────────|──────────
Get post stats          |   1   |   0    | ~50ms
Add comment             |   1   |   2    | ~100ms
Get 50 comments         |  50   |   0    | ~200ms
Like post               |   1   |   1    | ~100ms
Delete comment          |   1   |   2    | ~100ms
Real-time updates       |   0*  |   0    | instant

* After initial subscription
```

## Security Model

```
┌─────────────────────────────────────────────┐
│  Firestore Security Rules                   │
│                                             │
│  posts/{postId}                             │
│    ├── read: anyone                         │
│    ├── create: authenticated users          │
│    └── update/delete: post owner only       │
│                                             │
│  posts/{postId}/comments/{commentId}        │
│    ├── read: anyone                         │
│    ├── create: authenticated users          │
│    ├── update: comment owner only           │
│    └── delete: comment owner OR post owner  │
│                                             │
│  Validation Rules:                          │
│    • Comment text: 1-1000 characters        │
│    • userId must match auth.uid             │
│    • postId must match parent document      │
│    • Cannot change userId after creation    │
└─────────────────────────────────────────────┘
```

## Scalability

```
Comments per Post | Strategy
──────────────────|─────────────────────────────
< 100             | Load all at once
100 - 1000        | Pagination (20-50 per page)
> 1000            | Virtual scrolling + pagination
> 10,000          | Consider comment threads/sections

The aggregate counter works efficiently
regardless of comment count! 🚀
```

## Summary

✅ **3-Layer Architecture**: UI → ViewModel → Repository  
✅ **Aggregate Counters**: Fast O(1) access to stats  
✅ **Subcollections**: Scalable comment storage  
✅ **Atomic Transactions**: Data consistency guaranteed  
✅ **Real-time Updates**: Instant UI updates via Flow  
✅ **Security Rules**: Proper access control  
✅ **Nested Comments**: Support for replies  

This architecture supports millions of posts and comments
while maintaining excellent performance! 🎯

