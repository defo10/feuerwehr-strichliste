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

As ClojureScript is a dynamic programming language, we must be very disciplined about introducing different data shapes. As such we want to define core data models in . We must be particulary careful about introducing semenatically similar but slightly different models, as this will introduce complexity down the line.

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
 :user/status   :active}    ;; :active, :inactive, or :suspended
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

**Every domain mutation must be paired with a `:persist!` effect.** Handlers may freely write to `[:ui ...]`.

Command handlers apply the event to the domain snapshot using `apply-event`, then return both the updated db and the persist effect together — ensuring the two are never decoupled:

```clojure
{:db       (assoc db :domain domain')
 :persist! {:event event' :snapshot domain'}}
```

This separates the app-db into two regions:

```clojure
{:domain { ... }    ;; snapshot — materialized view, updated by command handlers
 :ui     { ... }}   ;; modals, loading states, selections — freely written
```

---

### Event Flow

```
User taps a button
    ↓
re-frame command handler
    ├── validates (permissions, business rules)
    ├── rejected → {:db (assoc-in db [:ui :error] ...)}
    └── accepted → apply-event(snapshot, event) → {domain', event'}
                        ↓
                   {:db       (assoc db :domain domain')   ← sync, UI updates immediately
                    :persist! {:event event' :snapshot domain'}}
                        ↓
               persist! effect handler (async)
                 1. append event' to konserve log
                 2. persist domain' snapshot to konserve
                        ↓
               on error → dispatch [:error :errors/persist-failed message]
```

Steps 1 and 2 are sequential: event before snapshot. This preserves the invariant that replaying the log reproduces the snapshot.

The UI updates synchronously via `:db` before konserve writes complete. On a local-only app, storage writes are near-certain to succeed. The worst case — tablet dies mid-write — loses the last action.

---

### Wiring

A command handler:

```clojure
(re-frame/reg-event-fx
 :command/record-consumption
 (fn [{:keys [db]} [_ {:keys [user-id item-id quantity]}]]
   (let [user  (get-in db [:domain :users user-id])
         price (get-in db [:domain :items item-id :item/price])]
     (if (and (= :active (:user/status user)) price)
       (let [{:keys [domain event]} (reducer/apply-event
                                     (:domain db)
                                     {:event/type             :consumption/recorded
                                      :event/timestamp        (.toISOString (js/Date.))
                                      :event/actor            user-id
                                      :consumption/item-id    item-id
                                      :consumption/quantity   quantity
                                      :consumption/unit-price price})]
         {:db       (assoc db :domain domain)
          :persist! {:event event :snapshot domain}})
       {:db (assoc-in db [:ui :error] {:type :errors/not-allowed :message "Not allowed"})}))))
```

The persist effect (pure async IO — no db access):

```clojure
(re-frame/reg-fx
 :persist!
 (fn [{:keys [event snapshot]}]
   (go
     (try
       (<! (k/append @storage/store :event-log event))
       (<! (k/assoc-in @storage/store [:snapshot] snapshot))
       (catch :default e
         (re-frame/dispatch [:error :errors/persist-failed (.-message e)])
         (when config/debug?
           (js/setTimeout #(throw e) 0)))))))
```

---

### The Reducer

Pure function. No side effects, no IO. No `:default` method — unhandled event types throw explicitly.

```clojure
(defmulti reduce-event (fn [_snapshot event] (:event/type event)))

(defmethod reduce-event :consumption/recorded
  [snapshot {:keys [event/actor consumption/item-id
                    consumption/quantity consumption/unit-price]}]
  (let [cost (* quantity unit-price)]
    (-> snapshot
        (update-in [:balances actor] - cost)
        (update-in [:items item-id :item/stock] - quantity))))
```

`apply-event` wraps `reduce-event` — it assigns the sequential ID and increments the counter:

```clojure
(defn apply-event [domain event]
  (let [id      (:next-event-id domain)
        event'  (assoc event :event/id id)
        domain' (-> (reduce-event domain event')
                    (update :next-event-id inc))]
    {:domain domain' :event event'}))
```

Command handlers call `apply-event`, never `reduce-event` directly. Because it's pure, testing is trivial: pass a snapshot and an event, assert the output.

The snapshot shape:

```clojure
{:users         {}   ;; map of id → user
 :balances      {}   ;; map of user-id → amount
 :next-event-id 0}   ;; used by apply-event for sequential IDs
```

The event log lives in konserve only — not in the snapshot.

---

### Persistence with Konserve

Konserve stores two things:

1. **Event log** — ordered list of all domain events, append-only (`k/append store :event-log event`)
2. **Snapshot** — current materialized state, a cache (`k/assoc-in store [:snapshot] snapshot`)

The backend is selected at startup based on `config/debug?`:
- **Dev**: in-memory store (reset on reload, no setup required)
- **Prod**: IndexedDB (persists across reloads)

```clojure
(defn init! [on-ready]
  (if config/debug?
    (do (reset! store (k/create-store mem-config {:sync? true}))
        (on-ready))
    (go
      (reset! store (<! (k/create-store idb-config {:sync? false})))
      (on-ready))))
```

If the snapshot is lost or corrupted, rebuild by replaying the log:

```clojure
(defn rebuild-snapshot [event-log]
  (reduce #(:domain (reducer/apply-event %1 %2)) reducer/empty-snapshot event-log))
```

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