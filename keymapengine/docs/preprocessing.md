# Preprocessing

Done when key maps change, never per key event. The steps run in this order. The rules (R5, R9)
are in [overlap-rules.md](overlap-rules.md).

1. **Drop disabled key maps.**
2. **Turn the vibrate and toast options into actions.** They are then performed like any other
   action, and a merged trigger (step 6) keeps each key map's options.
3. **Expand modifier keys into their left and right variants.** `Shift + A` becomes
   `Shift Left + A` and `Shift Right + A`. Drop a variant if the user explicitly mapped it
   separately: with `Shift Right + A` mapped to something else, `Shift + A` only expands to
   `Shift Left + A`.
4. **Expand any-order combo triggers** into one in-order trigger per ordering of their keys, all
   with the same actions. Any-order `A + B` becomes in-order `A + B` and `B + A`. The matching
   algorithm only knows in-order combo triggers.
5. **Drop orderings the user explicitly mapped** (R5). Any-order `A + B + C` doesn't expand to
   `C + B + A` if the user made an in-order `C + B + A` trigger.
6. **Merge key maps with the same trigger into one trigger** (R9). The trigger keeps a list of
   action patterns, one per key map, so firing it starts a separate action job for each key map.
   The key maps keep their own settings, such as the delay between actions.
7. **Create dummy key maps for imitating keys.** For each key that a sequence or combo trigger
   consumes but that has no single-key trigger of its own, add a dummy single-key trigger whose
   action sends the key again. If a sequence fails (`A`, timeout, with only `A → B` mapped),
   the dummy fires and the key isn't lost (#1057). Imitating is then ordinary matching instead
   of a special case.
8. **Build the sequence trie** and an index from each key to the combo triggers that contain it.

## Expansion size

Expansion produces n! orderings per any-order combo: 6 for 3 keys, 24 for 4, 120 for 5. Modifier
expansion multiplies this by 2 per modifier: any-order `Ctrl + Shift + Alt + A` is 2³ × 4! = 192
triggers. Cap the total per key map (for example at 5 keys). Above the cap, keep the trigger as a
key set and check it on demand instead of expanding.
