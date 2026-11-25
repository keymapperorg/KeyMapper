use std::cmp::Reverse;

use crate::cursor::Cursor;

/// Remove canceled cursors, then deduplicate. If multiple point to the same
/// node then keep the most recent one.
pub fn cleanup_cursors(cursors: &mut Vec<Cursor>) {
    // Filter first so a canceled cursor can't win dedup and take an active
    // cursor on the same node down with it.
    cursors.retain(|c| !c.is_canceled());

    // Sorting by node makes duplicates adjacent so dedup can remove them,
    // and newest-first within a node means dedup keeps the most recent.
    cursors.sort_by_key(|c| (c.node(), Reverse(c.first_down_time())));
    cursors.dedup_by_key(|c| c.node());
}

#[cfg(test)]
mod tests {
    use std::{collections::HashMap, time::Duration};

    use proptest::{collection::vec, prelude::*};
    use slotmap::SlotMap;
    use tokio::time::Instant;

    use super::cleanup_cursors;
    use crate::{
        cursor::Cursor,
        device::DeviceHandle,
        input::{Input, InputRef},
        tree::NodeId,
    };

    /// Build cursors from (node index, input times in ms, canceled) tuples.
    /// An empty time list is a cursor that never advanced, e.g. the root cursor.
    fn build(specs: &[(usize, Vec<u64>, bool)]) -> Vec<Cursor> {
        let mut nodes: SlotMap<NodeId, ()> = SlotMap::default();
        let ids: Vec<NodeId> = (0..=specs.iter().map(|s| s.0).max().unwrap_or(0))
            .map(|_| nodes.insert(()))
            .collect();
        let base = Instant::now();

        specs
            .iter()
            .map(|(node, times, canceled)| {
                let mut cursor = Cursor::new(ids[*node]);
                for ms in times {
                    let time = base + Duration::from_millis(*ms);
                    cursor.advance(ids[*node], Input::at(InputRef(0), DeviceHandle(0), time));
                }
                if *canceled {
                    cursor.cancel();
                }
                cursor
            })
            .collect()
    }

    /// The simplest possible model: the newest first_down_time per node,
    /// ignoring canceled cursors. `None` sorts below `Some`, so a cursor that
    /// has advanced beats one that hasn't.
    fn newest_active_per_node(cursors: &[Cursor]) -> HashMap<NodeId, Option<Instant>> {
        let mut newest = HashMap::new();
        for c in cursors.iter().filter(|c| !c.is_canceled()) {
            let entry = newest.entry(c.node()).or_insert(c.first_down_time());
            *entry = (*entry).max(c.first_down_time());
        }
        newest
    }

    #[test]
    fn empty_stays_empty() {
        let mut cursors = Vec::new();
        cleanup_cursors(&mut cursors);
        assert!(cursors.is_empty());
    }

    #[test]
    fn removes_non_adjacent_duplicates() {
        // X@0, Y@1, X@2: the X duplicates are not adjacent when sorted by time.
        let mut cursors = build(&[
            (0, vec![0], false),
            (1, vec![1], false),
            (0, vec![2], false),
        ]);
        let expected = newest_active_per_node(&cursors);

        cleanup_cursors(&mut cursors);

        assert_eq!(cursors.len(), 2);
        assert_eq!(newest_active_per_node(&cursors), expected);
    }

    #[test]
    fn all_canceled_becomes_empty() {
        let mut cursors = build(&[(0, vec![0], true), (1, vec![], true)]);
        cleanup_cursors(&mut cursors);
        assert!(cursors.is_empty());
    }

    #[test]
    fn canceled_newest_does_not_remove_older_active_on_same_node() {
        let mut cursors = build(&[(0, vec![0], false), (0, vec![2], true)]);
        cleanup_cursors(&mut cursors);

        assert_eq!(cursors.len(), 1);
        assert!(!cursors[0].is_canceled());
    }

    proptest! {
        // Small node and time ranges force duplicates, ties and None/Some mixes.
        // Covers: empty, single, all-same-node, all-distinct, cursors that never
        // advanced (None time), equal times, multi-input cursors, and canceled
        // cursors mixed with active ones on the same node (including all canceled).
        #[test]
        fn keeps_exactly_the_newest_active_cursor_per_node(
            specs in vec(
                (0..4usize, vec(0..5u64, 0..3).prop_map(|mut t| { t.sort(); t }), any::<bool>()),
                0..20,
            )
        ) {
            let mut cursors = build(&specs);
            let expected = newest_active_per_node(&cursors);

            cleanup_cursors(&mut cursors);

            // No canceled cursor survives.
            prop_assert!(cursors.iter().all(|c| !c.is_canceled()));
            let actual = newest_active_per_node(&cursors);
            // One cursor per node: no duplicates survive.
            prop_assert_eq!(cursors.len(), actual.len());
            // Every node with an active cursor is kept, with its newest one.
            prop_assert_eq!(actual, expected);

            // Idempotent: running again changes nothing.
            let before: Vec<_> = cursors.iter().map(|c| (c.node(), c.first_down_time())).collect();
            cleanup_cursors(&mut cursors);
            let after: Vec<_> = cursors.iter().map(|c| (c.node(), c.first_down_time())).collect();
            prop_assert_eq!(before, after);
        }
    }
}
