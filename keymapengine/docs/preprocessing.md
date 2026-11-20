# Preprocessing

Done when key maps change, never per key event.

1. **Expand any-order combo triggers** into one in-order trigger per ordering of their keys,
   all with the same actions. Any-order `A + B` becomes in-order `A + B` and `B + A`. The
   matching algorithm only knows in-order combo triggers.
2. **Drop generated orderings that collide with an explicit in-order trigger** (R5).
3. **Merge identical triggers** (R9).
4. **Build the sequence trie** and an index from each key to the combo triggers that contain it.

Expansion produces n! orderings: 6 for 3 keys, 24 for 4, 120 for 5. Cap it (for example at 5
keys).
