use serde::{Deserialize, Serialize};

#[derive(Debug, Clone, Serialize, Deserialize, PartialEq, Eq)]
#[serde(rename_all = "snake_case")]
pub enum MemoStatus {
    Active,
    Cleared,
}

impl MemoStatus {
    pub fn as_str(&self) -> &'static str {
        match self {
            Self::Active => "active",
            Self::Cleared => "cleared",
        }
    }

    pub fn from_str(value: &str) -> Self {
        match value {
            "cleared" => Self::Cleared,
            _ => Self::Active,
        }
    }
}

#[derive(Debug, Clone, Serialize, Deserialize, PartialEq, Eq)]
#[serde(rename_all = "snake_case")]
pub enum MemoEventType {
    Created,
    Edited,
    Kept,
    Cleared,
    Reopened,
}

impl MemoEventType {
    pub fn as_str(&self) -> &'static str {
        match self {
            Self::Created => "created",
            Self::Edited => "edited",
            Self::Kept => "kept",
            Self::Cleared => "cleared",
            Self::Reopened => "reopened",
        }
    }

    pub fn from_str(value: &str) -> Self {
        match value {
            "edited" => Self::Edited,
            "kept" => Self::Kept,
            "cleared" => Self::Cleared,
            "reopened" => Self::Reopened,
            _ => Self::Created,
        }
    }
}

#[derive(Debug, Clone, Serialize, Deserialize, PartialEq, Eq)]
pub struct Memo {
    pub id: String,
    pub content: String,
    pub status: MemoStatus,
    pub created_at_ms: i64,
    pub updated_at_ms: i64,
    pub cleared_at_ms: Option<i64>,
    pub reminder_count: i64,
    pub last_reviewed_at_ms: Option<i64>,
    #[serde(default)]
    pub display_order: i64,
    #[serde(default)]
    pub pinned: bool,
}

#[derive(Debug, Clone, Serialize, Deserialize, PartialEq, Eq)]
pub struct MemoEvent {
    pub id: i64,
    pub memo_id: String,
    pub event_type: MemoEventType,
    pub content_snapshot: String,
    pub note: Option<String>,
    pub created_at_ms: i64,
}

#[derive(Debug, Clone, Serialize, Deserialize, PartialEq)]
pub struct SearchResult {
    pub memo: Memo,
    pub score: f64,
    pub matched_by: Vec<String>,
}

#[derive(Debug, Clone, Serialize, Deserialize, PartialEq, Eq)]
pub struct ReviewSnapshot {
    pub due: Vec<Memo>,
    pub generated_at_ms: i64,
    pub active_count: usize,
    pub due_count: usize,
}
