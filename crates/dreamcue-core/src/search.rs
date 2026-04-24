use std::collections::HashSet;

use crate::{Memo, MemoStatus, SearchResult};

pub struct SearchEngine;

impl SearchEngine {
    pub fn search(query: &str, memos: &[Memo], limit: usize) -> Vec<SearchResult> {
        let query_profile = TextProfile::new(query);
        if query_profile.normalized.is_empty() {
            return memos
                .iter()
                .take(limit)
                .cloned()
                .map(|memo| SearchResult {
                    memo,
                    score: 0.0,
                    matched_by: vec!["recent".to_string()],
                })
                .collect();
        }

        let mut results: Vec<SearchResult> = memos
            .iter()
            .filter_map(|memo| Self::score_memo(&query_profile, memo))
            .collect();

        results.sort_by(|left, right| {
            right
                .score
                .total_cmp(&left.score)
                .then_with(|| right.memo.updated_at_ms.cmp(&left.memo.updated_at_ms))
        });
        results.truncate(limit);
        results
    }

    fn score_memo(query: &TextProfile, memo: &Memo) -> Option<SearchResult> {
        let memo_profile = TextProfile::new(&memo.content);
        let exact = memo_profile.normalized.contains(&query.normalized);
        let token_overlap = overlap_ratio(&query.tokens, &memo_profile.tokens);
        let gram_overlap = overlap_ratio(&query.grams, &memo_profile.grams);
        let synonym_overlap = overlap_ratio(&query.expanded_terms, &memo_profile.expanded_terms);
        let subsequence = is_subsequence(&query.compact, &memo_profile.compact);

        let mut score = 0.0_f64;
        let mut matched_by = Vec::new();

        if exact {
            score += 1.8;
            matched_by.push("exact".to_string());
        }
        if token_overlap > 0.0 {
            score += token_overlap * 1.2;
            matched_by.push("token_overlap".to_string());
        }
        if gram_overlap > 0.0 {
            score += gram_overlap;
            matched_by.push("fuzzy".to_string());
        }
        if synonym_overlap > 0.0 {
            score += synonym_overlap * 0.8;
            matched_by.push("semantic_hint".to_string());
        }
        if subsequence {
            score += 0.25;
            matched_by.push("subsequence".to_string());
        }

        score += match memo.status {
            MemoStatus::Active => 0.08,
            MemoStatus::Cleared => -0.02,
        };

        if score < 0.2 {
            return None;
        }

        matched_by.sort();
        matched_by.dedup();
        Some(SearchResult {
            memo: memo.clone(),
            score,
            matched_by,
        })
    }
}

#[derive(Debug)]
struct TextProfile {
    normalized: String,
    compact: String,
    tokens: HashSet<String>,
    grams: HashSet<String>,
    expanded_terms: HashSet<String>,
}

impl TextProfile {
    fn new(input: &str) -> Self {
        let normalized = normalize(input);
        let compact: String = normalized.chars().filter(|c| !c.is_whitespace()).collect();
        let atomic_tokens = atomic_tokens(&normalized);
        let grams = char_grams(&compact, 2)
            .into_iter()
            .chain(char_grams(&compact, 3))
            .collect();
        let expanded_terms = expanded_terms(&normalized, &atomic_tokens);

        Self {
            normalized,
            compact,
            tokens: atomic_tokens,
            grams,
            expanded_terms,
        }
    }
}

fn normalize(input: &str) -> String {
    input.trim().to_lowercase()
}

fn atomic_tokens(input: &str) -> HashSet<String> {
    let mut tokens = HashSet::new();
    if input.is_empty() {
        return tokens;
    }

    tokens.insert(input.to_string());

    for token in input.split(is_delimiter) {
        let token = token.trim();
        if !token.is_empty() {
            tokens.insert(token.to_string());
        }
    }

    let compact: String = input.chars().filter(|c| !c.is_whitespace()).collect();
    for gram in char_grams(&compact, 2) {
        if !gram.is_empty() {
            tokens.insert(gram);
        }
    }

    tokens
}

fn expanded_terms(normalized: &str, tokens: &HashSet<String>) -> HashSet<String> {
    let mut expanded = tokens.clone();
    for group in SYNONYM_GROUPS {
        let matched = group
            .iter()
            .any(|term| normalized.contains(term) || tokens.contains(*term));
        if matched {
            for term in *group {
                expanded.insert((*term).to_string());
            }
        }
    }
    expanded
}

fn overlap_ratio(left: &HashSet<String>, right: &HashSet<String>) -> f64 {
    if left.is_empty() || right.is_empty() {
        return 0.0;
    }
    let intersection = left.intersection(right).count() as f64;
    let base = left.len().max(right.len()) as f64;
    intersection / base
}

fn char_grams(input: &str, size: usize) -> Vec<String> {
    if input.is_empty() {
        return Vec::new();
    }

    let chars: Vec<char> = input.chars().collect();
    if chars.len() < size {
        return vec![input.to_string()];
    }

    chars
        .windows(size)
        .map(|window| window.iter().collect::<String>())
        .collect()
}

fn is_delimiter(ch: char) -> bool {
    ch.is_whitespace()
        || matches!(
            ch,
            ',' | '，'
                | '.'
                | '。'
                | ';'
                | '；'
                | ':'
                | '：'
                | '!'
                | '！'
                | '?'
                | '？'
                | '/'
                | '\\'
                | '|'
                | '-'
                | '_'
                | '('
                | ')'
                | '（'
                | '）'
                | '['
                | ']'
                | '{'
                | '}'
        )
}

fn is_subsequence(needle: &str, haystack: &str) -> bool {
    if needle.is_empty() {
        return true;
    }

    let mut needle_chars = needle.chars();
    let mut current = match needle_chars.next() {
        Some(ch) => ch,
        None => return true,
    };

    for hay in haystack.chars() {
        if hay == current {
            if let Some(next) = needle_chars.next() {
                current = next;
            } else {
                return true;
            }
        }
    }

    false
}

const SYNONYM_GROUPS: &[&[&str]] = &[
    &["提醒", "记得", "别忘", "待办"],
    &["开会", "会议", "讨论", "沟通"],
    &["买", "购买", "采购", "下单"],
    &["学习", "复习", "看书", "准备考试"],
    &["运动", "锻炼", "跑步", "健身"],
    &["联系", "回电话", "回拨", "沟通"],
    &["缴费", "付款", "交钱", "支付"],
    &["提交", "上交", "发送", "发出"],
    &["看医生", "复诊", "门诊", "挂号"],
    &["做饭", "买菜", "准备晚饭", "煮饭"],
];
