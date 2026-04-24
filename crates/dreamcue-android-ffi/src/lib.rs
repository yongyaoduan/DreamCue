use std::ptr;
use std::sync::Mutex;

use anyhow::{anyhow, Context, Result};
use dreamcue_core::{Memo, MemoService};
use jni::objects::{JClass, JString};
use jni::sys::{jint, jlong, jstring};
use jni::JNIEnv;
use serde::Serialize;

type SharedService = Mutex<MemoService>;

#[derive(Serialize)]
struct OkEnvelope<T> {
    ok: bool,
    data: T,
}

#[derive(Serialize)]
struct ErrEnvelope {
    ok: bool,
    error: String,
}

#[no_mangle]
pub extern "system" fn Java_app_dreamcue_DreamCueBridge_nativeInit(
    mut env: JNIEnv,
    _class: JClass,
    db_path: JString,
) -> jstring {
    let response = (|| -> Result<OkEnvelope<i64>> {
        let db_path = get_string(&mut env, db_path)?;
        let service = MemoService::open(&db_path)?;
        let handle = Box::into_raw(Box::new(Mutex::new(service))) as i64;
        Ok(OkEnvelope {
            ok: true,
            data: handle,
        })
    })();

    to_jstring(&mut env, envelope_json(response))
}

#[no_mangle]
pub extern "system" fn Java_app_dreamcue_DreamCueBridge_nativeDispose(
    _env: JNIEnv,
    _class: JClass,
    handle: jlong,
) {
    if handle == 0 {
        return;
    }

    let ptr = handle as *mut SharedService;
    if ptr.is_null() {
        return;
    }

    unsafe {
        drop(Box::from_raw(ptr));
    }
}

#[no_mangle]
pub extern "system" fn Java_app_dreamcue_DreamCueBridge_nativeAddMemo(
    mut env: JNIEnv,
    _class: JClass,
    handle: jlong,
    content: JString,
) -> jstring {
    let response = with_service_json(handle, || {
        let content = get_string(&mut env, content)?;
        with_service(handle, |service| service.add_memo(&content))
    });
    to_jstring(&mut env, response)
}

#[no_mangle]
pub extern "system" fn Java_app_dreamcue_DreamCueBridge_nativeUpdateMemo(
    mut env: JNIEnv,
    _class: JClass,
    handle: jlong,
    memo_id: JString,
    content: JString,
) -> jstring {
    let response = with_service_json(handle, || {
        let memo_id = get_string(&mut env, memo_id)?;
        let content = get_string(&mut env, content)?;
        with_service(handle, |service| service.update_memo(&memo_id, &content))
    });
    to_jstring(&mut env, response)
}

#[no_mangle]
pub extern "system" fn Java_app_dreamcue_DreamCueBridge_nativeKeepMemo(
    mut env: JNIEnv,
    _class: JClass,
    handle: jlong,
    memo_id: JString,
) -> jstring {
    let response = with_service_json(handle, || {
        let memo_id = get_string(&mut env, memo_id)?;
        with_service(handle, |service| service.keep_memo(&memo_id))
    });
    to_jstring(&mut env, response)
}

#[no_mangle]
pub extern "system" fn Java_app_dreamcue_DreamCueBridge_nativeClearMemo(
    mut env: JNIEnv,
    _class: JClass,
    handle: jlong,
    memo_id: JString,
) -> jstring {
    let response = with_service_json(handle, || {
        let memo_id = get_string(&mut env, memo_id)?;
        with_service(handle, |service| service.clear_memo(&memo_id))
    });
    to_jstring(&mut env, response)
}

#[no_mangle]
pub extern "system" fn Java_app_dreamcue_DreamCueBridge_nativeReopenMemo(
    mut env: JNIEnv,
    _class: JClass,
    handle: jlong,
    memo_id: JString,
) -> jstring {
    let response = with_service_json(handle, || {
        let memo_id = get_string(&mut env, memo_id)?;
        with_service(handle, |service| service.reopen_memo(&memo_id))
    });
    to_jstring(&mut env, response)
}

#[no_mangle]
pub extern "system" fn Java_app_dreamcue_DreamCueBridge_nativeDeleteMemo(
    mut env: JNIEnv,
    _class: JClass,
    handle: jlong,
    memo_id: JString,
) -> jstring {
    let response = with_service_json(handle, || {
        let memo_id = get_string(&mut env, memo_id)?;
        with_service(handle, |service| service.delete_memo(&memo_id))
    });
    to_jstring(&mut env, response)
}

#[no_mangle]
pub extern "system" fn Java_app_dreamcue_DreamCueBridge_nativeApplyRemoteMemo(
    mut env: JNIEnv,
    _class: JClass,
    handle: jlong,
    memo_json: JString,
) -> jstring {
    let response = with_service_json(handle, || {
        let memo_json = get_string(&mut env, memo_json)?;
        let memo: Memo = serde_json::from_str(&memo_json).context("failed to parse remote memo")?;
        with_service(handle, |service| service.apply_remote_memo(&memo))
    });
    to_jstring(&mut env, response)
}

#[no_mangle]
pub extern "system" fn Java_app_dreamcue_DreamCueBridge_nativeDeleteRemoteMemo(
    mut env: JNIEnv,
    _class: JClass,
    handle: jlong,
    memo_id: JString,
) -> jstring {
    let response = with_service_json(handle, || {
        let memo_id = get_string(&mut env, memo_id)?;
        with_service(handle, |service| service.delete_remote_memo(&memo_id))
    });
    to_jstring(&mut env, response)
}

#[no_mangle]
pub extern "system" fn Java_app_dreamcue_DreamCueBridge_nativeListActiveMemos(
    mut env: JNIEnv,
    _class: JClass,
    handle: jlong,
) -> jstring {
    let response = with_service_json(handle, || {
        with_service(handle, |service| service.list_active_memos())
    });
    to_jstring(&mut env, response)
}

#[no_mangle]
pub extern "system" fn Java_app_dreamcue_DreamCueBridge_nativeListAllMemos(
    mut env: JNIEnv,
    _class: JClass,
    handle: jlong,
) -> jstring {
    let response = with_service_json(handle, || {
        with_service(handle, |service| service.list_all_memos())
    });
    to_jstring(&mut env, response)
}

#[no_mangle]
pub extern "system" fn Java_app_dreamcue_DreamCueBridge_nativeReviewSnapshot(
    mut env: JNIEnv,
    _class: JClass,
    handle: jlong,
) -> jstring {
    let response = with_service_json(handle, || {
        with_service(handle, |service| service.review_snapshot())
    });
    to_jstring(&mut env, response)
}

#[no_mangle]
pub extern "system" fn Java_app_dreamcue_DreamCueBridge_nativeSearchMemos(
    mut env: JNIEnv,
    _class: JClass,
    handle: jlong,
    query: JString,
    limit: jint,
) -> jstring {
    let response = with_service_json(handle, || {
        let query = get_string(&mut env, query)?;
        with_service(handle, |service| {
            service.search_memos(&query, limit.max(1) as usize)
        })
    });
    to_jstring(&mut env, response)
}

#[no_mangle]
pub extern "system" fn Java_app_dreamcue_DreamCueBridge_nativeListEvents(
    mut env: JNIEnv,
    _class: JClass,
    handle: jlong,
    limit: jint,
) -> jstring {
    let response = with_service_json(handle, || {
        with_service(handle, |service| service.list_events(limit.max(1) as usize))
    });
    to_jstring(&mut env, response)
}

fn with_service<T>(
    handle: jlong,
    operation: impl FnOnce(&mut MemoService) -> Result<T>,
) -> Result<T> {
    if handle == 0 {
        return Err(anyhow!("native DreamCue service is not initialized"));
    }

    let ptr = handle as *mut SharedService;
    if ptr.is_null() {
        return Err(anyhow!("native DreamCue service handle is null"));
    }

    let shared = unsafe { &*ptr };
    let mut service = shared
        .lock()
        .map_err(|_| anyhow!("native DreamCue service lock is poisoned"))?;
    operation(&mut service)
}

fn with_service_json<T: Serialize>(
    _handle: jlong,
    operation: impl FnOnce() -> Result<T>,
) -> String {
    envelope_json(operation().map(|data| OkEnvelope { ok: true, data }))
}

fn get_string(env: &mut JNIEnv, value: JString) -> Result<String> {
    let value = env
        .get_string(&value)
        .context("failed to read Java string")?;
    Ok(value.into())
}

fn to_jstring(env: &mut JNIEnv, value: String) -> jstring {
    match env.new_string(value) {
        Ok(output) => output.into_raw(),
        Err(_) => ptr::null_mut(),
    }
}

fn envelope_json<T: Serialize>(result: Result<T>) -> String {
    match result {
        Ok(data) => serde_json::to_string(&data).unwrap_or_else(|err| {
            fallback_error_json(format!("failed to serialize ok response: {err}"))
        }),
        Err(err) => fallback_error_json(format!("{:#}", err)),
    }
}

fn fallback_error_json(message: String) -> String {
    serde_json::to_string(&ErrEnvelope {
        ok: false,
        error: message,
    })
    .unwrap_or_else(|_| "{\"ok\":false,\"error\":\"unknown native error\"}".to_string())
}
