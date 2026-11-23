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
   beats a shorter one it contains. A combo whose keys are all held but which is still waiting
   for its long-press delay already counts: a subset that completes first defers to it.
   Issues: #407 (closed), #571 (closed), #653 (closed), #1781 (open), #804 (open),
   #1508 (open), #1526 (open).
5. **R5: In-order beats any-order.** An ordering generated from an any-order trigger is dropped
   when the user explicitly created the same in-order trigger, so only the explicit one fires.
   Issues: #591 (open).
6. **R6: A key press is only used once.** When a trigger fires, the key presses it matched can't
   count toward another trigger. That key has to be pressed again. A sequence uses all of its key
   presses. A combo uses only the press that completed it: its other keys are still held and can
   be part of the next combo, so with Ctrl held, `Ctrl + 1` then `Ctrl + 2` both fire.
   Issues: #2236 (open: the down that completed a double press also fires the single press).
7. **R7: Wait if something longer could still match.** A sequence (or single key) whose trie node
   has children waits for the next key or the timeout. If its key is still held when the wait
   ends, it fires then. Combo triggers never wait. Children whose triggers all have
   unsatisfied constraints don't cause a wait.
   Issues: #1386 (closed), #521 (closed), #804 (open), #1508 (open), #1526 (open), #1095 (open),
   #2041 (closed). Constraints: #1271 (closed), #2197, #2225, #2274, #2303 (all open).
8. **R8: Sequences match independently.** A key press that advances one sequence is ignored by
   the others, like any other key. With `A → B → C` and `A → D → E`, a stray B doesn't stop
   `A B D E` from firing `A → D → E`. When two sequences progress through the same key presses,
   R6 makes sure only the first to complete fires.
   Issues: none. Today each trigger already tracks its own position, so this is unchanged.
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

### Same key

```
Trigger 1: A
Trigger 2: A

| Input | Fires              | Rule |
|-------|--------------------|------|
| A     | one merged trigger | R9   |
```

## Single vs sequence

### Single is the first key

```
Trigger 1: A
Trigger 2: A → B

| Input               | Fires                   | Rule       |
|---------------------|-------------------------|------------|
| A, B within timeout | A → B                   | R7 (#1386) |
| A, timeout          | A after the timeout     | R7         |
| hold A past timeout | A when the timeout ends | R7         |
```

Today the last case never fires.

### Single is a later key

```
Trigger 1: B
Trigger 2: A → B

| Input   | Fires         | Rule                                     |
|---------|---------------|------------------------------------------|
| B alone | B immediately | R7 doesn't apply: root B has no children |
| A B     | A → B         | R4                                       |
```

Test: `Do not trigger both triggers if sequence trigger is triggered while waiting`.

### Double press

```
Trigger 1: A
Trigger 2: A → A

| Input                            | Fires | Rule                                   |
|----------------------------------|-------|----------------------------------------|
| A, A within double-press timeout | A → A | R7, A → A has the double-press timeout |
| A, timeout                       | A     | R7                                     |
```

## Single vs combo

### Single is a key of an any-order combo

```
Trigger 1: A
Trigger 2: Shift + A (any order)

| Input     | Fires                    | Rule          |
|-----------|--------------------------|---------------|
| Shift↓ A↓ | Shift + A                | R3, R4        |
| A↓ Shift↓ | Shift + A                | R10: A waited |
| A↓ A↑     | A on A↑, sent as down/up | R10           |
```

Test: `KeyMapAlgorithmTest.kt:3572`.

### Single is the last key of an in-order combo

```
Trigger 1: A
Trigger 2: Shift + A (in order)

| Input | Fires         | Rule                                |
|-------|---------------|-------------------------------------|
| A↓    | A immediately | R10 doesn't apply: Shift isn't held |
```

### Single is the first key of an in-order combo

```
Trigger 1: A
Trigger 2: A + B (in order)

| Input | Fires   | Rule                    |
|-------|---------|-------------------------|
| A↓ A↑ | A on A↑ | R10: A is the first key |
```

### Single is the last key of an in-order combo: volume keys

```
Trigger 1: Vol down
Trigger 2: Vol up + Vol down (in order)

| Input             | Fires                | Rule                                 |
|-------------------|----------------------|--------------------------------------|
| Vol up↓ Vol down↓ | Vol up + Vol down    | R4                                   |
| Vol down alone    | Vol down immediately | R10 doesn't apply: Vol up isn't held |
```

Test: `overlapping triggers 3`.

Because a waiting single key fires on release as a down/up, a hold-down or repeat action on `A`
doesn't work while `A` is part of a combo trigger that could still complete.

### Long-press single is a key of a long-press combo

```
Trigger 1: Power (long)
Trigger 2: Power (long) + Vol up (long)

| Input                                          | Fires               | Rule                       |
|------------------------------------------------|---------------------|----------------------------|
| Power↓, Vol up↓, both held past Vol up's delay | Power + Vol up only | R4: pending combo          |
| Power↓, Vol up↓, Vol up↑ before its delay      | Power on Vol up↑    | R4, then R7: still held    |
| Power↓ held past its delay, then Vol up↓       | Power, then nothing | R6: Power↓ is already used |
| Power↓ held past its delay, alone              | Power               |                            |
```

Issue: #1781 (open). Today both fire. The two triggers complete at different times: Power's
long press at `Power↓ + delay`, the combo's at `Vol up↓ + delay`, which is later. When the single
completes, the combo hasn't, so R4 has nothing to compare against. The fix is to treat a combo
as a competitor while it is pending, meaning all its keys are held and it's waiting for its
delay. When Power's long press completes, look up the combos containing Power in the key index
and check whether their other keys are held. That's the same lookup R10 does.

R10 (wait for release) would also prevent the double fire, but it would delay long press Power
until release whenever the combo could still complete, which defeats firing during the hold
(#1095). The pending check only defers when the combo is already underway.

## Sequence vs sequence

### Prefix

```
Trigger 1: A → B
Trigger 2: A → B → C

| Input        | Fires     | Rule          |
|--------------|-----------|---------------|
| A B C        | A → B → C | R4, R7 (#407) |
| A B, timeout | A → B     | R7            |
```

### Shared prefix, then diverge

```
Trigger 1: A → B → C
Trigger 2: A → D → E

| Input     | Fires          | Rule                       |
|-----------|----------------|----------------------------|
| A B C     | A → B → C      | no wait                    |
| A D E     | A → D → E      | no wait                    |
| A B D E   | A → D → E      | R8: B is ignored           |
| A B D C E | A → B → C only | R8, R6: A↓ is already used |
```

### Suffix

```
Trigger 1: B → C
Trigger 2: A → B → C

| Input | Fires     | Rule                    |
|-------|-----------|-------------------------|
| A B C | A → B → C | R4: both complete on C↓ |
| B C   | B → C     |                         |
```

### Same keys, different order

```
Trigger 1: A → B
Trigger 2: B → A

| Input | Fires | Rule                   |
|-------|-------|------------------------|
| A B   | A → B | key-down order         |
| B A   | B → A | key-down order         |
| A B A | A → B | R6: B↓ is already used |
```

### Repeated key

```
Trigger 1: A → A
Trigger 2: A → A → A

| Input | Fires     | Rule                 |
|-------|-----------|----------------------|
| A A A | A → A → A | R4, R7 (#804, #1526) |
```

## Sequence vs combo

Any-order and in-order combo triggers differ only when the keys are pressed in the "wrong"
order.

### Same keys, same order

```
Trigger 1: A → B
Trigger 2: A + B (any order or in order)

| Input                     | Fires       | Rule                                 |
|---------------------------|-------------|--------------------------------------|
| A↓ B↓ (A held)            | A + B       | R3                                   |
| A↓ A↑ B↓, A + B any-order | A → B on B↑ | R10: A could be pressed again        |
| A↓ A↑ B↓, A + B in-order  | A → B on B↓ | R10 doesn't apply: A must come first |
```

### Sequence's last key starts a combo

```
Trigger 1: A → B
Trigger 2: B + C (any order or in order)

| Input   | Fires       | Rule |
|---------|-------------|------|
| A B↓ B↑ | A → B on B↑ | R10  |
| A B↓ C↓ | B + C       | R3   |
```

### Same keys, opposite order, in-order combo

```
Trigger 1: A → B
Trigger 2: B + A (in order)

| Input | Fires | Rule                                     |
|-------|-------|------------------------------------------|
| A↓ B↓ | A → B | in-order doesn't match, overlap accepted |
| B↓ A↓ | B + A | R3                                       |
```

### Same keys, opposite order, any-order combo

```
Trigger 1: A → B
Trigger 2: A + B (any order)

| Input | Fires | Rule                   |
|-------|-------|------------------------|
| B↓ A↓ | A + B | sequence doesn't match |
```

### Combo is a prefix of the sequence

```
Trigger 1: A + B
Trigger 2: A → B → C

| Input                    | Fires                       | Rule                       |
|--------------------------|-----------------------------|----------------------------|
| A↓ B↓ (A held), C        | A + B on B↓, then nothing   | R3, R6: B↓ is already used |
| A↓ B↓ (A held), then B C | A + B on B↓, then A → B → C | R6: the combo used only B↓ |
| A B C (taps)             | A → B → C                   |                            |
```

Holding A and pressing B fires the combo, which uses only B↓. The sequence's cursor at A
survives, so pressing B again and then C within the timeout still completes `A → B → C`.

### Combo ends with a sequence key

```
Trigger 1: Shift + A
Trigger 2: A → B

| Input        | Fires                   | Rule                   |
|--------------|-------------------------|------------------------|
| Shift↓ A↓, B | Shift + A, then nothing | R6: A↓ is already used |
| A B          | A → B                   |                        |
```

### Combo ends with a later sequence key

```
Trigger 1: Shift + B
Trigger 2: A → B

| Input        | Fires     | Rule                            |
|--------------|-----------|---------------------------------|
| A, Shift↓ B↓ | Shift + B | R3: combo wins on the same down |
```

## Combo vs combo

### Subset

```
Trigger 1: Ctrl + 1
Trigger 2: Ctrl + Shift + 1

| Input                 | Fires            | Rule                   |
|-----------------------|------------------|------------------------|
| Ctrl↓ Shift↓ 1↓       | Ctrl + Shift + 1 | R4                     |
| Ctrl↓ 1↓ Shift↓ (any) | Ctrl + 1 only    | R6: 1↓ is already used |
```

Test: `overlapping triggers 1`.

### Shared held key

```
Trigger 1: Ctrl + 1
Trigger 2: Ctrl + 2

| Input          | Fires | Rule                        |
|----------------|-------|-----------------------------|
| Ctrl↓ 1↓ 1↑ 2↓ | both  | R6: Ctrl↓ is held, not used |
```

### Re-press

```
Trigger: Ctrl + 1

| Input          | Fires          | Rule       |
|----------------|----------------|------------|
| Ctrl↓ 1↓ 1↑ 1↓ | Ctrl + 1 twice | R6: new 1↓ |
```

### Partial overlap

```
Trigger 1: A + B
Trigger 2: B + C

| Input    | Fires      | Rule                   |
|----------|------------|------------------------|
| A↓ B↓ C↓ | A + B only | R6: B↓ is already used |
```

### Same keys, both in order

```
Trigger 1: A + B (in order)
Trigger 2: B + A (in order)

| Input | Fires | Rule         |
|-------|-------|--------------|
| A↓ B↓ | A + B | order (#591) |
| B↓ A↓ | B + A | order (#591) |
```

### Same keys, any order vs in order

```
Trigger 1: A + B (any order)
Trigger 2: B + A (in order)

| Input | Fires | Rule |
|-------|-------|------|
| B↓ A↓ | B + A | R5   |
| A↓ B↓ | A + B |      |
```

### Same keys, both any order

```
Trigger 1: A + B (any order)
Trigger 2: B + A (any order)

| Input        | Fires              | Rule |
|--------------|--------------------|------|
| either order | one merged trigger | R9   |
```

Migration turns on in-order for these to keep them apart.

## Long press

A long-press step is a different trie edge (`B` and `B (long)`). On `B↓` the cursor branches,
and the release or the long-press delay picks the branch. Combinations of hold lengths with the
types above follow the same cases, deciding on the resolved click type instead of the key-down.
