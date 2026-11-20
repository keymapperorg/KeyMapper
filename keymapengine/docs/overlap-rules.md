# Overlapping trigger rules

What fires when two triggers share keys.

Trigger types:

- **Single**: one key. It's a 1-key sequence, the leaf or inner node directly under the trie root.
- **Sequence** (`A → B`): key-down order, other keys ignored, separate taps or overlapping presses.
- **Combo any-order** (`A + B`): all keys held, pressed in any order. The default.
- **Combo in-order** (`A + B` ordered): all keys held, pressed in the stored order.

**Combo** triggers were called **parallel** triggers in the old algorithm and in the app. They
are renamed in this module because sequence keys can also be held at the same time, so
"parallel" no longer tells the two apart. What defines a combo is that all its keys are held
when it fires.

## Core rules

The cases below only apply these.

1. **R1: A trigger completes on a key-down.** Key-ups only resolve the click type (short vs long
   press).
   Issues: #963 (open), #1095 (open), #2041 (closed).
2. **R2: Each key-down completes at most one trigger.** If several complete on the same key-down,
   one winner is picked (R3–R5) and the others are dropped.
   Issues: #407 (closed), #653 (closed), #1781 (open).
3. **R3: Combo beats sequence** when all of the combo trigger's keys are held at that
   key-down.
   Issues: #407 (closed), #1386 (closed).
4. **R4: More keys win.** A superset combo trigger beats a subset one, and a longer sequence
   beats a shorter one it contains.
   Issues: #407 (closed), #571 (closed), #653 (closed), #1781 (open), #804 (open),
   #1508 (open), #1526 (open).
5. **R5: In-order beats any-order.** An ordering generated from an any-order trigger is dropped
   when the user explicitly created the same in-order trigger, so only the explicit one fires.
   Issues: #591 (open).
6. **R6: Claimed downs.** A trigger can't count a key-down that already completed another trigger.
   The key has to be pressed again. Keys that are only held (Ctrl in `Ctrl + 1`) aren't claimed,
   so `Ctrl + 1` then `Ctrl + 2` both fire while Ctrl stays held.
   Issues: #2236 (open: the down that completed a double press also fires the single press).
7. **R7: Wait if something longer could still match.** A sequence (or single key) whose trie node
   has children waits for the next key or the timeout. If its key is still held when the wait
   ends, it fires then. Combo triggers never wait. Children whose triggers all have
   unsatisfied constraints don't cause a wait.
   Issues: #1386 (closed), #521 (closed), #804 (open), #1508 (open), #1526 (open), #1095 (open),
   #2041 (closed). Constraints: #1271 (closed), #2197, #2225, #2274, #2303 (all open).
8. **R8: The first divergent key commits** a sequence to one branch of the trie.
   Issues: none.
9. **R9: Identical triggers are merged** in preprocessing into one trigger that performs every key
   map's actions. Any-order combo triggers with the same set of keys expand to identical
   orderings, so they merge. Key maps that differ in click type or long-press delay aren't
   identical.
   Issues: #591 (open), #2094 (open: one key map per order variant of `Ctrl + Shift + V`),
   #693 (closed: same trigger, different long-press delays).
10. **R10: Wait for release if a combo trigger could still complete.** A single or sequence
    trigger completed by a key-down waits if that key-down could still become part of an
    in-order combo trigger: the trigger contains the key and all keys before it are held.
    For an expanded any-order trigger, the ordering that starts with the key always qualifies,
    so any-order needs no special case. If the combo trigger completes, it wins (R3). If the
    key is released first, the trigger fires on the release, with the action sent as an
    immediate down/up.
    Doesn't apply to combo triggers (`Ctrl + 1` vs `Ctrl + Shift + 1`).
    Issues: none. Needed because of any-order combo triggers (#591).

Each case below is a table of the key presses, what fires, and the rule that decides it.

## Single vs single

### `A`, `A`

```
| Input | Fires              | Rule |
|-------|--------------------|------|
| A     | one merged trigger | R9   |
```

## Single vs sequence

### `A`, `A → B` (single is the first key)

```
| Input               | Fires                   | Rule       |
|---------------------|-------------------------|------------|
| A, B within timeout | A → B                   | R7 (#1386) |
| A, timeout          | A after the timeout     | R7         |
| hold A past timeout | A when the timeout ends | R7         |
```

Today the last case never fires.

### `B`, `A → B` (single is a later key)

```
| Input   | Fires         | Rule                                     |
|---------|---------------|------------------------------------------|
| B alone | B immediately | R7 doesn't apply: root B has no children |
| A B     | A → B         | R4                                       |
```

Test: `Do not trigger both triggers if sequence trigger is triggered while waiting`.

### `A`, `A → A` (double press)

```
| Input                            | Fires | Rule                      |
|----------------------------------|-------|---------------------------|
| A, A within double-press timeout | A → A | R7, short per-gap timeout |
| A, timeout                       | A     | R7                        |
```

## Single vs combo

### `A`, `Shift + A` (any order)

```
| Input     | Fires                    | Rule          |
|-----------|--------------------------|---------------|
| Shift↓ A↓ | Shift + A                | R3, R4        |
| A↓ Shift↓ | Shift + A                | R10: A waited |
| A↓ A↑     | A on A↑, sent as down/up | R10           |
```

Test: `KeyMapAlgorithmTest.kt:3572`.

### `A`, `Shift + A` (in order)

```
| Input | Fires         | Rule                                |
|-------|---------------|-------------------------------------|
| A↓    | A immediately | R10 doesn't apply: Shift isn't held |
```

### `A`, `A + B` (in order)

```
| Input | Fires   | Rule                    |
|-------|---------|-------------------------|
| A↓ A↑ | A on A↑ | R10: A is the first key |
```

### `Vol down`, `Vol up + Vol down` (in order)

```
| Input             | Fires                | Rule                                 |
|-------------------|----------------------|--------------------------------------|
| Vol up↓ Vol down↓ | Vol up + Vol down    | R4                                   |
| Vol down alone    | Vol down immediately | R10 doesn't apply: Vol up isn't held |
```

Test: `overlapping triggers 3`.

Because a waiting single key fires on release as a down/up, a hold-down or repeat action on `A`
doesn't work while `A` is part of a combo trigger that could still complete.

## Sequence vs sequence

### `A → B`, `A → B → C` (prefix)

```
| Input        | Fires     | Rule          |
|--------------|-----------|---------------|
| A B C        | A → B → C | R4, R7 (#407) |
| A B, timeout | A → B     | R7            |
```

### `A → B → C`, `A → D → E` (shared prefix, then diverge)

```
| Input     | Fires          | Rule                       |
|-----------|----------------|----------------------------|
| A B C     | A → B → C      | R8, no wait                |
| A D E     | A → D → E      | R8, no wait                |
| A B D C E | A → B → C only | R8: B committed the branch |
```

### `B → C`, `A → B → C` (suffix)

```
| Input | Fires     | Rule                    |
|-------|-----------|-------------------------|
| A B C | A → B → C | R4: both complete on C↓ |
| B C   | B → C     |                         |
```

### `A → B`, `B → A` (same keys, different order)

```
| Input | Fires | Rule              |
|-------|-------|-------------------|
| A B   | A → B | key-down order    |
| B A   | B → A | key-down order    |
| A B A | A → B | R6: B↓ is claimed |
```

### `A → A`, `A → A → A` (repeated key)

```
| Input | Fires     | Rule                 |
|-------|-----------|----------------------|
| A A A | A → A → A | R4, R7 (#804, #1526) |
```

## Sequence vs combo

Any-order and in-order combo triggers differ only when the keys are pressed in the "wrong"
order.

### `A → B`, `A + B` (same keys, same order)

```
| Input                     | Fires       | Rule                                 |
|---------------------------|-------------|--------------------------------------|
| A↓ B↓ (A held)            | A + B       | R3                                   |
| A↓ A↑ B↓, A + B any-order | A → B on B↑ | R10: A could be pressed again        |
| A↓ A↑ B↓, A + B in-order  | A → B on B↓ | R10 doesn't apply: A must come first |
```

### `A → B`, `B + C` (sequence's last key starts a combo trigger)

```
| Input   | Fires       | Rule |
|---------|-------------|------|
| A B↓ B↑ | A → B on B↑ | R10  |
| A B↓ C↓ | B + C       | R3   |
```

### `A → B`, `B + A` (in order, opposite order)

```
| Input | Fires | Rule                                     |
|-------|-------|------------------------------------------|
| A↓ B↓ | A → B | in-order doesn't match, overlap accepted |
| B↓ A↓ | B + A | R3                                       |
```

### `A → B`, `A + B` (any order, opposite order)

```
| Input | Fires | Rule                   |
|-------|-------|------------------------|
| B↓ A↓ | A + B | sequence doesn't match |
```

### `A + B`, `A → B → C` (combo is a prefix of the sequence)

```
| Input             | Fires                     | Rule   |
|-------------------|---------------------------|--------|
| A↓ B↓ (A held), C | A + B on B↓, then nothing | R3, R6 |
| A B C (taps)      | A → B → C                 |        |
```

The sequence must be tapped separately.

### `Shift + A`, `A → B` (combo ends with a sequence key)

```
| Input        | Fires                   | Rule              |
|--------------|-------------------------|-------------------|
| Shift↓ A↓, B | Shift + A, then nothing | R6: A↓ is claimed |
| A B          | A → B                   |                   |
```

### `Shift + B`, `A → B` (combo ends with a later sequence key)

```
| Input        | Fires     | Rule                            |
|--------------|-----------|---------------------------------|
| A, Shift↓ B↓ | Shift + B | R3: combo wins on the same down |
```

## Combo vs combo

### `Ctrl + 1`, `Ctrl + Shift + 1` (subset)

```
| Input                 | Fires            | Rule              |
|-----------------------|------------------|-------------------|
| Ctrl↓ Shift↓ 1↓       | Ctrl + Shift + 1 | R4                |
| Ctrl↓ 1↓ Shift↓ (any) | Ctrl + 1 only    | R6: 1↓ is claimed |
```

Test: `overlapping triggers 1`.

### `Ctrl + 1`, `Ctrl + 2` (shared held key)

```
| Input          | Fires | Rule                        |
|----------------|-------|-----------------------------|
| Ctrl↓ 1↓ 1↑ 2↓ | both  | R6: Ctrl↓ completed nothing |
```

### `Ctrl + 1` (re-press)

```
| Input          | Fires          | Rule       |
|----------------|----------------|------------|
| Ctrl↓ 1↓ 1↑ 1↓ | Ctrl + 1 twice | R6: new 1↓ |
```

### `A + B`, `B + C` (partial overlap)

```
| Input    | Fires      | Rule              |
|----------|------------|-------------------|
| A↓ B↓ C↓ | A + B only | R6: B↓ is claimed |
```

### `A + B`, `B + A` (both in order)

```
| Input | Fires | Rule         |
|-------|-------|--------------|
| A↓ B↓ | A + B | order (#591) |
| B↓ A↓ | B + A | order (#591) |
```

### `A + B` (any order), `B + A` (in order)

```
| Input | Fires | Rule |
|-------|-------|------|
| B↓ A↓ | B + A | R5   |
| A↓ B↓ | A + B |      |
```

### `A + B`, `B + A` (both any order)

```
| Input        | Fires              | Rule |
|--------------|--------------------|------|
| either order | one merged trigger | R9   |
```

Migration turns on in-order for these to keep them apart.

## Long press

A long-press step is a different trie edge (`B` and `B (long)`). On `B↓` the cursor branches,
and the release or the long-press delay picks the branch. Combinations of hold lengths with the
types above follow the same cases, deciding on the resolved click type instead of the key-down.
