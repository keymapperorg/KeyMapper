# Terminology


| Term            | Meaning                                                                                                                   |
|-----------------|---------------------------------------------------------------------------------------------------------------------------|
| `KeyMap`        | A `Trigger` together with the `Action`s it fires and the `Condition`s that must be met.                                   |
| `Trigger`       | The input that activates a key map. It is either a `Combo` or a `Sequence` of `Input`s.                                   |
| `Input`         | One input, such as a key or button, with a hold duration. A tap is a hold of 0.                                           |
| `Combo`         | `Input`s that must all be pressed together (parallel trigger).                                                            |
| `Sequence`      | `Input`s pressed one after another.                                                                                       |
| `Action`        | An opaque id the engine hands back to the caller to perform.                                                              |
| `ActionPattern` | How an `Action` behaves over time: repeating, holding down, etc.                                                          |
| `Signal`        | What the engine emits for an `Action`: `Down` or `Up`. A one-shot action, including each repeat, is a `Down` followed immediately by an `Up`. |
| `Condition`     | A requirement for a key map to be active. The caller evaluates it.                                                        |
| `Consume`       | Whether the engine swallowed an input event, so the caller should not pass it on.                                         |
