package com.example.chatgptnova;

import android.inputmethodservice.InputMethodService;

/** Test-APK-only IME: the probe sends commands; no second writer changes composition. */
public final class ClipboardProbeIme extends InputMethodService {
    @Override public boolean onEvaluateInputViewShown() { return false; }
}
