mod models;
mod search;

use std::collections::HashSet;
use std::path::Path;

use anyhow::{anyhow, Context, Result};
use chrono::{TimeZone, Utc};
use rusqlite::{params, Connection, OptionalExtension};
use uuid::Uuid;

pub use models::{Memo, MemoEvent, MemoEventType, MemoStatus, ReviewSnapshot, SearchResult};
pub use search::SearchEngine;

pub struct MemoService {
    conn: Connection,
}

impl MemoService {
    pub fn open(path: impl AsRef<Path>) -> Result<Self> {
        let conn = Connection::open(path.as_ref()).with_context(|| {
            format!(
                "failed to open memo database at {}",
                path.as_ref().display()
            )
        })?;
        let mut service = Self { conn };
        service.init_schema()?;
        Ok(service)
    }

    pub fn open_in_memory() -> Result<Self> {
        let conn = Connection::open_in_memory().context("failed to open in-memory database")?;
        let mut service = Self { conn };
        service.init_schema()?;
        Ok(service)
    }

    pub fn add_memo(&mut self, content: &str) -> Result<Memo> {
        self.add_memo_at(content, now_ms())
    }

    pub fn add_memo_at(&mut self, content: &str, at_ms: i64) -> Result<Memo> {
        let content = normalize_content(content)?;
        let memo = Memo {
            id: Uuid::new_v4().to_string(),
            content: content.to_string(),
            status: MemoStatus::Active,
            created_at_ms: at_ms,
            updated_at_ms: at_ms,
            cleared_at_ms: None,
            reminder_count: 0,
            last_reviewed_at_ms: None,
            display_order: at_ms,
            pinned: false,
        };

        let tx = self
            .conn
            .transaction()
            .context("failed to start transaction")?;
        tx.execute(
            "INSERT INTO memos (
                id,
                content,
                status,
                created_at_ms,
                updated_at_ms,
                cleared_at_ms,
                reminder_count,
                last_reviewed_at_ms,
                display_order,
                pinned
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            params![
                &memo.id,
                &memo.content,
                memo.status.as_str(),
                memo.created_at_ms,
                memo.updated_at_ms,
                memo.cleared_at_ms,
                memo.reminder_count,
                memo.last_reviewed_at_ms,
                memo.display_order,
                memo.pinned as i64
            ],
        )
        .context("failed to insert memo")?;
        Self::insert_event_tx(
            &tx,
            &memo.id,
            MemoEventType::Created,
            &memo.content,
            None,
            at_ms,
        )?;
        tx.commit().context("failed to commit insert memo")?;

        Ok(memo)
    }

    pub fn update_memo(&mut self, memo_id: &str, content: &str) -> Result<Memo> {
        self.update_memo_at(memo_id, content, now_ms())
    }

    pub fn update_memo_at(&mut self, memo_id: &str, content: &str, at_ms: i64) -> Result<Memo> {
        let content = normalize_content(content)?;
        let existing = self.get_memo(memo_id)?;

        let tx = self
            .conn
            .transaction()
            .context("failed to start transaction")?;
        tx.execute(
            "UPDATE memos
             SET content = ?, updated_at_ms = ?
             WHERE id = ?",
            params![content, at_ms, memo_id],
        )
        .with_context(|| format!("failed to update memo {memo_id}"))?;
        Self::insert_event_tx(
            &tx,
            memo_id,
            MemoEventType::Edited,
            content,
            Some(existing.content.as_str()),
            at_ms,
        )?;
        tx.commit().context("failed to commit memo update")?;

        self.get_memo(memo_id)
    }

    pub fn keep_memo(&mut self, memo_id: &str) -> Result<Memo> {
        self.keep_memo_at(memo_id, now_ms())
    }

    pub fn keep_memo_at(&mut self, memo_id: &str, at_ms: i64) -> Result<Memo> {
        let existing = self.get_memo(memo_id)?;
        if existing.status == MemoStatus::Cleared {
            return Err(anyhow!("memo {memo_id} is already cleared"));
        }

        let tx = self
            .conn
            .transaction()
            .context("failed to start transaction")?;
        tx.execute(
            "UPDATE memos
             SET updated_at_ms = ?,
                 reminder_count = reminder_count + 1,
                 last_reviewed_at_ms = ?
             WHERE id = ?",
            params![at_ms, at_ms, memo_id],
        )
        .with_context(|| format!("failed to keep memo {memo_id}"))?;
        Self::insert_event_tx(
            &tx,
            memo_id,
            MemoEventType::Kept,
            &existing.content,
            Some("still active"),
            at_ms,
        )?;
        tx.commit().context("failed to commit keep memo")?;

        self.get_memo(memo_id)
    }

    pub fn clear_memo(&mut self, memo_id: &str) -> Result<Memo> {
        self.clear_memo_at(memo_id, now_ms())
    }

    pub fn clear_memo_at(&mut self, memo_id: &str, at_ms: i64) -> Result<Memo> {
        let existing = self.get_memo(memo_id)?;
        if existing.status == MemoStatus::Cleared {
            return Ok(existing);
        }

        let tx = self
            .conn
            .transaction()
            .context("failed to start transaction")?;
        tx.execute(
            "UPDATE memos
             SET status = ?,
                 updated_at_ms = ?,
                 cleared_at_ms = ?,
                 reminder_count = reminder_count + 1,
                 last_reviewed_at_ms = ?
             WHERE id = ?",
            params![MemoStatus::Cleared.as_str(), at_ms, at_ms, at_ms, memo_id],
        )
        .with_context(|| format!("failed to clear memo {memo_id}"))?;
        Self::insert_event_tx(
            &tx,
            memo_id,
            MemoEventType::Cleared,
            &existing.content,
            Some("cleared by user"),
            at_ms,
        )?;
        tx.commit().context("failed to commit clear memo")?;

        self.get_memo(memo_id)
    }

    pub fn reopen_memo(&mut self, memo_id: &str) -> Result<Memo> {
        self.reopen_memo_at(memo_id, now_ms())
    }

    pub fn reopen_memo_at(&mut self, memo_id: &str, at_ms: i64) -> Result<Memo> {
        let existing = self.get_memo(memo_id)?;
        if existing.status == MemoStatus::Active {
            return Ok(existing);
        }

        let tx = self
            .conn
            .transaction()
            .context("failed to start transaction")?;
        tx.execute(
            "UPDATE memos
             SET status = ?,
                 updated_at_ms = ?,
                 cleared_at_ms = NULL,
                 last_reviewed_at_ms = NULL,
                 display_order = ?
             WHERE id = ?",
            params![MemoStatus::Active.as_str(), at_ms, at_ms, memo_id],
        )
        .with_context(|| format!("failed to reopen memo {memo_id}"))?;
        Self::insert_event_tx(
            &tx,
            memo_id,
            MemoEventType::Reopened,
            &existing.content,
            Some("reopened for reminders"),
            at_ms,
        )?;
        tx.commit().context("failed to commit reopen memo")?;

        self.get_memo(memo_id)
    }

    pub fn set_memo_pinned(&mut self, memo_id: &str, pinned: bool) -> Result<Memo> {
        self.set_memo_pinned_at(memo_id, pinned, now_ms())
    }

    pub fn set_memo_pinned_at(
        &mut self,
        memo_id: &str,
        pinned: bool,
        at_ms: i64,
    ) -> Result<Memo> {
        let existing = self.get_memo(memo_id)?;
        if existing.pinned == pinned {
            return Ok(existing);
        }

        self.conn
            .execute(
                "UPDATE memos
                 SET pinned = ?,
                     display_order = ?,
                     updated_at_ms = ?
                 WHERE id = ?",
                params![pinned as i64, at_ms, at_ms, memo_id],
            )
            .with_context(|| format!("failed to update pin state for memo {memo_id}"))?;

        self.get_memo(memo_id)
    }

    pub fn delete_memo(&mut self, memo_id: &str) -> Result<()> {
        self.delete_memo_at(memo_id, now_ms())
    }

    pub fn delete_memo_at(&mut self, memo_id: &str, _at_ms: i64) -> Result<()> {
        self.get_memo(memo_id)?;

        let tx = self
            .conn
            .transaction()
            .context("failed to start transaction")?;
        tx.execute(
            "DELETE FROM memo_events
             WHERE memo_id = ?",
            params![memo_id],
        )
        .with_context(|| format!("failed to delete events for memo {memo_id}"))?;
        tx.execute(
            "DELETE FROM memos
             WHERE id = ?",
            params![memo_id],
        )
        .with_context(|| format!("failed to delete memo {memo_id}"))?;
        tx.commit().context("failed to commit memo deletion")?;

        Ok(())
    }

    pub fn apply_remote_memo(&mut self, memo: &Memo) -> Result<Memo> {
        normalize_content(&memo.content)?;
        let remote_display_order = normalized_display_order(memo);

        if let Some(existing) = self.get_memo_optional(&memo.id)? {
            let remote_order_is_newer = remote_display_order > existing.display_order;
            if existing.updated_at_ms > memo.updated_at_ms && !remote_order_is_newer {
                return Ok(existing);
            }

            let remote_content_is_newer = memo.updated_at_ms >= existing.updated_at_ms;
            let content = if remote_content_is_newer {
                memo.content.as_str()
            } else {
                existing.content.as_str()
            };
            let status = if remote_content_is_newer {
                memo.status.as_str()
            } else {
                existing.status.as_str()
            };
            let created_at_ms = if remote_content_is_newer {
                memo.created_at_ms
            } else {
                existing.created_at_ms
            };
            let updated_at_ms = if remote_content_is_newer {
                memo.updated_at_ms
            } else {
                existing.updated_at_ms
            };
            let cleared_at_ms = if remote_content_is_newer {
                memo.cleared_at_ms
            } else {
                existing.cleared_at_ms
            };
            let reminder_count = if remote_content_is_newer {
                memo.reminder_count
            } else {
                existing.reminder_count
            };
            let last_reviewed_at_ms = if remote_content_is_newer {
                memo.last_reviewed_at_ms
            } else {
                existing.last_reviewed_at_ms
            };
            let display_order = if remote_order_is_newer {
                remote_display_order
            } else {
                existing.display_order
            };
            let pinned = if remote_order_is_newer {
                memo.pinned
            } else {
                existing.pinned
            };

            self.conn
                .execute(
                    "UPDATE memos
                     SET content = ?,
                         status = ?,
                         created_at_ms = ?,
                         updated_at_ms = ?,
                         cleared_at_ms = ?,
                         reminder_count = ?,
                         last_reviewed_at_ms = ?,
                         display_order = ?,
                         pinned = ?
                     WHERE id = ?",
                    params![
                        content,
                        status,
                        created_at_ms,
                        updated_at_ms,
                        cleared_at_ms,
                        reminder_count,
                        last_reviewed_at_ms,
                        display_order,
                        pinned as i64,
                        &memo.id
                    ],
                )
                .with_context(|| format!("failed to apply remote memo {}", memo.id))?;
        } else {
            self.conn
                .execute(
                    "INSERT INTO memos (
                        id,
                        content,
                        status,
                        created_at_ms,
                        updated_at_ms,
                        cleared_at_ms,
                        reminder_count,
                        last_reviewed_at_ms,
                        display_order,
                        pinned
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    params![
                        &memo.id,
                        &memo.content,
                        memo.status.as_str(),
                        memo.created_at_ms,
                        memo.updated_at_ms,
                        memo.cleared_at_ms,
                        memo.reminder_count,
                        memo.last_reviewed_at_ms,
                        remote_display_order,
                        memo.pinned as i64
                    ],
                )
                .with_context(|| format!("failed to insert remote memo {}", memo.id))?;
        }

        self.get_memo(&memo.id)
    }

    pub fn delete_remote_memo(&mut self, memo_id: &str) -> Result<()> {
        self.delete_remote_memo_at(memo_id, i64::MAX)
    }

    pub fn delete_remote_memo_at(&mut self, memo_id: &str, deleted_at_ms: i64) -> Result<()> {
        let Some(existing) = self.get_memo_optional(memo_id)? else {
            return Ok(());
        };
        if existing.updated_at_ms > deleted_at_ms {
            return Ok(());
        }
        self.delete_memo(memo_id)
    }

    pub fn reorder_active_memos(&mut self, ordered_ids: &[String]) -> Result<Vec<Memo>> {
        let active = self.list_active_memos()?;
        if active.is_empty() {
            return Ok(active);
        }

        let active_ids: HashSet<&str> = active.iter().map(|memo| memo.id.as_str()).collect();
        let mut seen = HashSet::new();
        let mut ordered = Vec::new();

        for memo_id in ordered_ids {
            if active_ids.contains(memo_id.as_str()) && seen.insert(memo_id.clone()) {
                ordered.push(memo_id.clone());
            }
        }

        for memo in active {
            if seen.insert(memo.id.clone()) {
                ordered.push(memo.id);
            }
        }

        let now = now_ms();
        let tx = self
            .conn
            .transaction()
            .context("failed to start transaction")?;
        for (offset, memo_id) in ordered.iter().enumerate() {
            tx.execute(
                "UPDATE memos
                 SET display_order = ?
                 WHERE id = ? AND status = 'active'",
                params![now - offset as i64, memo_id],
            )
            .with_context(|| format!("failed to reorder memo {memo_id}"))?;
        }
        tx.commit().context("failed to commit memo reorder")?;

        self.list_active_memos()
    }

    pub fn get_memo(&self, memo_id: &str) -> Result<Memo> {
        self.get_memo_optional(memo_id)?
            .ok_or_else(|| anyhow!("memo {memo_id} not found"))
    }

    pub fn list_active_memos(&self) -> Result<Vec<Memo>> {
        self.query_memos(
            "SELECT * FROM memos
             WHERE status = 'active'
             ORDER BY pinned DESC, display_order DESC, updated_at_ms DESC",
        )
    }

    pub fn list_all_memos(&self) -> Result<Vec<Memo>> {
        self.query_memos("SELECT * FROM memos ORDER BY updated_at_ms DESC")
    }

    pub fn review_snapshot(&self) -> Result<ReviewSnapshot> {
        self.review_snapshot_at(now_ms())
    }

    pub fn review_snapshot_at(&self, at_ms: i64) -> Result<ReviewSnapshot> {
        let active = self.list_active_memos()?;
        let due: Vec<Memo> = active
            .iter()
            .filter(|memo| is_review_due(memo.last_reviewed_at_ms, at_ms))
            .cloned()
            .collect();

        Ok(ReviewSnapshot {
            due_count: due.len(),
            active_count: active.len(),
            due,
            generated_at_ms: at_ms,
        })
    }

    pub fn search_memos(&self, query: &str, limit: usize) -> Result<Vec<SearchResult>> {
        let memos = self.list_all_memos()?;
        Ok(SearchEngine::search(query, &memos, limit))
    }

    pub fn list_events(&self, limit: usize) -> Result<Vec<MemoEvent>> {
        let mut statement = self
            .conn
            .prepare(
                "SELECT
                    id,
                    memo_id,
                    event_type,
                    content_snapshot,
                    note,
                    created_at_ms
                 FROM memo_events
                 ORDER BY created_at_ms DESC
                 LIMIT ?",
            )
            .context("failed to prepare list events query")?;
        let rows = statement
            .query_map(params![limit as i64], Self::map_event_row)
            .context("failed to query events")?;

        let mut events = Vec::new();
        for row in rows {
            events.push(row.context("failed to parse event row")?);
        }
        Ok(events)
    }

    fn init_schema(&mut self) -> Result<()> {
        self.conn
            .execute_batch(
                "CREATE TABLE IF NOT EXISTS memos (
                    id TEXT PRIMARY KEY,
                    content TEXT NOT NULL,
                    status TEXT NOT NULL,
                    created_at_ms INTEGER NOT NULL,
                    updated_at_ms INTEGER NOT NULL,
                    cleared_at_ms INTEGER,
                    reminder_count INTEGER NOT NULL DEFAULT 0,
                    last_reviewed_at_ms INTEGER,
                    display_order INTEGER NOT NULL DEFAULT 0,
                    pinned INTEGER NOT NULL DEFAULT 0
                );

                CREATE TABLE IF NOT EXISTS memo_events (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    memo_id TEXT NOT NULL,
                    event_type TEXT NOT NULL,
                    content_snapshot TEXT NOT NULL,
                    note TEXT,
                    created_at_ms INTEGER NOT NULL,
                    FOREIGN KEY (memo_id) REFERENCES memos(id)
                );

                CREATE INDEX IF NOT EXISTS idx_memos_status_created
                    ON memos(status, created_at_ms DESC);
                CREATE INDEX IF NOT EXISTS idx_memos_active_order
                    ON memos(status, pinned DESC, display_order DESC);
                CREATE INDEX IF NOT EXISTS idx_events_memo_created
                    ON memo_events(memo_id, created_at_ms DESC);",
            )
            .context("failed to initialize schema")?;
        self.ensure_memo_column("display_order", "INTEGER NOT NULL DEFAULT 0")?;
        self.ensure_memo_column("pinned", "INTEGER NOT NULL DEFAULT 0")?;
        self.conn
            .execute(
                "UPDATE memos
                 SET display_order = updated_at_ms
                 WHERE display_order = 0",
                [],
            )
            .context("failed to backfill memo order")?;
        Ok(())
    }

    fn ensure_memo_column(&self, name: &str, definition: &str) -> Result<()> {
        let mut statement = self
            .conn
            .prepare("PRAGMA table_info(memos)")
            .context("failed to inspect memo schema")?;
        let column_names = statement
            .query_map([], |row| row.get::<_, String>("name"))
            .context("failed to read memo schema")?
            .collect::<rusqlite::Result<Vec<_>>>()
            .context("failed to parse memo schema")?;
        if !column_names.iter().any(|column| column == name) {
            self.conn
                .execute(&format!("ALTER TABLE memos ADD COLUMN {name} {definition}"), [])
                .with_context(|| format!("failed to add memo column {name}"))?;
        }
        Ok(())
    }

    fn get_memo_optional(&self, memo_id: &str) -> Result<Option<Memo>> {
        let memo = self
            .conn
            .query_row(
                "SELECT * FROM memos WHERE id = ? LIMIT 1",
                params![memo_id],
                Self::map_memo_row,
            )
            .optional()
            .with_context(|| format!("failed to load memo {memo_id}"))?;
        Ok(memo)
    }

    fn query_memos(&self, sql: &str) -> Result<Vec<Memo>> {
        let mut statement = self
            .conn
            .prepare(sql)
            .context("failed to prepare memo query")?;
        let rows = statement
            .query_map([], Self::map_memo_row)
            .context("failed to query memos")?;

        let mut memos = Vec::new();
        for row in rows {
            memos.push(row.context("failed to parse memo row")?);
        }
        Ok(memos)
    }

    fn insert_event_tx(
        tx: &rusqlite::Transaction<'_>,
        memo_id: &str,
        event_type: MemoEventType,
        content_snapshot: &str,
        note: Option<&str>,
        at_ms: i64,
    ) -> Result<()> {
        tx.execute(
            "INSERT INTO memo_events (
                memo_id,
                event_type,
                content_snapshot,
                note,
                created_at_ms
            ) VALUES (?, ?, ?, ?, ?)",
            params![memo_id, event_type.as_str(), content_snapshot, note, at_ms],
        )
        .with_context(|| format!("failed to insert event for memo {memo_id}"))?;
        Ok(())
    }

    fn map_memo_row(row: &rusqlite::Row<'_>) -> rusqlite::Result<Memo> {
        Ok(Memo {
            id: row.get("id")?,
            content: row.get("content")?,
            status: MemoStatus::from_str(row.get::<_, String>("status")?.as_str()),
            created_at_ms: row.get("created_at_ms")?,
            updated_at_ms: row.get("updated_at_ms")?,
            cleared_at_ms: row.get("cleared_at_ms")?,
            reminder_count: row.get("reminder_count")?,
            last_reviewed_at_ms: row.get("last_reviewed_at_ms")?,
            display_order: row.get("display_order")?,
            pinned: row.get::<_, i64>("pinned")? != 0,
        })
    }

    fn map_event_row(row: &rusqlite::Row<'_>) -> rusqlite::Result<MemoEvent> {
        Ok(MemoEvent {
            id: row.get("id")?,
            memo_id: row.get("memo_id")?,
            event_type: MemoEventType::from_str(row.get::<_, String>("event_type")?.as_str()),
            content_snapshot: row.get("content_snapshot")?,
            note: row.get("note")?,
            created_at_ms: row.get("created_at_ms")?,
        })
    }
}

fn normalize_content(content: &str) -> Result<&str> {
    if content.trim().is_empty() {
        return Err(anyhow!("memo content cannot be blank"));
    }
    Ok(content)
}

fn normalized_display_order(memo: &Memo) -> i64 {
    if memo.display_order == 0 {
        memo.updated_at_ms
    } else {
        memo.display_order
    }
}

fn now_ms() -> i64 {
    Utc::now().timestamp_millis()
}

fn is_review_due(last_reviewed_at_ms: Option<i64>, now_ms: i64) -> bool {
    match last_reviewed_at_ms {
        None => true,
        Some(last_ms) => day_key(last_ms) != day_key(now_ms),
    }
}

fn day_key(timestamp_ms: i64) -> String {
    Utc.timestamp_millis_opt(timestamp_ms)
        .single()
        .map(|dt| dt.format("%F").to_string())
        .unwrap_or_else(|| "invalid-day".to_string())
}

#[cfg(test)]
mod tests {
    use super::*;
    use chrono::Utc;

    fn ts(year: i32, month: u32, day: u32, hour: u32, minute: u32) -> i64 {
        Utc.with_ymd_and_hms(year, month, day, hour, minute, 0)
            .single()
            .expect("valid timestamp")
            .timestamp_millis()
    }

    #[test]
    fn memo_lifecycle_is_logged() {
        let mut service = MemoService::open_in_memory().expect("service");
        let created_at = ts(2026, 3, 10, 9, 0);
        let updated_at = ts(2026, 3, 10, 9, 5);
        let kept_at = ts(2026, 3, 10, 21, 0);
        let cleared_at = ts(2026, 3, 11, 21, 0);

        let memo = service
            .add_memo_at("Call Mom tonight", created_at)
            .expect("create");
        let memo = service
            .update_memo_at(&memo.id, "Call Mom after dinner", updated_at)
            .expect("update");
        assert_eq!(memo.updated_at_ms, updated_at);

        let memo = service.keep_memo_at(&memo.id, kept_at).expect("keep");
        assert_eq!(memo.reminder_count, 1);

        let memo = service.clear_memo_at(&memo.id, cleared_at).expect("clear");
        assert_eq!(memo.status, MemoStatus::Cleared);
        assert_eq!(memo.cleared_at_ms, Some(cleared_at));

        let memo = service
            .reopen_memo_at(&memo.id, ts(2026, 3, 12, 8, 0))
            .expect("reopen");
        assert_eq!(memo.status, MemoStatus::Active);
        assert_eq!(memo.cleared_at_ms, None);

        let events = service.list_events(10).expect("events");
        assert_eq!(events.len(), 5);
        assert_eq!(events[0].event_type, MemoEventType::Reopened);
    }

    #[test]
    fn review_queue_only_returns_memos_not_processed_today() {
        let mut service = MemoService::open_in_memory().expect("service");
        let created_at = ts(2026, 3, 10, 8, 0);
        let review_time = ts(2026, 3, 10, 21, 0);
        let next_day = ts(2026, 3, 11, 21, 0);

        let memo = service
            .add_memo_at("Pay the utility bill by Friday", created_at)
            .expect("create");
        let snapshot = service.review_snapshot_at(review_time).expect("review");
        assert_eq!(snapshot.due_count, 1);

        service.keep_memo_at(&memo.id, review_time).expect("keep");
        let same_day_snapshot = service.review_snapshot_at(review_time).expect("review");
        assert_eq!(same_day_snapshot.due_count, 0);

        let next_day_snapshot = service.review_snapshot_at(next_day).expect("review");
        assert_eq!(next_day_snapshot.due_count, 1);
    }

    #[test]
    fn search_prefers_close_matches_for_short_memos() {
        let mut service = MemoService::open_in_memory().expect("service");
        service
            .add_memo_at("Product meeting tomorrow at ten", ts(2026, 3, 10, 8, 0))
            .expect("create");
        service
            .add_memo_at("Buy milk and eggs tonight", ts(2026, 3, 10, 8, 1))
            .expect("create");

        let results = service.search_memos("meeting", 10).expect("search");
        assert_eq!(results.len(), 1);
        assert!(results[0].memo.content.contains("meeting"));
    }

    #[test]
    fn deleting_a_memo_removes_it_and_its_history() {
        let mut service = MemoService::open_in_memory().expect("service");
        let memo = service
            .add_memo_at("Delete this memo later", ts(2026, 3, 10, 8, 0))
            .expect("create");
        service
            .clear_memo_at(&memo.id, ts(2026, 3, 10, 9, 0))
            .expect("clear");

        service
            .delete_memo_at(&memo.id, ts(2026, 3, 10, 10, 0))
            .expect("delete");

        assert!(service
            .get_memo_optional(&memo.id)
            .expect("lookup")
            .is_none());
        assert!(service.list_events(10).expect("events").is_empty());
    }

    #[test]
    fn applying_remote_memo_inserts_missing_memo() {
        let mut service = MemoService::open_in_memory().expect("service");
        let remote = Memo {
            id: "remote-a".to_string(),
            content: "Pay rent before Friday".to_string(),
            status: MemoStatus::Active,
            created_at_ms: ts(2026, 3, 10, 8, 0),
            updated_at_ms: ts(2026, 3, 10, 8, 0),
            cleared_at_ms: None,
            reminder_count: 0,
            last_reviewed_at_ms: None,
            display_order: ts(2026, 3, 10, 8, 0),
            pinned: false,
        };

        let applied = service.apply_remote_memo(&remote).expect("apply");

        assert_eq!(applied, remote);
        assert_eq!(service.get_memo("remote-a").expect("memo"), remote);
    }

    #[test]
    fn applying_remote_memo_keeps_newer_local_memo() {
        let mut service = MemoService::open_in_memory().expect("service");
        let memo = service
            .add_memo_at("Book hotel", ts(2026, 3, 10, 8, 0))
            .expect("create");
        service
            .update_memo_at(&memo.id, "Book hotel near station", ts(2026, 3, 10, 9, 0))
            .expect("update");
        let remote = Memo {
            id: memo.id.clone(),
            content: "Book hotel near airport".to_string(),
            status: MemoStatus::Active,
            created_at_ms: ts(2026, 3, 10, 8, 0),
            updated_at_ms: ts(2026, 3, 10, 8, 30),
            cleared_at_ms: None,
            reminder_count: 0,
            last_reviewed_at_ms: None,
            display_order: ts(2026, 3, 10, 8, 30),
            pinned: false,
        };

        let applied = service.apply_remote_memo(&remote).expect("apply");

        assert_eq!(applied.content, "Book hotel near station");
        assert_eq!(
            service.get_memo(&memo.id).expect("memo").content,
            "Book hotel near station"
        );
    }

    #[test]
    fn applying_remote_memo_replaces_older_local_memo() {
        let mut service = MemoService::open_in_memory().expect("service");
        let memo = service
            .add_memo_at("Call bank", ts(2026, 3, 10, 8, 0))
            .expect("create");
        let remote = Memo {
            id: memo.id.clone(),
            content: "Call bank about card".to_string(),
            status: MemoStatus::Active,
            created_at_ms: ts(2026, 3, 10, 8, 0),
            updated_at_ms: ts(2026, 3, 10, 9, 0),
            cleared_at_ms: None,
            reminder_count: 0,
            last_reviewed_at_ms: None,
            display_order: ts(2026, 3, 10, 9, 0),
            pinned: false,
        };

        let applied = service.apply_remote_memo(&remote).expect("apply");

        assert_eq!(applied.content, "Call bank about card");
        assert_eq!(
            service.get_memo(&memo.id).expect("memo").content,
            "Call bank about card"
        );
    }

    #[test]
    fn deleting_remote_memo_is_idempotent() {
        let mut service = MemoService::open_in_memory().expect("service");
        let memo = service
            .add_memo_at("Remove synced memo", ts(2026, 3, 10, 8, 0))
            .expect("create");

        service.delete_remote_memo(&memo.id).expect("delete");
        service.delete_remote_memo(&memo.id).expect("delete again");

        assert!(service.get_memo_optional(&memo.id).expect("lookup").is_none());
    }

    #[test]
    fn stale_remote_deletion_does_not_remove_newer_local_memo() {
        let mut service = MemoService::open_in_memory().expect("service");
        let memo = service
            .add_memo_at("Keep newer local memo", ts(2026, 3, 10, 8, 0))
            .expect("create");
        service
            .update_memo_at(&memo.id, "Keep updated local memo", ts(2026, 3, 10, 9, 0))
            .expect("update");

        service
            .delete_remote_memo_at(&memo.id, ts(2026, 3, 10, 8, 30))
            .expect("remote delete");

        assert!(service.get_memo_optional(&memo.id).expect("lookup").is_some());
    }

    #[test]
    fn reordering_active_memos_persists_custom_order() {
        let mut service = MemoService::open_in_memory().expect("service");
        let first = service
            .add_memo_at("First cue", ts(2026, 3, 10, 8, 0))
            .expect("create");
        let second = service
            .add_memo_at("Second cue", ts(2026, 3, 10, 8, 1))
            .expect("create");
        let third = service
            .add_memo_at("Third cue", ts(2026, 3, 10, 8, 2))
            .expect("create");

        service
            .reorder_active_memos(&[first.id.clone(), third.id.clone(), second.id.clone()])
            .expect("reorder");

        let active = service.list_active_memos().expect("list active");
        assert_eq!(
            active.iter().map(|memo| memo.id.as_str()).collect::<Vec<_>>(),
            vec![first.id.as_str(), third.id.as_str(), second.id.as_str()]
        );
    }

    #[test]
    fn reordering_active_memos_keeps_updated_timestamps_stable() {
        let mut service = MemoService::open_in_memory().expect("service");
        let first = service
            .add_memo_at("First cue", ts(2026, 3, 10, 8, 0))
            .expect("create");
        let second = service
            .add_memo_at("Second cue", ts(2026, 3, 10, 8, 1))
            .expect("create");
        let original_first_updated_at = first.updated_at_ms;

        service
            .reorder_active_memos(&[second.id.clone(), first.id.clone()])
            .expect("reorder");

        let reloaded_first = service.get_memo(&first.id).expect("first memo");
        let active = service.list_active_memos().expect("list active");
        assert_eq!(reloaded_first.updated_at_ms, original_first_updated_at);
        assert_eq!(
            active.iter().map(|memo| memo.id.as_str()).collect::<Vec<_>>(),
            vec![second.id.as_str(), first.id.as_str()]
        );
    }

    #[test]
    fn pinned_memos_stay_above_newer_regular_memos() {
        let mut service = MemoService::open_in_memory().expect("service");
        let pinned = service
            .add_memo_at("Pinned cue", ts(2026, 3, 10, 8, 0))
            .expect("create");
        service
            .set_memo_pinned_at(&pinned.id, true, ts(2026, 3, 10, 8, 1))
            .expect("pin");
        let regular = service
            .add_memo_at("Regular cue", ts(2026, 3, 10, 8, 2))
            .expect("create");

        let active = service.list_active_memos().expect("list active");

        assert_eq!(
            active.iter().map(|memo| memo.id.as_str()).collect::<Vec<_>>(),
            vec![pinned.id.as_str(), regular.id.as_str()]
        );
        assert!(active[0].pinned);
        assert!(!active[1].pinned);
    }

    #[test]
    fn reopened_memo_returns_to_top_of_regular_memos() {
        let mut service = MemoService::open_in_memory().expect("service");
        let first = service
            .add_memo_at("First cue", ts(2026, 3, 10, 8, 0))
            .expect("create");
        let second = service
            .add_memo_at("Second cue", ts(2026, 3, 10, 8, 1))
            .expect("create");
        service
            .clear_memo_at(&first.id, ts(2026, 3, 10, 8, 2))
            .expect("clear");

        service
            .reopen_memo_at(&first.id, ts(2026, 3, 10, 8, 3))
            .expect("reopen");

        let active = service.list_active_memos().expect("list active");
        assert_eq!(
            active.iter().map(|memo| memo.id.as_str()).collect::<Vec<_>>(),
            vec![first.id.as_str(), second.id.as_str()]
        );
    }
}
