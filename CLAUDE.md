# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```sh
# Install dependencies
npm install

# Start dev server with hot reload (app available at http://localhost:8280/)
npm run watch

# Run tests (headless Chrome, single run)
npm run ci

# Production build
npm run release

# Lint (requires clj-kondo installed)
clj-kondo --lint src
```

The nREPL server runs on port 8777. Connect with your editor or:
```sh
lein repl :connect localhost:8777
# then: (shadow.cljs.devtools.api/nrepl-select :app)
```

## Architecture

This is a ClojureScript SPA using the **re-frame** pattern (unidirectional data flow):

**Panel routing pattern**: `routes/panels` is a multimethod. Add a new route by:
1. Adding a path in `routes/routes` atom
2. Defining a component and `(defmethod routes/panels :your-panel-name [] [your-component])`

The `::events/navigate` event triggers the `:navigate` effect which calls `pushy/set-token!`, causing pushy to dispatch `::events/set-active-panel` with the matched panel keyword.

**Build targets** (defined in `shadow-cljs.edn`):
- `:app` — main browser build, dev server at port 8280
- `:browser-test` — browser test runner at port 8290
- `:karma-test` — headless Karma test runner, output to `target/karma-test.js`

Test namespaces must end in `-test` (files `*_test.cljs`).

### Overview

A local-only PWA for managing communal food and drink consumption. Runs on a single shared tablet. Users track what they consume, manage their balance, and the system maintains a full audit trail via event sourcing.

The app trusts its users but logs everything.

Written in ClojureScript using re-frame. Persisted with konserve.

---

### Glossary

These five terms carry specific meaning throughout the codebase.

**Command** — A user's intent to change something. Never persisted.

**Domain Event** — An immutable fact that happened. Append-only. Once in the log, it stays. Has a sequential integer ID.

**Snapshot** — The current state of the world, derived by applying all events in order. A cache — rebuildable by replaying the log.

**Reducer** — A pure function `(snapshot, event) → snapshot'`. The only place where domain state transitions are defined.

**Compensating Event** — How you reverse something in an append-only log. The original event stays; a new event (e.g., `:transaction/voided`) cancels its effect.

---

### Roles & Capabilities

Every user can consume, view their own balance, top up, and change their own pin Capabilities only name the things that are *restricted*:

```clojure
(def extra-capabilities
  {:kitchen #{:restock :manage-items}
   :admin   #{:manage-users :view-all-profiles
              :void-transaction}})
```

Checking permission:

```clojure
(defn can? [role capability]
  (or (not (restricted? capability))
      (contains? (get extra-capabilities role) capability)))
```

If it's not in any role's set, anyone can do it. If it is, your role must include it.

---

### User Model

```clojure
{:user/id       1
 :user/name     "Alex"
 :user/role     :member     ;; :member, :kitchen, or :admin
 :user/pin-hash "bcrypt-hashed-pin"
 :user/active?  true}
```

Pins are hashed with bcrypt — not encrypted. No key to manage, no way to recover the pin, only to verify it.

Authentication: user selects name → enters pin → app hashes input → compares to stored hash.

---

### Domain Events

Every domain state change is a self-contained fact with a sequential integer ID:

```clojure
{:event/type       :consumption/recorded
 :event/id         12
 :event/timestamp  "2026-04-18T14:30:00Z"
 :event/actor      "user-id"
 :consumption/item-id    "item-id"
 :consumption/quantity   2
 :consumption/unit-price 1.50}

{:event/type       :balance/topped-up
 :event/id         13
 :event/timestamp  "2026-04-18T14:31:00Z"
 :event/actor      "user-id"
 :top-up/amount    20.00}

{:event/type            :transaction/voided
 :event/id              14
 :event/timestamp       "2026-04-18T15:00:00Z"
 :event/actor           "admin-id"
 :void/original-event   12
 :void/reason           "Accidental double entry"}
```

The ID is the event's position in the log. It's stored inside the event so that exported events are self-describing and cross-references (like `:void/original-event 12`) are just integers.

---

### The One Rule

**Re-frame event handlers never write to `[:domain ...]` directly.** All domain mutations flow through the `:persist!` effect. Handlers may freely write to `[:ui ...]`.

This separates the app-db into two regions:

```clojure
{:domain { ... }    ;; snapshot — only written by :domain/state-updated
 :ui     { ... }}   ;; modals, loading states, selections — freely written
```

---

### Event Flow

```
User taps a button
    ↓
re-frame event handler
    ├── validates (permissions, business rules)
    ├── rejected → {:notify error}
    └── accepted → {:persist! domain-event}
                        ↓
               persist! effect handler
                 1. reducer(snapshot, event) → snapshot'
                 2. dispatch :domain/state-updated snapshot'  ← sync, UI updates
                 3. append event to konserve log              ← async
                 4. persist snapshot' to konserve              ← async
                        ↓
               :domain/state-updated
                 assoc snapshot' into app-db [:domain]
                        ↓
               subscriptions on [:domain ...] re-render UI
```

Steps 3 and 4 are sequential: event before snapshot. This preserves the invariant that replaying the log reproduces the snapshot.

On a local-only app, storage writes are near-certain to succeed. The worst case — tablet dies mid-write — loses the last action.

---

### Wiring

A command handler:

```clojure
(rf/reg-event-fx
  :command/record-consumption
  (fn [{:keys [db]} [_ {:keys [user-id item-id quantity]}]]
    (let [user  (get-in db [:domain :users user-id])
          price (get-in db [:domain :items item-id :item/price])
          next-id (count (get-in db [:domain :event-log]))]
      (if (and (:user/active? user) price)
        {:persist!
         {:event/type             :consumption/recorded
          :event/id               next-id
          :event/timestamp        (now)
          :event/actor            user-id
          :consumption/item-id    item-id
          :consumption/quantity   quantity
          :consumption/unit-price price}}
        {:db (assoc-in db [:ui :error] "Not allowed")}))))
```

The persist effect:

```clojure
(rf/reg-fx
  :persist!
  (fn [event]
    (let [snapshot  (:domain @re-frame.db/app-db)
          snapshot' (reduce-event snapshot event)]
      ;; sync: update UI
      (rf/dispatch-sync [:domain/state-updated snapshot'])
      ;; async: persist, event before snapshot
      (go
        (<! (konserve/append store :event-log event))
        (<! (konserve/assoc-in store [:snapshot] snapshot'))))))

(rf/reg-event-db
  :domain/state-updated
  (fn [db [_ snapshot']]
    (assoc db :domain snapshot')))
```

---

### The Reducer

Pure function. No side effects, no IO.

```clojure
(defmulti reduce-event
  (fn [_snapshot event] (:event/type event)))

(defmethod reduce-event :consumption/recorded
  [snapshot {:keys [event/actor consumption/item-id
                    consumption/quantity consumption/unit-price]}]
  (let [cost (* quantity unit-price)]
    (-> snapshot
        (update-in [:balances actor] - cost)
        (update-in [:items item-id :item/stock] - quantity))))

(defmethod reduce-event :balance/topped-up
  [snapshot {:keys [event/actor top-up/amount]}]
  (update-in snapshot [:balances actor] + amount))

(defmethod reduce-event :transaction/voided
  [snapshot {:keys [void/original-event]}]
  (let [original (get-in snapshot [:event-log-index void/original-event])]
    (reverse-event snapshot original)))
```

Because it's pure, testing is trivial: pass a snapshot and an event, assert the output.

---

### Persistence with Konserve

Konserve stores two things:

1. **Event log** — ordered list of all domain events, append-only
2. **Snapshot** — current materialized state (a cache)

If the snapshot is lost or corrupted:

```clojure
(defn rebuild-snapshot [event-log]
  (reduce reduce-event {} event-log))
```

Persistence frequency is a tuning knob. Start with persisting after every event. Batch later if needed.

---

### Export

The event log exports as EDN or JSON. Events are self-contained and sequentially numbered, so the export is a complete, human-readable audit trail. No snapshot needed — any consumer rebuilds state by replaying.

---

## 1. Think Before Coding

**Don't assume. Don't hide confusion. Surface tradeoffs.**

Before implementing:
- State your assumptions explicitly. If uncertain, ask.
- If multiple interpretations exist, present them - don't pick silently.
- If a simpler approach exists, say so. Push back when warranted.
- If something is unclear, stop. Name what's confusing. Ask.

## 2. Simplicity First

**Minimum code that solves the problem. Nothing speculative.**

- No features beyond what was asked.
- No abstractions for single-use code.
- No "flexibility" or "configurability" that wasn't requested.
- No error handling for impossible scenarios.
- If you write 200 lines and it could be 50, rewrite it.

Ask yourself: "Would a senior engineer say this is overcomplicated?" If yes, simplify.

## 3. Surgical Changes

**Touch only what you must. Clean up only your own mess.**

When editing existing code:
- Don't "improve" adjacent code, comments, or formatting.
- Don't refactor things that aren't broken.
- Match existing style, even if you'd do it differently.
- If you notice unrelated dead code, mention it - don't delete it.

When your changes create orphans:
- Remove imports/variables/functions that YOUR changes made unused.
- Don't remove pre-existing dead code unless asked.

The test: Every changed line should trace directly to the user's request.

## 4. Goal-Driven Execution

**Define success criteria. Loop until verified.**

Transform tasks into verifiable goals:
- "Add validation" → "Write tests for invalid inputs, then make them pass"
- "Fix the bug" → "Write a test that reproduces it, then make it pass"
- "Refactor X" → "Ensure tests pass before and after"

For multi-step tasks, state a brief plan:
```
1. [Step] → verify: [check]
2. [Step] → verify: [check]
3. [Step] → verify: [check]
```

Strong success criteria let you loop independently. Weak criteria ("make it work") require constant clarification.